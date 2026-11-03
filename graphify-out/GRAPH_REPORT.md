# Graph Report - PayUp  (2026-11-03)

## Corpus Check
- 63 files · ~23,355 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 14 file(s) not represented in the graph (top: .xml 6, (none) 3, .properties 2)

## Summary
- 675 nodes · 1556 edges · 37 communities (19 shown, 18 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 42 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f8f352d8`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Transaction Data Access
- App Navigation & Entry
- Account
- Category Persistence
- Merchant Rule Persistence
- DashboardScreen.kt
- IncomeSource
- Dashboard View Model
- Pending Observation Persistence
- ScreenshotParser.kt
- Notification Parser Service
- TransactionType
- TransactionStatus
- 🌟 Key Features
- .generateHash
- Room Type Converters
- BudgetDao
- Add Transaction ViewModel
- Account Model & Types
- PayUpDatabase
- .parse
- Budget
- TransactionDetailViewModel.kt
- ScreenshotReviewViewModel.kt
- Project Architecture Concepts
- ObservationStatus
- DetectionMethod

## God Nodes (most connected - your core abstractions)
1. `Transaction` - 63 edges
2. `TransactionRepository` - 42 edges
3. `Category` - 40 edges
4. `PayUpDatabase` - 33 edges
5. `Account` - 33 edges
6. `TransactionDao` - 30 edges
7. `Budget` - 28 edges
8. `AddTransactionViewModel` - 26 edges
9. `TransactionType` - 25 edges
10. `DashboardViewModel` - 23 edges

## Surprising Connections (you probably didn't know these)
- `PayUpNavHost()` --calls--> `AddTransactionScreen()`  [INFERRED]
  app/src/main/java/com/payup/app/ui/navigation/PayUpNavigation.kt → app/src/main/java/com/payup/app/ui/screen/AddTransactionScreen.kt
- `PayUpNavHost()` --calls--> `CategoryTransactionsScreen()`  [INFERRED]
  app/src/main/java/com/payup/app/ui/navigation/PayUpNavigation.kt → app/src/main/java/com/payup/app/ui/screen/CategoryTransactionsScreen.kt
- `PayUpNavHost()` --calls--> `DashboardScreen()`  [INFERRED]
  app/src/main/java/com/payup/app/ui/navigation/PayUpNavigation.kt → app/src/main/java/com/payup/app/ui/screen/DashboardScreen.kt
- `PayUpNavHost()` --calls--> `ScreenshotReviewScreen()`  [INFERRED]
  app/src/main/java/com/payup/app/ui/navigation/PayUpNavigation.kt → app/src/main/java/com/payup/app/ui/screen/ScreenshotReviewScreen.kt
- `PayUpNavHost()` --calls--> `TransactionDetailScreen()`  [INFERRED]
  app/src/main/java/com/payup/app/ui/navigation/PayUpNavigation.kt → app/src/main/java/com/payup/app/ui/screen/TransactionDetailScreen.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Financial Core Architecture & State Management** — project_state_deterministic_paise_accounting, project_state_reactive_dashboard, project_state_canonical_cash [INFERRED 0.85]

## Communities (37 total, 18 thin omitted)

### Community 0 - "Transaction Data Access"
Cohesion: 0.07
Nodes (6): AccountNetBalance, AccountSpend, CategorySpend, TransactionDao, Transaction, TransactionRepository

### Community 1 - "App Navigation & Entry"
Cohesion: 0.14
Nodes (4): MainActivity, PayUpNavHost(), Routes, PayUpTheme()

### Community 2 - "Account"
Cohesion: 0.12
Nodes (3): AccountDao, Account, AccountRepository

### Community 3 - "Category Persistence"
Cohesion: 0.11
Nodes (3): CategoryDao, Category, CategoryRepository

### Community 4 - "Merchant Rule Persistence"
Cohesion: 0.13
Nodes (3): MerchantRuleDao, MerchantRule, MerchantCategorizer

### Community 5 - "DashboardScreen.kt"
Cohesion: 0.05
Nodes (29): PayUpBadge(), PayUpCard(), PayUpEmptyState(), PayUpLinearProgress(), PayUpOutlinedButton(), PayUpPill(), PayUpPrimaryButton(), PayUpSectionHeader() (+21 more)

### Community 7 - "Dashboard View Model"
Cohesion: 0.13
Nodes (7): BudgetUiModel, CategoryBudgetInfo, DashboardState, DashboardViewModel, Tuple3, Tuple4, DateUtils

### Community 10 - "Notification Parser Service"
Cohesion: 0.24
Nodes (6): Confirmed, Incomplete, NotificationParser, ParsedTransaction, ParseResult, NotificationParserTest

### Community 11 - "TransactionType"
Cohesion: 0.33
Nodes (4): TransactionType, EXPENSE, INCOME, TRANSFER

### Community 14 - "TransactionStatus"
Cohesion: 0.40
Nodes (5): TransactionStatus, COMPLETED, FAILED, PENDING, REFUNDED

### Community 15 - "🌟 Key Features"
Cohesion: 0.11
Nodes (17): 🔒 100% Offline & Private, Building from Source, 🤝 Contributing, 📊 Envelope Budgeting & Salary Allocations, 💰 Exact Integer Accounting (Paise Precision), 🚀 Getting Started, 📩 Intelligent On-Device SMS Detection, 🌟 Key Features (+9 more)

### Community 16 - ".generateHash"
Cohesion: 0.14
Nodes (3): DedupService, AccountingCalculationTest, DedupServiceTest

### Community 24 - "Account Model & Types"
Cohesion: 0.33
Nodes (5): AccountType, BANK, CASH, CREDIT_CARD, WALLET

### Community 25 - "PayUpDatabase"
Cohesion: 0.06
Nodes (7): PayUpDatabase, Callback, PayUpApplication, PayUpNotificationListenerService, PayUpSmsReceiver, TransactionListFilter, TransactionListViewModel

### Community 26 - ".parse"
Cohesion: 0.35
Nodes (3): ParsedSms, SmsParser, SmsParserTest

### Community 29 - "ScreenshotReviewViewModel.kt"
Cohesion: 0.08
Nodes (5): ScreenshotCandidate, ScreenshotParser, ScreenshotReviewState, ScreenshotReviewViewModel, ScreenshotParserTest

### Community 30 - "Project Architecture Concepts"
Cohesion: 0.31
Nodes (9): Canonical Cash Deduplication, Custom Budgets & Category Mappings, Deterministic Paise Accounting, Expandable Detail Views & Transaction Edit, On-Device Screenshot Import (ML Kit OCR), Multi-Income Allocations & Rollover, PayUp Android MVP v0.3.0, PendingObservation Table & Entity (+1 more)

### Community 31 - "ObservationStatus"
Cohesion: 0.50
Nodes (4): ObservationStatus, DISMISSED, PENDING_REVIEW, RESOLVED

### Community 33 - "DetectionMethod"
Cohesion: 0.20
Nodes (6): DetectionMethod, CSV_IMPORT, MANUAL, NOTIFICATION, SCREENSHOT_OCR, SMS

## Knowledge Gaps
- **22 isolated node(s):** `BANK`, `WALLET`, `CASH`, `CREDIT_CARD`, `PENDING_REVIEW` (+17 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 178 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **18 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PayUpDatabase` connect `PayUpDatabase` to `Transaction Data Access`, `Account`, `Category Persistence`, `Merchant Rule Persistence`, `IncomeSource`, `Dashboard View Model`, `Pending Observation Persistence`, `Room Type Converters`, `Add Transaction ViewModel`, `Budget`, `TransactionDetailViewModel.kt`, `ScreenshotReviewViewModel.kt`?**
  _High betweenness centrality (0.199) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `Transaction` (e.g. with `.onNotificationPosted()` and `.save()`) actually correct?**
  _`Transaction` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `BANK`, `WALLET`, `CASH` to the rest of the system?**
  _22 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Transaction Data Access` be split into smaller, more focused modules?**
  _Cohesion score 0.06811263318112633 - nodes in this community are weakly interconnected._
- **Why does `Transaction` connect `Transaction Data Access` to `DetectionMethod`, `DashboardScreen.kt`, `Dashboard View Model`, `TransactionType`, `TransactionStatus`, `.generateHash`, `PayUpDatabase`, `TransactionDetailViewModel.kt`, `ScreenshotReviewViewModel.kt`?**
  _High betweenness centrality (0.171) - this node is a cross-community bridge._
- **Should `App Navigation & Entry` be split into smaller, more focused modules?**
  _Cohesion score 0.1380952380952381 - nodes in this community are weakly interconnected._
- **Why does `Account` connect `Account` to `DashboardScreen.kt`, `Dashboard View Model`, `Add Transaction ViewModel`, `Account Model & Types`, `PayUpDatabase`, `TransactionDetailViewModel.kt`, `ScreenshotReviewViewModel.kt`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._