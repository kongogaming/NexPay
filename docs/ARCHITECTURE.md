# NexPay Architecture

> **Attribution Note**: NexPay is an adapted derivative work based upon the foundational offline payment architecture, state machine, and SMS verification pipeline pioneered by the open-source **Flowpay** project (Payments Without Internet, Apache 2.0).

NexPay is a single-module Android app that puts a smartphone UI on top of two offline UPI rails — `*99#` USSD and UPI 123PAY IVR — and confirms payment outcomes by reading the bank's confirmation SMS locally. There is no backend, no `INTERNET` permission, and no telemetry. This document is the map: how the pieces fit, where the load-bearing invariants live, and which known simplifications are deliberate.

---

## The One Invariant That Matters

**A payment is only ever marked SUCCESS by a confirming bank SMS — never by call state or call duration.**

Everything below serves this. A dialed call that connects and ends "normally" proves nothing; only the bank's own confirmation SMS, matched to the amount that was sent, promotes a payment to SUCCESS. When no SMS arrives before the deadline, the attempt is discarded and nothing is shown — an outcome the bank never confirmed is not one this app can report on, and it is never silently assumed either way.

---

## Composition Root

[`NexPayApplication`](../app/src/main/java/com/nexpay/app/NexPayApplication.kt) owns a single [`AppContainer`](../app/src/main/java/com/nexpay/app/di/AppContainer.kt) — the manual, framework-free composition root. It constructs and holds the process-scoped graph:

```
AppContainer
├── appScope: CoroutineScope           (SupervisorJob + Dispatchers.Default)
├── settingsRepository
├── callStateCoordinator               (the ONE telephony listener)
└── paymentSessionManager              (the ONLY writer of payment state)
        └── store = TransactionRepository (implements PaymentTransactionStore)
```

Receivers and services reach it via `NexPayApplication.from(context)`. DI is hand-wired on purpose: the graph is small, and for a payments app, construction a reader can follow by eye beats annotation-generated indirection. `TransactionRepository`, `AppDatabase`, and `TransactionDetector` remain thread-safe, application-context-keyed `getInstance()` singletons; the container references them rather than duplicating their lifecycle.

---

## Payment Lifecycle State Machine

[`PaymentSessionManager`](../app/src/main/java/com/nexpay/app/payment/PaymentSessionManager.kt) is the single writer of payment state and the most carefully-built part of the app. It takes its dependencies as interfaces (`PaymentTransactionStore`, `CallStateSource`) plus an injectable clock, which is what makes it fully unit-testable without a device (see `PaymentSessionManagerTest`).

```
begin(phone, amount)          [QR: begin("", amount?, upiId, source=QR)]
   │  writes a PENDING row BEFORE anything is dialed, so a process death
   │  mid-call still leaves a record of the attempt
   ▼
Initiating ──OFFHOOK──▶ InProgress ──call ends──▶ WaitingForVerification
   │                        │  (short call <5s ⇒ Cancelled, never SUCCESS)     │
   │                        │                                                   │
   └── dial failed /        └───────────── confirming bank SMS ────────────────┤
       call never started              (onSmsConfirmed)                         │
              │                                                                 ▼
              ▼                              ┌── amount matches ⇒ Success ──────┤
           Cancelled                         ├── amount mismatch ⇒ dropped,     │
       (PaymentWindowObserver also           │   window stays open for the      │
        closes the SMS window, so a          │   real confirmation              │
        later debit can't be adopted         └── failure keyword ⇒ Failed       │
        onto the cancelled row)                                                 │
   no SMS before the 10-minute deadline ⇒ Timeout, and the PENDING row is DELETED
                                             │
                    Nothing is surfaced: a payment the bank never confirmed
                    leaves no record. The SMS window outlives the deadline by
                    30s, so a late-but-genuine confirmation still lands — with
                    no row to adopt it is saved as a standalone transaction.
```

Every terminal transition `join()`s the pending-insert coroutine first, so the PENDING row always exists before it's updated. Stale PENDING rows left by a killed process are discarded lazily (`reconcileStalePending` on app start). If the process died mid-payment and the confirming SMS arrives after restart, the ingestion pipeline *reattaches* it: the session txnId is persisted in the operation window at `begin()`, and a no-live-session confirmation updates that still-PENDING row instead of inserting a duplicate. Adoption is guarded to PENDING rows in SQL, so a confirmation can never rewrite a row the user already cancelled. The QR flow runs through the same session lifecycle. The `PaymentState` sealed hierarchy carries exactly the states the machine emits.

---

## SMS Ingestion — Two Pipelines, One Parser

The confirmation SMS is money-outcome truth, so its handling is the second most careful area.

```
                 ┌─────────────────────────────┐
  bank SMS ─────▶│ SimpleSMSReceiver           │  priority-999 broadcast
                 │ (RECEIVE_SMS)               │  receiver; goAsync + 8s cap
                 └──────────────┬──────────────┘
                                │
                 shouldProcessSMS() + tryClaimSms()
                                │
                                ▼
                        SmsTransactionParser.parse()   ← PURE, Context-free, tested
                                            │
                                SmsIngestionPipeline.ingest()
                                            │
                     ┌──────────────────────┴───────────────────────┐
                     ▼                                               ▼
          active session? update its row              no session (QR/legacy)?
          via PaymentSessionManager.onSmsConfirmed     insert a fresh row
                     └──────────────────┬──────────────────────────┘
                                        ▼
                            launch PaymentResultActivity
```

