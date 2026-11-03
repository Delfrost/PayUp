# PayUp — Project State

## Status: Android MVP v0.3.1 Quality Audit Fixes Applied & Verified

### Architecture & Improvements
- **Tech Stack:** Kotlin, Jetpack Compose, Room (v2.6.1 + room-paging), Material 3, Navigation Compose, Paging 3, KSP, Google ML Kit (Text Recognition).
- **Data Integrity:** Deterministic integer accounting in paise (`Long`). Zero floating-point drift with `BigDecimal` scaling.
- **Sync Ready:** Stable UUIDs (`id`), timestamps in UTC `Instant`, dedup hash generation via SHA-256 for future web syncing.
- **Lean Architecture:** Unused dead code (`CsvImportService`, opencsv dependency, unreferenced entities/DAOs) removed.

### Completed Fixes & Additions (v0.3.1 Quality Audit)

1. **Migration & DB Seeding Stability (AUDIT-01 & AUDIT-02):**
   - Corrected Room `MIGRATION_1_2` schema: `created_at` and `timestamp` updated from `TEXT NOT NULL` to `INTEGER NOT NULL` to align with Room's `Instant` converter.
   - Fixed database initialization in `getInstance`: default categories, accounts, and budgets seed reliably on first install.

2. **Deterministic Paise Arithmetic & Formatter Thread Safety (AUDIT-03 & AUDIT-12):**
   - Eliminated `(amount.toDouble() * 100).toLong()` across `CurrencyUtils`, `SmsParser`, `ScreenshotParser`, `AddTransactionViewModel`, and `TransactionDetailScreen`.
   - Used `BigDecimal` with exact decimal scaling; guaranteed no 1-paisa drift.
   - Wrapped `CurrencyUtils` number formatter in `ThreadLocal` for thread safety across Compose recomposition and Coroutines dispatchers.

3. **SMS Ingestion & Credit/Debit Accuracy (AUDIT-04 & AUDIT-05):**
   - Refined `SmsParser` classification: incoming bank transfers with `"sent by"` clauses now properly resolve to `INCOME`.
   - Fixed regex so legitimate payment alerts containing `"QR code"` are not discarded as spam.

4. **Alert Deduplication & Ingestion Ordering (AUDIT-06 & AUDIT-07):**
   - `PayUpNotificationListenerService` now checks `findByReferenceId` prior to inserting to avoid duplicating transactions when Bank SMS arrives first.
   - `DedupService` uses the canonical reference ID (UTR) when available and rolling 5-minute buckets when absent to prevent cross-minute duplicate splits.

5. **Coroutine Lifecycle & Compose Performance (AUDIT-08 & AUDIT-09):**
   - `DashboardViewModel` cancels active observer jobs before launching new monthly collections in `loadDashboard()`, preventing job leaks and race conditions on month switches.
   - `DashboardScreen` only subscribes to budget and category payment flows when cards are expanded, removing 19 continuous idle observers from the dashboard.

6. **Search Filtering, Clean Deletions & Native Cleanup (AUDIT-10, AUDIT-11, AUDIT-14):**
   - Added `searchByCategoryPaged` so searching within category view filters exclusively within that category.
   - Avoided error flashing in `TransactionDetailScreen` upon intentional transaction deletions.
   - Handled `textRecognizer.close()` in `ScreenshotReviewViewModel.onCleared()` to free native ML Kit models.

7. **Codebase Slimming (AUDIT-15 & AUDIT-16):**
   - Removed `com.opencsv:opencsv:5.9` dependency and dead service files.
   - Cleaned up unreferenced legacy entities (`SavingsGoal`, `RecurringRule`, `BudgetAllocation`).
