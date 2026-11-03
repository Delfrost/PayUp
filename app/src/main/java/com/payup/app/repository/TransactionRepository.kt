package com.payup.app.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.payup.app.data.db.CategorySpend
import com.payup.app.data.db.TransactionDao
import com.payup.app.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {

    companion object {
        private const val PAGE_SIZE = 30
    }

    suspend fun insert(transaction: Transaction) = dao.insert(transaction)

    suspend fun insertIfNotDuplicate(transaction: Transaction): Boolean {
        return if (transaction.dedupHash != null) {
            dao.insertIfNotExists(transaction) != -1L
        } else {
            dao.insert(transaction)
            true
        }
    }

    suspend fun update(transaction: Transaction) = dao.update(transaction)

    suspend fun delete(transaction: Transaction) = dao.delete(transaction)

    suspend fun getById(id: String): Transaction? = dao.getById(id)

    fun observeById(id: String): Flow<Transaction?> = dao.observeById(id)

    fun allTransactionsPaged(): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingSource() }
    ).flow

    fun transactionsByCategoryPaged(categoryId: String): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingSourceByCategory(categoryId) }
    ).flow

    fun transactionsByBudgetPaged(budgetId: String): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingSourceByBudget(budgetId) }
    ).flow

    fun transactionsByAccountPaged(accountId: String): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { dao.pagingSourceByAccount(accountId) }
    ).flow

    fun searchPaged(query: String): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { dao.search(query) }
    ).flow

    fun searchByCategoryPaged(categoryId: String, query: String): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { dao.searchByCategory(categoryId, query) }
    ).flow

    fun totalIncome(startMillis: Long, endMillis: Long): Flow<Long> =
        dao.totalIncome(startMillis, endMillis)

    fun totalExpenses(startMillis: Long, endMillis: Long): Flow<Long> =
        dao.totalExpenses(startMillis, endMillis)

    fun spendByCategory(startMillis: Long, endMillis: Long): Flow<List<CategorySpend>> =
        dao.spendByCategory(startMillis, endMillis)

    fun recentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        dao.recentTransactions(limit)

    fun transactionCount(): Flow<Int> = dao.transactionCount()

    fun getByDateRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        dao.getByDateRange(startMillis, endMillis)

    fun getByCategoryAndDateRange(categoryId: String, startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        dao.getByCategoryAndDateRange(categoryId, startMillis, endMillis)

    fun getByBudgetAndDateRange(budgetId: String, startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        dao.getByBudgetAndDateRange(budgetId, startMillis, endMillis)

    fun spendByBudget(budgetId: String, startMillis: Long, endMillis: Long): Flow<Long> =
        dao.spendByBudget(budgetId, startMillis, endMillis)

    suspend fun findByDedupHash(hash: String): Transaction? = dao.findByDedupHash(hash)

    suspend fun findByReferenceId(refId: String): Transaction? = dao.findByReferenceId(refId)

    fun netBalancesByAccount() = dao.netBalancesByAccount()

    fun spendByAccount(startMillis: Long, endMillis: Long) = dao.spendByAccount(startMillis, endMillis)
}
