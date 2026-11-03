package com.payup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.payup.app.data.db.PayUpDatabase
import com.payup.app.data.model.Transaction
import com.payup.app.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class TransactionListFilter(
    val categoryId: String? = null,
    val accountId: String? = null,
    val searchQuery: String? = null
)

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PayUpDatabase.getInstance(application)
    private val transactionRepo = TransactionRepository(db.transactionDao())

    private val _filter = MutableStateFlow(TransactionListFilter())
    val filter: StateFlow<TransactionListFilter> = _filter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: Flow<PagingData<Transaction>> = _filter.flatMapLatest { f ->
        val query = f.searchQuery?.trim()
        when {
            !query.isNullOrBlank() && f.categoryId != null ->
                transactionRepo.searchByCategoryPaged(f.categoryId, query)
            !query.isNullOrBlank() ->
                transactionRepo.searchPaged(query)
            f.categoryId != null ->
                transactionRepo.transactionsByCategoryPaged(f.categoryId)
            f.accountId != null ->
                transactionRepo.transactionsByAccountPaged(f.accountId)
            else ->
                transactionRepo.allTransactionsPaged()
        }
    }.cachedIn(viewModelScope)

    val transactionCount: Flow<Int> = transactionRepo.transactionCount()

    fun setFilter(filter: TransactionListFilter) {
        _filter.value = filter
    }

    fun setSearch(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query.ifBlank { null })
    }

    fun setCategoryFilter(categoryId: String?) {
        _filter.value = TransactionListFilter(categoryId = categoryId)
    }

    fun clearFilter() {
        _filter.value = TransactionListFilter()
    }
}