- [`SmsTransactionParser`](../app/src/main/java/com/nexpay/app/payment/sms/SmsTransactionParser.kt) holds all the bank-SMS matching logic — pure, Context-free, and tested against a per-bank corpus. [`TransactionDetector`](../app/src/main/java/com/nexpay/app/helpers/TransactionDetector.kt) is the stateful shell around it: the SharedPreferences-backed *operation window* (only SMS arriving while a payment is in flight are eligible; sized to the verification deadline plus a grace margin, so a slow bank SMS is never dropped here while the session still awaits it) and the dedup that stops a redelivered SMS broadcast from double-processing one message.
- **Dedup** is `tryClaimSms` (a body-only normalized-key claim) plus `@Synchronized processSMS` re-checking the operation window under lock.
- [`SmsIngestionPipeline`](../app/src/main/java/com/nexpay/app/receivers/SmsIngestionPipeline.kt) is the single path from a claimed SMS to an outcome — parsing, session confirmation or orphaned-row reattach, persistence, broadcasts, the result notification, and the result-screen launch. The debug SMS-injection tool drives the same pipeline, so tests exercise the exact production path.
- `RECEIVE_SMS` is the only SMS ingestion path. NexPay does not request `READ_SMS` and never scans past SMS history.

---

## Telephony and the Call Overlay

[`CallStateCoordinator`](../app/src/main/java/com/nexpay/app/telephony/CallStateCoordinator.kt) is the single app-wide call-state listener; `PaymentSessionManager` consumes its events. [`CallManager`](../app/src/main/java/com/nexpay/app/managers/CallManager.kt) places the actual `ACTION_CALL` dial (the DTMF 123PAY string is built and validated by the pure, tested [`Upi123CallStringBuilder`](../app/src/main/java/com/nexpay/app/payment/Upi123CallStringBuilder.kt)) and manages call audio.

[`CallOverlayService`](../app/src/main/java/com/nexpay/app/services/CallOverlayService.kt) draws a `TYPE_APPLICATION_OVERLAY` window during the call so the user has a UI anchor, and mirrors `PaymentState` into result dialogs. Its overlay watchdog and the user "End call" path notify the session **before** any best-effort UI/audio cleanup.

---

## User Interface & Design Tokens

Screens are built with Jetpack Compose. `MainActivity`, `SettingsActivity` and `TransactionHistoryActivity` follow ViewModel + StateFlow collected in Compose, with Activity↔Compose one-shot events carried by a shared ViewModel ([`MainViewModel`](../app/src/main/java/com/nexpay/app/viewmodel/MainViewModel.kt)).

Colors and copy each have a single source of truth:
- Dynamic theme tokens in [`ui/theme/Color.kt`](../app/src/main/java/com/nexpay/app/ui/theme/Color.kt) and [`ui/theme/Theme.kt`](../app/src/main/java/com/nexpay/app/ui/theme/Theme.kt) provide accessible dark and light palettes.
- `statusColor(status)` maps transaction states to consistent semantic colors across all screens.
- All user-visible strings are loaded from `strings.xml`.
- CI safety gates enforce zero hardcoded inline hex colors or unlocalized text literals.

---

## Persistence

Local-only Room SQLite database encrypted at rest with SQLCipher. The database passphrase is a random key wrapped by a **non-exportable Android Keystore key**, so it never leaves hardware. If that wrapping key is ever lost or invalidated (OS update, keystore corruption, device restore) while the wrapped blob survives, [`DatabaseKeyManager`](../app/src/main/java/com/nexpay/app/data/DatabaseKeyManager.kt) recovers gracefully rather than crashing — it discards the unrecoverable blob, regenerates the key, and cleanly initializes a fresh database.

The raw SMS body is never persisted — only a privacy-safe excerpt constructed from extracted fields (validated via automated CI gates).

---

## Components & Manifest Declarations

- **Activities**: `MainActivity` (sole LAUNCHER, exported), plus Setup / TestConfiguration / QRScanner / PaymentResult (`singleTask`) / TransactionHistory / Settings (all `exported=false`).
- **Services**: `CallOverlayService`.
- **Receiver**: `SimpleSMSReceiver` (priority-999, guarded by `BROADCAST_SMS`).
- **Debug only**: `DebugSmsInjectionReceiver` lives in `src/debug/` and is completely absent from release builds.

Permissions are minimal and strictly scoped: `CALL_PHONE`, `READ_PHONE_STATE`, `ANSWER_PHONE_CALLS`, `RECEIVE_SMS` (never `READ_SMS`), `CAMERA`, `READ_CONTACTS`, `SYSTEM_ALERT_WINDOW`, `POST_NOTIFICATIONS`, `VIBRATE`, and `MODIFY_AUDIO_SETTINGS`. No `INTERNET` permission is declared or utilized.

---

## Known Deliberate Architecture Simplifications

- **`CallManager` Per-Caller Scope**: The legacy generic `initiateCall()` path still uses a per-call `PhoneStateListener`. This is reachable only from the `TestConfiguration` connectivity-test screen.
- **`PaymentResultActivity` as Classic View**: Launched from background receivers with specific window/`singleTask` behavior; keeping it as a View prevents windowing regressions on payment outcome displays.
- **Permissive SMS Matcher with Strict Downstream Validation**: Banks phrase SMS confirmations in varying formats across regions. NexPay uses a broad regex matcher followed by strict paise-exact amount verification, word-boundary keywords, and debit-first extraction.
