package com.payup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.payup.app.data.db.PayUpDatabase
import com.payup.app.data.model.Category
import com.payup.app.data.model.Account
import com.payup.app.data.model.Transaction
import com.payup.app.data.model.TransactionType
import com.payup.app.data.model.TransactionStatus
import com.payup.app.data.model.DetectionMethod
import com.payup.app.repository.AccountRepository
import com.payup.app.repository.CategoryRepository
import com.payup.app.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

import com.payup.app.data.model.Budget
import com.payup.app.repository.BudgetRepository

data class TransactionDetailState(
    val transaction: Transaction? = null,
    val category: Category? = null,
    val budget: Budget? = null,
    val account: Account? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

class TransactionDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PayUpDatabase.getInstance(application)
    private val transactionRepo = TransactionRepository(db.transactionDao())
    private val categoryRepo = CategoryRepository(db.categoryDao())
    private val accountRepo = AccountRepository(db.accountDao())
    private val budgetRepo = BudgetRepository(db.budgetDao(), db.incomeSourceDao())

    private val _state = MutableStateFlow(TransactionDetailState())
    val state: StateFlow<TransactionDetailState> = _state.asStateFlow()
    private var isDeleting = false

    val categories: Flow<List<Category>> = categoryRepo.getAll()
    val accounts: Flow<List<Account>> = accountRepo.getAll()
    val budgets: Flow<List<Budget>> = budgetRepo.getAllActiveBudgets()

    fun loadTransaction(id: String) {
        viewModelScope.launch {
            transactionRepo.observeById(id).collect { txn ->
                if (txn != null) {
                    val cat = txn.categoryId?.let { categoryRepo.getById(it) }
                    val acc = txn.accountId?.let { accountRepo.getById(it) }
                    val bud = txn.budgetId?.let { budgetRepo.getBudgetById(it) } 
                        ?: cat?.budgetId?.let { budgetRepo.getBudgetById(it) }
                    _state.value = TransactionDetailState(
                        transaction = txn,
                        category = cat,
                        budget = bud,
                        account = acc,
                        isLoading = false
                    )
                } else if (!isDeleting) {
                    _state.value = TransactionDetailState(isLoading = false, error = "Transaction not found")
                }
            }
        }
    }

    fun updateTransaction(
        amountPaise: Long? = null,
        merchant: String? = null,
        categoryId: String? = null,
        budgetId: String? = null,
        accountId: String? = null,
        notes: String? = null,
        type: TransactionType? = null,
        status: TransactionStatus? = null
    ) {
        val current = _state.value.transaction ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val updated = current.copy(
                amountPaise = amountPaise ?: current.amountPaise,
                merchant = merchant ?: current.merchant,
                categoryId = categoryId ?: current.categoryId,
                budgetId = budgetId ?: current.budgetId,
                accountId = accountId ?: current.accountId,
                notes = notes ?: current.notes,
                type = type ?: current.type,
                status = status ?: current.status,
                updatedAt = Instant.now()
            )
            transactionRepo.update(updated)
            _state.value = _state.value.copy(isSaving = false)
        }
    }

    fun deleteTransaction() {
        val current = _state.value.transaction ?: return
        isDeleting = true
        viewModelScope.launch {
            transactionRepo.delete(current)
        }
    }
}
