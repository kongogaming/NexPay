# Legal Notice & Disclaimer

**Please read this document carefully before using, building, forking, or contributing to NexPay. By using or interacting with this software, you accept these terms in full.**

---

## 1. Upstream Attribution & Open Source Foundation

NexPay is an independent open-source project adapted and derived from **Flowpay: Payments Without Internet** (Copyright 2026 Flowpay, available at [https://github.com/Flowpayup/Payments-Without-Internet](https://github.com/Flowpayup/Payments-Without-Internet)).

NexPay builds upon the foundational offline payment architecture, payment state machine, and SMS verification pipeline pioneered by the Flowpay project, while incorporating project-specific rebranding, refined high-contrast Jetpack Compose interfaces, dynamic system theming, additional financial institution support (such as Slice Small Finance Bank), and comprehensive test/build hardening.

Both the original Flowpay code and NexPay modifications are distributed under the terms of the **Apache License, Version 2.0** (see [LICENSE](LICENSE) and [NOTICE](NOTICE)).

---

## 2. No Warranty

This software is provided under Apache License 2.0 **"AS IS", WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied**, including but not limited to the warranties of merchantability, fitness for a particular purpose, non-infringement, accuracy, quiet enjoyment, or reliability. See Section 7 of [LICENSE](LICENSE) for the full legal text.

You assume all risk associated with the quality, performance, operation, and results of using this software.

---

## 3. No Affiliation or Official Endorsement

NexPay is an independent open-source community project. It is **not affiliated with, sponsored by, certified by, partnered with, or endorsed by**:
- The National Payments Corporation of India (NPCI)
- Unified Payments Interface (UPI) or UPI 123PAY schemes
- Any banking or financial institution
- Any telecommunications operator (e.g., Airtel, Jio, Vodafone Idea, BSNL, MTNL)
- Google LLC or the Android Open Source Project (AOSP)
- The original Flowpay authors or maintainers

All references to payment rails (`*99#` NUUP USSD, UPI 123PAY IVR), banking entities, telecom operators, and shortcodes are made strictly for descriptive, identification, and technical compatibility purposes.

---

## 4. Direct Bank and Telecom Interactions

When NexPay initiates a `*99#` USSD session or a UPI 123PAY telephone call:
- **You interact directly with your telecom network and your financial institution.**
- NexPay does **not** act as a payment gateway, payment aggregator, financial intermediary, money transmitter, or wallet.
- NexPay never prompts for, captures, stores, logs, or transmits your UPI PIN. UPI PIN entry is executed exclusively within the secure telephony/IVR or USSD prompt provided by your telecom operator and bank.
- NexPay only launches the native dialer with standard DTMF strings and monitors incoming bank confirmation SMS messages locally on the device to display transaction status.

All financial transaction outcomes—whether successful, declined, delayed, or debited—are strictly between you, your bank, and your mobile network operator, subject to their respective terms of service and applicable financial scheme regulations.

---

## 5. User Responsibility & Regulatory Compliance

You are solely responsible for ensuring that your use of this application complies with all relevant local, state, national, and international laws and regulations, including telecommunications rules, financial service laws, and operator terms. If your telecom operator or jurisdiction restricts automated dialing or USSD interactions, do not use this application.

---

## 6. Telecom & Banking Charges

Dialing `*99#` USSD codes or placing voice calls to UPI 123PAY IVR service numbers may incur airtime, carrier, or USSD session charges according to your telecom tariff. Certain banks may also levy standard transaction fees. NexPay has no visibility into, nor responsibility for, any carrier or banking fees incurred.

---

## 7. Permissions and Local Data Privacy

- **Zero Internet Access**: NexPay does not request or possess the `android.permission.INTERNET` permission. No data, telemetry, analytics, or user identifiers can ever leave the device over an IP network.
- **Least-Privilege SMS**: NexPay utilizes `android.permission.RECEIVE_SMS` exclusively to capture incoming bank confirmation messages during an active payment observation window. It does not request `android.permission.READ_SMS` and does not scan or inspect past SMS inbox history.
- **Local Storage**: Transaction records are persisted strictly on-device in a locally encrypted SQLite database (`SQLCipher`).

---

## 8. Nominative Trademark Notice

All product names, logos, trademarks, and service marks mentioned in this repository or within the application are the property of their respective owners. Their use is purely nominative and descriptive, and implies no affiliation, endorsement, or certification.

---

## 9. Limitation of Liability

To the maximum extent permitted by applicable law, neither the author(s), contributor(s), nor copyright holder(s) of NexPay or Flowpay shall be held liable for any direct, indirect, incidental, special, exemplary, or consequential damages (including, without limitation, loss of funds, erroneous transfers, double debits, transaction failures, carrier charges, loss of data, or operational disruptions) arising in any way out of the use of or inability to use this software.

---

*By building, installing, running, or redistributing NexPay, you explicitly acknowledge and agree to the above terms.*
