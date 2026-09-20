# NexPay v1.0.0 Release Notes

**Release Date**: September 20, 2026  
**Application ID**: `com.nexpay.app`  
**Target SDK**: 35 (Android 15) | **Min SDK**: 29 (Android 10)  
**License**: Apache License, Version 2.0  
**Upstream Project Foundation**: [Flowpay: Payments Without Internet](https://github.com/Flowpayup/Payments-Without-Internet)

---

## Overview

NexPay v1.0.0 is the initial public release of **NexPay — Payments Without Internet**. NexPay is an Android application designed for offline digital payments across India when mobile internet data is unavailable or unreliable. It operates directly over telecom-native payment rails (`*99#` NUUP USSD and UPI 123PAY IVR) and authoritatively verifies transaction outcomes through local incoming bank SMS parsing.

NexPay is derived and adapted from the open-source **Flowpay** project under the Apache License 2.0. This release provides enterprise-grade stabilization, complete Jetpack Compose theme support, least-privilege permission flow, and Slice Small Finance Bank integration.

---

## What's Included in v1.0.0

### Core Payment Functionality
- **Dual Offline Rails**:
  - **`*99#` USSD (NUUP)**: Optimized for GSM operators (Airtel, Vodafone Idea, BSNL, MTNL). Automated dialer trigger for scan-to-pay QR flows.
  - **UPI 123PAY (IVR)**: Built for voice-capable and all-IP/VoLTE networks (such as Reliance Jio) where USSD is unsupported. Validates DTMF payloads and automates dialing sequences.
- **Offline QR Code Scanning**:
  - Integrated CameraX and ZXing scanner for parsing UPI and BharatQR codes locally without network connectivity.
- **Authoritative SMS Ingestion Pipeline**:
  - Outgoing transactions create an active observation window.
  - High-priority `BroadcastReceiver` captures bank confirmation SMS (`android.permission.RECEIVE_SMS`).
  - Pure, context-free regex parsing engine extracts transaction references, timestamps, and paise-exact amounts.
- **Local Persistence & Encryption**:
  - Room SQLite database encrypted at rest using SQLCipher.
  - Database passphrase wrapped via hardware-backed, non-exportable Android Keystore keys.
- **Dynamic Theming**:
  - Full support for System Default, Forced Light, and Forced Dark themes using custom semantic design tokens (`LocalNexPayColors`). Selection persists across app restarts.
- **Expanded Financial Support**:
  - Integration of **Slice Small Finance Bank** across bank selection lists, alongside all major Indian public and private sector banks.
- **2-Step Onboarding & Permissions Wizard**:
  - Step 1: Account setup (bank selection and primary SIM confirmation).
  - Step 2: Step-by-step least-privilege permission checklist with real-time status badges.

---

## Security & Privacy Model

- **Zero Internet Permission (`NO_INTERNET`)**: `android.permission.INTERNET` is neither requested nor present in the manifest.
- **Least-Privilege SMS Architecture**: Uses `RECEIVE_SMS` only during active payment windows. Does **not** request `READ_SMS` and never accesses historical inboxes.
- **No Microphone Access**: Does **not** request `RECORD_AUDIO`.
- **UPI PIN Protection**: NexPay never prompts for, reads, or records the user's UPI PIN. All PIN entries occur directly within the telecom provider's secure dialer or IVR prompt.
- **No Analytics / Telemetry**: No third-party tracking, crash analytics, or remote logging SDKs are bundled.

---

## Testing & Verification Status

The v1.0.0 codebase has undergone rigorous local engineering verification:

1. **Static Analysis (Detekt)**:
   - Zero violations across all source sets.
   - Strictly conforms to maximum line length ($\le 120$ chars), method length ($\le 60$ lines), and zero wildcard import rules.
2. **Unit Tests**:
   - `:app:testDebugUnitTest`: All test suites passed.
   - `:app:testReleaseUnitTest`: All test suites passed.
3. **Code Coverage**:
   - Kover XML report generated via `:app:koverXmlReportDebug`.
4. **Android Lint**:
   - `:app:lintDebug`: 0 errors.
   - `:app:lintRelease`: 0 errors.
5. **CI Safety Gates**:
   - `check_ci_gates.ps1`: 0 PII leaks, 0 unlocalized text leaks, 0 hardcoded palette leaks.

---

## Verified Build Artifacts

| Parameter | Release APK (`app-release.apk`) |
| :--- | :--- |
| **Output Path** | `app/build/outputs/apk/release/app-release.apk` |
| **File Size** | **16,386,883 bytes** (~15.63 MB) |
| **Optimization** | R8 code minification & resource shrinking enabled |
| **Signature Scheme** | **APK Signature Scheme v2** (Verified via `apksigner`) |
| **Signer Certificate** | `CN=NexPay Local Test, OU=Development, O=NexPay, C=IN` |
| **SHA-256 Digest** | `48bc415de9ba832be9966eba1c5523440c86e2f8aef48aef72d72851aa331f37` |

---

## Known Limitations

1. **Upstream NPCI IVR Platform Behavior (Spoken Voice Prompts)**:
   - In certain telecom circles, the national NPCI UPI 123PAY IVR platform may require spoken voice responses (e.g., saying *"Pay money"*) rather than touch-tone DTMF pauses.
   - NexPay intentionally avoids requesting `RECORD_AUDIO` to maintain its privacy guarantees. Users should speak directly into the active phone call when prompted by the IVR.
2. **GSM Operator USSD Constraints**:
   - `*99#` USSD sessions rely on legacy 2G/GSM signalling and may experience carrier session timeouts in congested network zones. USSD is not supported on Reliance Jio (which uses the 123PAY IVR rail instead).
3. **Bank SMS Phrasing Variations**:
   - Transaction success requires an authentic confirmation SMS from the issuing bank. Delays in carrier SMS delivery will hold the payment in an observation state until received or timed out.

---

## Upstream Project Attribution

NexPay is derived from **Flowpay: Payments Without Internet**:
- **Upstream Repository**: [https://github.com/Flowpayup/Payments-Without-Internet](https://github.com/Flowpayup/Payments-Without-Internet)
- **Upstream Copyright**: Copyright 2026 Flowpay
- **Modifications Copyright**: Copyright 2026 NexPay Contributors
- **License**: Apache License, Version 2.0 (see [LICENSE](../LICENSE) and [NOTICE](../NOTICE))
