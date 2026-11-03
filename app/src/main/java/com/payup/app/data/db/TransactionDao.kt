package com.payup.app.data.db

import androidx.paging.PagingSource
import androidx.room.*
import com.payup.app.data.model.Transaction
import com.payup.app.data.model.TransactionStatus
import com.payup.app.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class CategorySpend(
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "total") val total: Long
)

data class AccountNetBalance(
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "net_paise") val netPaise: Long
)

data class AccountSpend(
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "total_paise") val totalPaise: Long
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): Transaction?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: String): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE status != 'FAILED' ORDER BY timestamp DESC")
    fun pagingSource(): PagingSource<Int, Transaction>

    @Query("SELECT * FROM transactions WHERE status != 'FAILED' AND category_id = :categoryId ORDER BY timestamp DESC")
    fun pagingSourceByCategory(categoryId: String): PagingSource<Int, Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE status != 'FAILED' 
        AND (budget_id = :budgetId OR (budget_id IS NULL AND category_id IN (SELECT id FROM categories WHERE budget_id = :budgetId)))
        ORDER BY timestamp DESC
    """)
    fun pagingSourceByBudget(budgetId: String): PagingSource<Int, Transaction>

    @Query("SELECT * FROM transactions WHERE status != 'FAILED' AND account_id = :accountId ORDER BY timestamp DESC")
    fun pagingSourceByAccount(accountId: String): PagingSource<Int, Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE status != 'FAILED' 
        AND (merchant LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR reference_id LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun search(query: String): PagingSource<Int, Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE status != 'FAILED' AND category_id = :categoryId
        AND (merchant LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR reference_id LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchByCategory(categoryId: String, query: String): PagingSource<Int, Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE status != 'FAILED' 
        AND timestamp >= :startMillis AND timestamp <= :endMillis
        ORDER BY timestamp DESC
    """)
    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(SUM(amount_paise), 0) FROM transactions 
        WHERE type = 'INCOME' AND status = 'COMPLETED'
        AND timestamp >= :startMillis AND timestamp <= :endMillis
    """)
    fun totalIncome(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount_paise), 0) FROM transactions 
        WHERE type = 'EXPENSE' AND status = 'COMPLETED'
        AND timestamp >= :startMillis AND timestamp <= :endMillis
    """)
    fun totalExpenses(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("""
        SELECT category_id, COALESCE(SUM(amount_paise), 0) as total 
        FROM transactions 
        WHERE type = 'EXPENSE' AND status = 'COMPLETED'
        AND timestamp >= :startMillis AND timestamp <= :endMillis
        GROUP BY category_id
    """)
    fun spendByCategory(startMillis: Long, endMillis: Long): Flow<List<CategorySpend>>

    @Query("SELECT * FROM transactions WHERE dedup_hash = :hash LIMIT 1")
    suspend fun findByDedupHash(hash: String): Transaction?

    @Query("SELECT * FROM transactions WHERE reference_id = :refId AND status != 'FAILED' LIMIT 1")
    suspend fun findByReferenceId(refId: String): Transaction?

    @Query("SELECT COUNT(*) FROM transactions WHERE status != 'FAILED'")
    fun transactionCount(): Flow<Int>

    @Query("""
        SELECT * FROM transactions 
        WHERE status != 'FAILED' AND category_id = :categoryId 
        AND timestamp >= :startMillis AND timestamp <= :endMillis
        ORDER BY timestamp DESC
    """)
    fun getByCategoryAndDateRange(categoryId: String, startMillis: Long, endMillis: Long): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE status != 'FAILED' 
        AND (budget_id = :budgetId OR (budget_id IS NULL AND category_id IN (SELECT id FROM categories WHERE budget_id = :budgetId)))
        AND timestamp >= :startMillis AND timestamp <= :endMillis
        ORDER BY timestamp DESC
    """)
    fun getByBudgetAndDateRange(budgetId: String, startMillis: Long, endMillis: Long): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(SUM(amount_paise), 0)
        FROM transactions 
        WHERE type = 'EXPENSE' AND status = 'COMPLETED'
        AND (budget_id = :budgetId OR (budget_id IS NULL AND category_id IN (SELECT id FROM categories WHERE budget_id = :budgetId)))
        AND timestamp >= :startMillis AND timestamp <= :endMillis
    """)
    fun spendByBudget(budgetId: String, startMillis: Long, endMillis: Long): Flow<Long>

    // For recent transactions on dashboard
    @Query("SELECT * FROM transactions WHERE status != 'FAILED' ORDER BY timestamp DESC LIMIT :limit")
    fun recentTransactions(limit: Int = 10): Flow<List<Transaction>>

    @Query("""
        SELECT account_id,
               COALESCE(SUM(CASE 
                   WHEN type = 'INCOME' THEN amount_paise 
                   WHEN type = 'EXPENSE' THEN -amount_paise 
                   ELSE 0 
               END), 0) as net_paise
        FROM transactions
        WHERE status = 'COMPLETED' AND account_id IS NOT NULL
        GROUP BY account_id
    """)
    fun netBalancesByAccount(): Flow<List<AccountNetBalance>>

    @Query("""
        SELECT account_id, COALESCE(SUM(amount_paise), 0) as total_paise
        FROM transactions
        WHERE type = 'EXPENSE' AND status = 'COMPLETED' AND account_id IS NOT NULL
        AND timestamp >= :startMillis AND timestamp <= :endMillis
        GROUP BY account_id
    """)
    fun spendByAccount(startMillis: Long, endMillis: Long): Flow<List<AccountSpend>>
}
