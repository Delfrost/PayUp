# MoneyPilot (PayUp) 💸

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/Target%20SDK-35-green?style=flat-square&logo=android)](https://developer.android.com/about/versions/15)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room%202.6.1-orange?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![Offline First](https://img.shields.io/badge/Privacy-100%25%20Offline-black?style=flat-square)](https://github.com/Delfrost/PayUp)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey?style=flat-square)](LICENSE)

**MoneyPilot** (also known as **PayUp**) is an offline-first, privacy-focused personal finance and expense tracking application built natively for Android. Designed to deliver bank-grade accounting precision without compromising user data, MoneyPilot operates entirely on-device with zero cloud exposure, zero third-party telemetry, and zero subscription paywalls.

---

## 🌟 Key Features

### 🔒 100% Offline & Private
- **Zero Cloud Dependence:** All transactions, accounts, categories, and budgets are stored strictly in an encrypted local SQLite database via Room.
- **No Telemetry / No Ads:** No third-party analytical trackers, user profiling, or external server pings.

### 💰 Exact Integer Accounting (Paise Precision)
- **Zero Floating-Point Drift:** Every financial calculation (income, expense, transfer, allocation) is calculated deterministically in paise (`Long`) using `BigDecimal` scaling.
- **Accurate to the Cent/Paisa:** Guarantees zero 1-paisa rounding errors across months of transactions.

### 📩 Intelligent On-Device SMS Detection
- **Bank Alert Parser:** High-efficiency, local regex and state-machine parsers for SMS transaction alerts from major Indian banks (SBI, HDFC, ICICI, Axis, Kotak, PNB, BOB, etc.).
- **Smart Credit vs. Debit Resolution:** Robust classification of incoming peer transfers, salary credits, and POS/merchant debits with UTR / reference ID extraction.

### 🔔 Live Notification Ingestion
- **UPI Notification Listener:** Automatically captures payment push notifications from Google Pay, PhonePe, Paytm, BHIM, super.money, WhatsApp Pay, and Amazon Pay.
- **Cross-Source Deduplication:** Uses reference ID (UTR) matching and SHA-256 time-bucketed hashing to eliminate duplicate entries between SMS alerts and push notifications.

### 📸 On-Device Receipt & Screenshot OCR
- **Google ML Kit Vision Integration:** On-device OCR that extracts transaction amounts, merchant names, dates, and reference numbers directly from payment screenshots without uploading images to any external server.
- **Pre-Commit Review Flow:** Extracted transactions are displayed for manual user review, editing, and confirmation prior to database insertion.

### 📊 Envelope Budgeting & Salary Allocations
- **Configured Income & Pools:** Assign monthly salary into categorized budget pools (Rent, Personal, Travel, Groceries, Savings).
- **Over-Allocation Safeguards:** Visual banners immediately warn if assigned budget limits exceed total expected monthly income.
- **Rollover Support:** Optional month-to-month budget rollover for accumulated savings.

### 💳 Multi-Account & Net Worth Tracking
- **Unified Account Balances:** Reconcile spending across UPI / Wallets, Primary Bank Accounts, Credit Cards, and Cash.
- **Automatic Net Balance Calculation:** Dynamically updates ledger balances based on confirmed transaction history.

---

## 🛠️ Technology Stack & Architecture

MoneyPilot is architected following modern Android best practices (MAD) with single-activity architecture, unidirectional data flow (UDF), and clean layer separation:

- **Language:** Kotlin 2.0 (with KSP code generation)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (Material3)
- **Architecture Pattern:** MVVM (Model-View-ViewModel) + Repository Pattern + Unidirectional Data Flow (UDF)
- **Local Persistence:** [Room 2.6.1](https://developer.android.com/training/data-storage/room) + Room Paging + TypeConverters
- **Pagination:** AndroidX [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) for fast, smooth scrolling over thousands of transactions
- **Asynchronous Operations:** Kotlin Coroutines (`StateFlow`, `SharedFlow`, `combine`, `flatMapLatest`)
- **Machine Learning (OCR):** Google ML Kit Text Recognition (`com.google.mlkit:text-recognition:16.0.1`)
- **Preferences:** AndroidX DataStore Preferences

---

## 📁 Project Structure

```
PayUp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/payup/app/
│   │   │   │   ├── MainActivity.kt               # Single entry-point Activity
│   │   │   │   ├── PayUpApplication.kt           # Application class & DB singleton
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/                       # Room Database, DAOs, Migrations, TypeConverters
│   │   │   │   │   │   ├── PayUpDatabase.kt
│   │   │   │   │   │   ├── TransactionDao.kt
│   │   │   │   │   │   ├── CategoryDao.kt
│   │   │   │   │   │   ├── AccountDao.kt
│   │   │   │   │   │   └── BudgetDao.kt
│   │   │   │   │   └── model/                    # Room Entities (Transaction, Category, Account, Budget)
│   │   │   │   ├── repository/                   # Data access abstraction & caching
│   │   │   │   ├── service/                      # Background services & parsers
│   │   │   │   │   ├── SmsParser.kt              # Bank SMS regex & logic
│   │   │   │   │   ├── NotificationParser.kt     # Push notification processor
│   │   │   │   │   ├── ScreenshotParser.kt       # OCR text layout analysis
│   │   │   │   │   ├── DedupService.kt           # SHA-256 hash & UTR deduplication
│   │   │   │   │   └── PayUpNotificationListenerService.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── navigation/               # Compose NavHost & navigation routes
│   │   │   │   │   ├── screen/                   # Compose Screen Composables
│   │   │   │   │   │   ├── DashboardScreen.kt    # Main overview & financial summaries
│   │   │   │   │   │   ├── TransactionListScreen.kt # Paged transaction ledger
│   │   │   │   │   │   ├── TransactionDetailScreen.kt
│   │   │   │   │   │   ├── AddTransactionScreen.kt
│   │   │   │   │   │   └── ScreenshotReviewScreen.kt
│   │   │   │   │   ├── theme/                    # Material 3 colors, typography, icons
│   │   │   │   │   └── viewmodel/                # AndroidViewModels exposing StateFlow
│   │   │   │   └── util/                         # CurrencyUtils, DateUtils (ThreadLocal, ZoneId)
│   │   │   └── res/                              # Drawables, themes, XML configs
│   │   └── test/                                 # Unit tests (parsers, dedup, arithmetic)
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2+) or newer
- **JDK:** Java 17 (Microsoft OpenJDK / Oracle / Temurin)
- **Android Device or Emulator:** Android 8.0 (API level 26) or higher

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Delfrost/PayUp.git
   cd PayUp
   ```

2. **Open the project** in Android Studio, or compile using the Gradle wrapper:

   - **Windows:**
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   - **macOS / Linux:**
     ```bash
     ./gradlew assembleDebug
     ```

3. **Install on device:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🛡️ Privacy & Permissions

MoneyPilot requests optional permissions only when corresponding features are enabled by the user:

| Permission | Purpose |
|---|---|
| `RECEIVE_SMS` | Optional. Required only to automatically parse bank credit/debit alerts in real-time. |
| `POST_NOTIFICATIONS` | Optional. To notify the user when a transaction is automatically ingested and categorized. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Optional. Required only if UPI app push notification tracking is enabled. |
| `READ_MEDIA_IMAGES` / Photo Picker | Uses Android system photo picker to select screenshots for OCR review without full storage access. |

All data stays strictly within the application's private SQLite sandbox (`/data/data/com.payup.app/databases/payup.db`).

---

## 🤝 Contributing

Contributions, feature suggestions, and bug reports are welcome!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.
