package com.payup.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.payup.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Account::class,
        MerchantRule::class,
        Budget::class,
        IncomeSource::class,
        PendingObservation::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PayUpDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun merchantRuleDao(): MerchantRuleDao
    abstract fun budgetDao(): BudgetDao
    abstract fun incomeSourceDao(): IncomeSourceDao
    abstract fun pendingObservationDao(): PendingObservationDao

    companion object {
        @Volatile
        private var INSTANCE: PayUpDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE categories ADD COLUMN budget_id TEXT DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN budget_id TEXT DEFAULT NULL")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_budget_id ON transactions (budget_id)")
                } catch (_: Exception) {}

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        allocated_paise INTEGER NOT NULL,
                        order_index INTEGER NOT NULL,
                        color INTEGER NOT NULL,
                        icon TEXT NOT NULL,
                        is_rollover_enabled INTEGER NOT NULL,
                        is_archived INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS income_sources (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        expected_paise INTEGER NOT NULL,
                        is_recurring INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_observations (
                        id TEXT NOT NULL PRIMARY KEY,
                        amount_paise INTEGER DEFAULT NULL,
                        merchant TEXT DEFAULT NULL,
                        reference_id TEXT DEFAULT NULL,
                        source TEXT NOT NULL,
                        raw_content TEXT NOT NULL,
                        status TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop legacy tables that were removed in the ponytail audit
                try {
                    db.execSQL("DROP TABLE IF EXISTS savings_goals")
                } catch (_: Exception) {}
                try {
                    db.execSQL("DROP TABLE IF EXISTS recurring_rules")
                } catch (_: Exception) {}
                try {
                    db.execSQL("DROP TABLE IF EXISTS budget_allocations")
                } catch (_: Exception) {}
            }
        }

        fun getInstance(context: Context): PayUpDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PayUpDatabase::class.java,
                    "payup.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Safe migration for BUG-02: deduplicate legacy Cash accounts into canonical 'acc_cash'
                        try {
                            db.execSQL("""
                                UPDATE transactions 
                                SET account_id = 'acc_cash' 
                                WHERE account_id IN (
                                    SELECT id FROM accounts 
                                    WHERE (type = 'CASH' OR LOWER(name) = 'cash') AND id != 'acc_cash'
                                )
                            """.trimIndent())
                            db.execSQL("""
                                DELETE FROM accounts 
                                WHERE (type = 'CASH' OR LOWER(name) = 'cash') AND id != 'acc_cash'
                            """.trimIndent())
                        } catch (_: Exception) {}
                    }
                })
                .build()
                .also { database ->
                    INSTANCE = database
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            seedDefaultCategories(database.categoryDao())
                            seedDefaultAccountsIfNeeded(database.accountDao())
                            seedDefaultBudgetsAndIncomesIfNeeded(
                                database.budgetDao(),
                                database.incomeSourceDao(),
                                database.categoryDao()
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("PayUpDatabase", "Error seeding default database data", e)
                        }
                    }
                }
            }
        }

        private suspend fun seedDefaultCategories(dao: CategoryDao) {
            if (dao.count() > 0) return
            val defaults = listOf(
                Category(name = "Food & Dining", icon = "restaurant", color = 0xFFE91E63, sortOrder = 0, isDefault = true),
                Category(name = "Transport", icon = "directions_car", color = 0xFF2196F3, sortOrder = 1, isDefault = true),
                Category(name = "Shopping", icon = "shopping_bag", color = 0xFF9C27B0, sortOrder = 2, isDefault = true),
                Category(name = "Bills & Utilities", icon = "receipt_long", color = 0xFFFF9800, sortOrder = 3, isDefault = true),
                Category(name = "Entertainment", icon = "movie", color = 0xFF4CAF50, sortOrder = 4, isDefault = true),
                Category(name = "Health", icon = "local_hospital", color = 0xFFF44336, sortOrder = 5, isDefault = true),
                Category(name = "Education", icon = "school", color = 0xFF3F51B5, sortOrder = 6, isDefault = true),
                Category(name = "Groceries", icon = "local_grocery_store", color = 0xFF8BC34A, sortOrder = 7, isDefault = true),
                Category(name = "Rent", icon = "home", color = 0xFF795548, sortOrder = 8, isDefault = true),
                Category(name = "Subscriptions", icon = "subscriptions", color = 0xFF607D8B, sortOrder = 9, isDefault = true),
                Category(name = "Salary", icon = "payments", color = 0xFF4CAF50, sortOrder = 0, isIncome = true, isDefault = true),
                Category(name = "Freelance", icon = "work", color = 0xFF00BCD4, sortOrder = 1, isIncome = true, isDefault = true),
                Category(name = "Other Income", icon = "account_balance_wallet", color = 0xFF009688, sortOrder = 2, isIncome = true, isDefault = true),
                Category(name = "Uncategorized", icon = "help_outline", color = 0xFF9E9E9E, sortOrder = 99, isDefault = true)
            )
            dao.insertAll(defaults)
        }

        private suspend fun seedDefaultAccountsIfNeeded(dao: AccountDao) {
            val defaults = listOf(
                Account(id = "acc_upi", name = "UPI / Google Pay", type = AccountType.WALLET, icon = "payments", color = 0xFF1E88E5),
                Account(id = "acc_bank", name = "Primary Bank", type = AccountType.BANK, icon = "account_balance", color = 0xFF0D47A1),
                Account(id = "acc_cash", name = "Cash", type = AccountType.CASH, icon = "money", color = 0xFF4CAF50),
                Account(id = "acc_card", name = "Credit Card", type = AccountType.CREDIT_CARD, icon = "credit_card", color = 0xFFE53935)
            )
            defaults.forEach { acc ->
                if (dao.getById(acc.id) == null) {
                    dao.insert(acc)
                }
            }
        }

        private suspend fun seedDefaultBudgetsAndIncomesIfNeeded(
            budgetDao: BudgetDao,
            incomeDao: IncomeSourceDao,
            categoryDao: CategoryDao
        ) {
            if (budgetDao.count() == 0) {
                val budgets = listOf(
                    Budget(id = "b_personal", name = "Personal", allocatedPaise = 500000L, orderIndex = 0, color = 0xFFE91E63, icon = "person"),
                    Budget(id = "b_rent", name = "Rent", allocatedPaise = 1000000L, orderIndex = 1, color = 0xFF795548, icon = "home"),
                    Budget(id = "b_travel", name = "Intercity Travel", allocatedPaise = 200000L, orderIndex = 2, color = 0xFF2196F3, icon = "directions_car"),
                    Budget(id = "b_savings", name = "Savings", allocatedPaise = 500000L, orderIndex = 3, color = 0xFF4CAF50, icon = "savings"),
                    Budget(id = "b_other", name = "Other", allocatedPaise = 300000L, orderIndex = 4, color = 0xFF9C27B0, icon = "more_horiz")
                )
                budgetDao.insertAll(budgets)

                // Map default categories to initial budgets
                val categories = categoryDao.getAllList()
                categories.forEach { cat ->
                    val budgetId = when (cat.name.lowercase()) {
                        "food & dining", "shopping", "entertainment" -> "b_personal"
                        "rent" -> "b_rent"
                        "transport" -> "b_travel"
                        "bills & utilities", "groceries", "health", "education", "subscriptions", "uncategorized" -> "b_other"
                        else -> null
                    }
                    if (budgetId != null && cat.budgetId == null) {
                        categoryDao.assignCategoryToBudget(cat.id, budgetId)
                    }
                }
            }

            if (incomeDao.count() == 0) {
                incomeDao.insert(
                    IncomeSource(id = "inc_salary", name = "Salary", expectedPaise = 2500000L, isRecurring = true)
                )
            }
        }
    }
}
