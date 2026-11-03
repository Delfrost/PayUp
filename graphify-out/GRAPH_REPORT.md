# Graph Report - PayUp  (2026-11-03)

## Corpus Check
- 61 files · ~18,466 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 13 file(s) not represented in the graph (top: .xml 6, (none) 2, .properties 2)

## Summary
- 618 nodes · 1356 edges · 34 communities (18 shown, 16 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 15 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Transaction Data Access
- App Navigation & Entry
- Account Persistence
- Category Persistence
- Merchant Rule Persistence
- DashboardScreen.kt
- Budget
- Dashboard View Model
- Pending Observation Persistence
- CurrencyUtils
- Notification Parser Service
- TransactionType
- TransactionStatus
- .generateHash
- Room Type Converters
- TransactionListViewModel
- Add Transaction ViewModel
- Account Model & Types
- PayUpSmsReceiver.kt
- .parse
- ScreenshotReviewViewModel.kt
- Project Architecture Concepts
- Transaction Detection Domain

## God Nodes (most connected - your core abstractions)
1. `Transaction` - 61 edges
2. `TransactionRepository` - 42 edges
3. `Category` - 35 edges
4. `PayUpDatabase` - 33 edges
5. `TransactionDao` - 30 edges
6. `Account` - 26 edges
7. `Budget` - 26 edges
8. `AddTransactionViewModel` - 26 edges
9. `TransactionType` - 24 edges
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

## Communities (34 total, 16 thin omitted)

### Community 0 - "Transaction Data Access"
Cohesion: 0.07
Nodes (6): AccountNetBalance, AccountSpend, CategorySpend, TransactionDao, Transaction, TransactionRepository

### Community 1 - "App Navigation & Entry"
Cohesion: 0.11
Nodes (4): MainActivity, PayUpNavHost(), Routes, PayUpTheme()

### Community 2 - "Account Persistence"
Cohesion: 0.06
Nodes (8): AccountDao, PayUpDatabase, Callback, Account, PayUpApplication, AccountRepository, TransactionDetailState, TransactionDetailViewModel

### Community 3 - "Category Persistence"
Cohesion: 0.11
Nodes (3): CategoryDao, Category, CategoryRepository

### Community 4 - "Merchant Rule Persistence"
Cohesion: 0.13
Nodes (3): MerchantRuleDao, MerchantRule, MerchantCategorizer

### Community 5 - "DashboardScreen.kt"
Cohesion: 0.05
Nodes (15): AccountPill(), AddTransactionScreen(), CategoryChip(), CategoryTransactionsScreen(), CategoryPaymentItem(), DashboardScreen(), ExpandableBudgetCard(), ExpandableCategoryCard() (+7 more)

### Community 6 - "Budget"
Cohesion: 0.06
Nodes (5): BudgetDao, IncomeSourceDao, Budget, IncomeSource, BudgetRepository

### Community 7 - "Dashboard View Model"
Cohesion: 0.12
Nodes (7): BudgetUiModel, CategoryBudgetInfo, DashboardState, DashboardViewModel, Tuple3, Tuple4, DateUtils

### Community 8 - "Pending Observation Persistence"
Cohesion: 0.12
Nodes (6): PendingObservationDao, ObservationStatus, DISMISSED, PENDING_REVIEW, RESOLVED, PendingObservation

### Community 10 - "Notification Parser Service"
Cohesion: 0.24
Nodes (6): Confirmed, Incomplete, NotificationParser, ParsedTransaction, ParseResult, NotificationParserTest

### Community 11 - "TransactionType"
Cohesion: 0.40
Nodes (4): TransactionType, EXPENSE, INCOME, TRANSFER

### Community 14 - "TransactionStatus"
Cohesion: 0.33
Nodes (5): TransactionStatus, COMPLETED, FAILED, PENDING, REFUNDED

### Community 16 - ".generateHash"
Cohesion: 0.10
Nodes (6): DedupService, DetailItem(), EditTransactionSheet(), TransactionDetailScreen(), AccountingCalculationTest, DedupServiceTest

### Community 24 - "Account Model & Types"
Cohesion: 0.33
Nodes (5): AccountType, BANK, CASH, CREDIT_CARD, WALLET

### Community 26 - ".parse"
Cohesion: 0.35
Nodes (3): ParsedSms, SmsParser, SmsParserTest

### Community 29 - "ScreenshotReviewViewModel.kt"
Cohesion: 0.12
Nodes (5): ScreenshotCandidate, ScreenshotParser, ScreenshotReviewState, ScreenshotReviewViewModel, ScreenshotParserTest

### Community 30 - "Project Architecture Concepts"
Cohesion: 0.31
Nodes (9): Canonical Cash Deduplication, Custom Budgets & Category Mappings, Deterministic Paise Accounting, Expandable Detail Views & Transaction Edit, On-Device Screenshot Import (ML Kit OCR), Multi-Income Allocations & Rollover, PayUp Android MVP v0.3.0, PendingObservation Table & Entity (+1 more)

### Community 33 - "Transaction Detection Domain"
Cohesion: 0.33
Nodes (6): DetectionMethod, CSV_IMPORT, MANUAL, NOTIFICATION, SCREENSHOT_OCR, SMS

## Knowledge Gaps
- **22 isolated node(s):** `BANK`, `WALLET`, `CASH`, `CREDIT_CARD`, `PENDING_REVIEW` (+17 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 160 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PayUpDatabase` connect `Account Persistence` to `Transaction Data Access`, `Category Persistence`, `Merchant Rule Persistence`, `Budget`, `Dashboard View Model`, `Pending Observation Persistence`, `Room Type Converters`, `TransactionListViewModel`, `Add Transaction ViewModel`, `PayUpSmsReceiver.kt`, `ScreenshotReviewViewModel.kt`?**
  _High betweenness centrality (0.225) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `Transaction` (e.g. with `.onNotificationPosted()` and `.save()`) actually correct?**
  _`Transaction` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `BANK`, `WALLET`, `CASH` to the rest of the system?**
  _22 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Transaction Data Access` be split into smaller, more focused modules?**
  _Cohesion score 0.06811263318112633 - nodes in this community are weakly interconnected._
- **Why does `Transaction` connect `Transaction Data Access` to `Transaction Detection Domain`, `Account Persistence`, `DashboardScreen.kt`, `Dashboard View Model`, `CurrencyUtils`, `TransactionType`, `TransactionStatus`, `.generateHash`, `TransactionListViewModel`, `PayUpSmsReceiver.kt`, `ScreenshotReviewViewModel.kt`?**
  _High betweenness centrality (0.190) - this node is a cross-community bridge._
- **Should `App Navigation & Entry` be split into smaller, more focused modules?**
  _Cohesion score 0.11333333333333333 - nodes in this community are weakly interconnected._
- **Why does `TransactionRepository` connect `Transaction Data Access` to `Account Persistence`, `Dashboard View Model`, `TransactionListViewModel`, `Add Transaction ViewModel`, `PayUpSmsReceiver.kt`, `ScreenshotReviewViewModel.kt`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._