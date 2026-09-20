# Frequently Asked Questions (FAQ)

> **Note**: NexPay is an adapted open-source project based upon the foundational offline payment architecture of **Flowpay** (Payments Without Internet).

---

### Why does NexPay have no `INTERNET` permission?

Because it genuinely never talks to an IP network. NexPay is an on-device client operating over cellular telecom payment rails — `*99#` USSD and UPI 123PAY IVR — which run over legacy cellular voice/signalling channels. There is no backend, no analytics, no advertising SDKs, and no telemetry. The absence of the `INTERNET` permission in `AndroidManifest.xml` provides mathematical certainty that user payment metadata cannot leak to the internet.

---

### Why does NexPay request `RECEIVE_SMS` but not `READ_SMS`?

`RECEIVE_SMS` allows the application to observe incoming SMS messages in real time as they arrive; `READ_SMS` would grant broad access to scan the user's entire historical SMS inbox. NexPay strictly adheres to least privilege: it only needs to intercept bank confirmation broadcasts while an active payment observation window is open. NexPay never requests `READ_SMS`, never scans past messages, and only parses and stores privacy-safe transaction metadata.

---

### Does NexPay ever see or store my UPI PIN?

No. Your UPI PIN is entered exclusively inside your bank's secure telephony IVR or telecom USSD session. NexPay does not use an Android Accessibility Service and cannot inspect your dialer keypad inputs or screen. There is no technical path through which NexPay could observe, intercept, or log your UPI PIN.

---

### Why isn't NexPay on the Google Play Store?

Google Play's restricted permissions policy heavily limits apps that utilize SMS-related permissions (`RECEIVE_SMS`) unless designated as the user's default SMS handler. Rather than compromising the legitimate offline confirmation model, NexPay is distributed as an open-source project that you build and install yourself.

---

### How can a payment succeed without internet?

Your smartphone's cellular network connects directly to the telecom and banking switches over standard cellular signalling and voice channels. NexPay constructs the DTMF dialing sequence and launches the phone call; the transaction itself is authorized directly between you, your bank, and the telecom network, identical to dialing `*99#` or an IVR number manually.

---

### Why didn't a transaction appear in Recent Payments?

Because **no confirming SMS was received from your bank**. In accordance with NexPay's core invariant, a transaction is recorded as `SUCCESS` only when an authentic confirmation SMS arrives from the bank matching the expected amount. If no SMS arrives before the observation window closes, the attempt is discarded rather than recorded as an unverified success.

---

### What are the carrier and USSD limitations?

`*99#` NUUP USSD operates over 2G/GSM signalling. Session response times vary by operator, and USSD is not supported on pure-IP/VoLTE networks such as Reliance Jio. For this reason, NexPay integrates both `*99#` USSD (for Airtel, Vi, BSNL, MTNL) and UPI 123PAY IVR (which works across all networks, including Jio).

---

### Why does NexPay prompt for Primary SIM during setup?

UPI payments must originate from the mobile number registered with your bank account. In dual-SIM Android devices, outgoing phone calls and USSD sessions may default to SIM 2 even if your bank account is tied to SIM 1. NexPay detects and warns the user if the active voice call SIM does not match the configured bank SIM.

---

### Is it safe to build and run NexPay myself?

Yes. The project is fully self-contained and builds using public Maven repositories and the Gradle wrapper:
```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```
See [TESTING.md](TESTING.md) for testing procedures and [ARCHITECTURE.md](ARCHITECTURE.md) for architectural details.
