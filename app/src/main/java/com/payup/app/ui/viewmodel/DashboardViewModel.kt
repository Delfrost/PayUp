package com.payup.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.payup.app.data.db.AccountNetBalance
import com.payup.app.data.db.AccountSpend
import com.payup.app.data.db.CategorySpend
import com.payup.app.data.db.PayUpDatabase
import com.payup.app.data.model.*
import com.payup.app.repository.AccountRepository
import com.payup.app.repository.BudgetRepository
import com.payup.app.repository.CategoryRepository
import com.payup.app.repository.TransactionRepository
import com.payup.app.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryBudgetInfo(
    val category: Category,
    val spentPaise: Long,
    val budgetPaise: Long
)

data class BudgetUiModel(
    val budget: Budget,
    val allocatedPaise: Long,
    val spentPaise: Long,
    val remainingPaise: Long,
    val categories: List<Category>,
    val isOverspent: Boolean,
    val rolloverPaise: Long = 0L
)

data class DashboardState(
    val salaryIncome: Long = 0L,
    val totalIncome: Long = 0L,
    val configuredIncome: Long = 0L,
    val totalAllocatedBudgets: Long = 0L,
    val unallocatedIncome: Long = 0L,
    val isOverAllocated: Boolean = false,
    val totalExpenses: Long = 0L,
    val savings: Long = 0L,
    val totalBalance: Long = 0L,
    val recentTransactions: List<Transaction> = emptyList(),
    val budgets: List<BudgetUiModel> = emptyList(),
    val categorySpends: List<CategoryBudgetInfo> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val paymentMethodSpends: Map<String, Long> = emptyMap(),
    val currentMonth: String = DateUtils.currentYearMonth(),
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PayUpDatabase.getInstance(application)
    private val transactionRepo = TransactionRepository(db.transactionDao())
    private val categoryRepo = CategoryRepository(db.categoryDao())
    private val accountRepo = AccountRepository(db.accountDao())
    private val budgetRepo = BudgetRepository(db.budgetDao(), db.incomeSourceDao())

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _currentMonth = MutableStateFlow(DateUtils.currentYearMonth())
    private var loadJob: Job? = null

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val startMillis = DateUtils.monthStart(_currentMonth.value)
        val endMillis = DateUtils.monthEnd(_currentMonth.value)

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                combine(
                    combine(
                        transactionRepo.totalIncome(startMillis, endMillis),
                        transactionRepo.totalExpenses(startMillis, endMillis),
                        transactionRepo.recentTransactions(10),
                        transactionRepo.netBalancesByAccount()
                    ) { income, expenses, recent, netBalances ->
                        Tuple4(income, expenses, recent, netBalances)
                    },
                    combine(
                        transactionRepo.spendByCategory(startMillis, endMillis),
                        categoryRepo.getAll(),
                        accountRepo.getAll(),
                        transactionRepo.spendByAccount(startMillis, endMillis)
                    ) { categorySpends, categories, accounts, accountSpends ->
                        Tuple4(categorySpends, categories, accounts, accountSpends)
                    },
                    combine(
                        budgetRepo.getAllActiveBudgets(),
                        budgetRepo.getAllIncomeSources(),
                        transactionRepo.getByDateRange(startMillis, endMillis)
                    ) { budgets, incomeSources, monthTxns ->
                        Tuple3(budgets, incomeSources, monthTxns)
                    }
                ) { t1: Tuple4<Long, Long, List<Transaction>, List<AccountNetBalance>>,
                    t2: Tuple4<List<CategorySpend>, List<Category>, List<Account>, List<AccountSpend>>,
                    t3: Tuple3<List<Budget>, List<IncomeSource>, List<Transaction>> ->
                    val (income, expenses, recent, netBalances) = t1
                    val (categorySpends, categories, accounts, accountSpends) = t2
                    val (budgets, incomeSources, monthTxns) = t3

                    val netMap = netBalances.associate { it.accountId to it.netPaise }
                    val spendMap = accountSpends.associate { it.accountId to it.totalPaise }

                    val computedAccounts = accounts.map { acc ->
                        acc.copy(balancePaise = acc.balancePaise + (netMap[acc.id] ?: 0L))
                    }
                    val totalBalance = computedAccounts.sumOf { it.balancePaise }

                    // Category budget calculations
                    val categoryBudgets = buildCategoryBudgets(categories, categorySpends)
                    val salaryCat = categories.find { it.isIncome && it.name.equals("Salary", ignoreCase = true) }
                    val salarySpend = categorySpends.find { it.categoryId == salaryCat?.id }?.total ?: 0L

                    // FEAT-02: Income Allocations calculation
                    val configuredIncome = incomeSources.sumOf { it.expectedPaise }.let {
                        if (it > 0L) it else income
                    }
                    val totalAllocated = budgets.sumOf { it.allocatedPaise }
                    val unallocated = configuredIncome - totalAllocated
                    val isOverAllocated = totalAllocated > configuredIncome && configuredIncome > 0L

                    // FEAT-03: Budget spend calculation
                    val catIdToBudgetId = categories.associate { it.id to it.budgetId }
                    val budgetSpentMap = mutableMapOf<String, Long>()
                    monthTxns.filter { it.type == TransactionType.EXPENSE && it.status == TransactionStatus.COMPLETED }
                        .forEach { txn ->
                            val budgetId = txn.budgetId ?: txn.categoryId?.let { catIdToBudgetId[it] }
                            if (budgetId != null) {
                                budgetSpentMap[budgetId] = (budgetSpentMap[budgetId] ?: 0L) + txn.amountPaise
                            }
                        }

                    val budgetUiModels = budgets.map { b ->
                        val spent = budgetSpentMap[b.id] ?: 0L
                        val assignedCats = categories.filter { it.budgetId == b.id }
                        BudgetUiModel(
                            budget = b,
                            allocatedPaise = b.allocatedPaise,
                            spentPaise = spent,
                            remainingPaise = b.allocatedPaise - spent,
                            categories = assignedCats,
                            isOverspent = spent > b.allocatedPaise
                        )
                    }

                    DashboardState(
                        salaryIncome = salarySpend,
                        totalIncome = income,
                        configuredIncome = configuredIncome,
                        totalAllocatedBudgets = totalAllocated,
                        unallocatedIncome = unallocated,
                        isOverAllocated = isOverAllocated,
                        totalExpenses = expenses,
                        savings = income - expenses,
                        totalBalance = totalBalance,
                        recentTransactions = recent,
                        budgets = budgetUiModels,
                        categorySpends = categoryBudgets,
                        accounts = computedAccounts,
                        paymentMethodSpends = spendMap,
                        currentMonth = _currentMonth.value,
                        isLoading = false
                    )
                }
                .catch { e ->
                    android.util.Log.e("DashboardViewModel", "Error collecting dashboard data", e)
                    _state.value = _state.value.copy(isLoading = false)
                }
                .collect { state ->
                    _state.value = state
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error in loadDashboard", e)
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    private data class Tuple3<A, B, C>(val a: A, val b: B, val c: C)

    fun getCategoryTransactions(categoryId: String): Flow<List<Transaction>> {
        val startMillis = DateUtils.monthStart(_currentMonth.value)
        val endMillis = DateUtils.monthEnd(_currentMonth.value)
        return transactionRepo.getByCategoryAndDateRange(categoryId, startMillis, endMillis)
    }

    fun getBudgetTransactions(budgetId: String): Flow<List<Transaction>> {
        val startMillis = DateUtils.monthStart(_currentMonth.value)
        val endMillis = DateUtils.monthEnd(_currentMonth.value)
        return transactionRepo.getByBudgetAndDateRange(budgetId, startMillis, endMillis)
    }

    fun saveBudget(id: String?, name: String, allocatedPaise: Long, isRollover: Boolean = false) {
        viewModelScope.launch {
            if (id == null) {
                budgetRepo.insertBudget(
                    Budget(name = name, allocatedPaise = allocatedPaise, isRolloverEnabled = isRollover)
                )
            } else {
                val existing = budgetRepo.getBudgetById(id)
                if (existing != null) {
                    budgetRepo.updateBudget(
                        existing.copy(name = name, allocatedPaise = allocatedPaise, isRolloverEnabled = isRollover)
                    )
                }
            }
        }
    }

    fun assignCategoryToBudget(categoryId: String, budgetId: String?) {
        viewModelScope.launch {
            categoryRepo.assignCategoryToBudget(categoryId, budgetId)
        }
    }

    fun archiveBudget(budgetId: String) {
        viewModelScope.launch {
            budgetRepo.archiveBudget(budgetId)
        }
    }

    private fun buildCategoryBudgets(
        categories: List<Category>,
        spends: List<CategorySpend>
    ): List<CategoryBudgetInfo> {
        val spendMap = spends.associate { it.categoryId to it.total }
        return categories
            .filter { !it.isIncome }
            .map { cat ->
                CategoryBudgetInfo(
                    category = cat,
                    spentPaise = spendMap[cat.id] ?: 0L,
                    budgetPaise = cat.budgetPaise
                )
            }
            .filter { it.spentPaise > 0 || it.budgetPaise > 0 }
            .sortedByDescending { it.spentPaise }
    }

    fun setMonth(yearMonth: String) {
        _currentMonth.value = yearMonth
        loadDashboard()
    }
}
