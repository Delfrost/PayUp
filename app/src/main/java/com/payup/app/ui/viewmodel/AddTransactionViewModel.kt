package com.payup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.payup.app.data.db.PayUpDatabase
import com.payup.app.data.model.*
import com.payup.app.repository.AccountRepository
import com.payup.app.repository.CategoryRepository
import com.payup.app.repository.TransactionRepository
import com.payup.app.service.DedupService
import com.payup.app.service.MerchantCategorizer
import com.payup.app.util.CurrencyUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

import com.payup.app.data.model.Budget
import com.payup.app.repository.BudgetRepository

data class AddTransactionState(
    val amountText: String = "",
    val merchant: String = "",
    val selectedCategoryId: String? = null,
    val selectedBudgetId: String? = null,
    val selectedAccountId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val notes: String = "",
    val timestamp: Instant = Instant.now(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PayUpDatabase.getInstance(application)
    private val transactionRepo = TransactionRepository(db.transactionDao())
    private val categoryRepo = CategoryRepository(db.categoryDao())
    private val accountRepo = AccountRepository(db.accountDao())
    private val budgetRepo = BudgetRepository(db.budgetDao(), db.incomeSourceDao())
    private val merchantCategorizer = MerchantCategorizer(db.merchantRuleDao())

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    val categories: Flow<List<Category>> = categoryRepo.getAll()
    val accounts: Flow<List<Account>> = accountRepo.getAll()
    val budgets: Flow<List<Budget>> = budgetRepo.getAllActiveBudgets()

    fun updateAmount(text: String) {
        // Only allow valid numeric input
        if (text.isEmpty() || text.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
            _state.value = _state.value.copy(amountText = text)
        }
    }

    fun updateMerchant(merchant: String) {
        _state.value = _state.value.copy(merchant = merchant)
        // Auto-categorize on merchant change
        viewModelScope.launch {
            val catId = merchantCategorizer.categorize(merchant)
            if (catId != null && _state.value.selectedCategoryId == null) {
                _state.value = _state.value.copy(selectedCategoryId = catId)
            }
        }
    }

    fun updateCategory(categoryId: String?) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    fun updateBudget(budgetId: String?) {
        _state.value = _state.value.copy(selectedBudgetId = budgetId)
    }

    fun updateAccount(accountId: String?) {
        _state.value = _state.value.copy(selectedAccountId = accountId)
    }

    fun updateType(type: TransactionType) {
        _state.value = _state.value.copy(type = type)
    }

    fun updateNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun updateTimestamp(timestamp: Instant) {
        _state.value = _state.value.copy(timestamp = timestamp)
    }

    fun save() {
        val s = _state.value
        val amountPaise = CurrencyUtils.parseToPaise(s.amountText)
        if (amountPaise <= 0) {
            _state.value = _state.value.copy(error = "Amount must be greater than 0")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val dedupHash = DedupService.generateHash(amountPaise, s.merchant, s.timestamp.toEpochMilli())
                val transaction = Transaction(
                    amountPaise = amountPaise,
                    type = s.type,
                    merchant = s.merchant,
                    categoryId = s.selectedCategoryId,
                    budgetId = s.selectedBudgetId,
                    accountId = s.selectedAccountId,
                    timestamp = s.timestamp,
                    notes = s.notes.ifBlank { null },
                    detectionMethod = DetectionMethod.MANUAL,
                    dedupHash = dedupHash
                )
                transactionRepo.insert(transaction)

                // Learn merchant -> category mapping
                if (s.merchant.isNotBlank() && s.selectedCategoryId != null) {
                    merchantCategorizer.addRule(s.merchant, s.selectedCategoryId)
                }

                _state.value = _state.value.copy(isSaving = false, savedSuccessfully = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun reset() {
        _state.value = AddTransactionState()
    }
}
