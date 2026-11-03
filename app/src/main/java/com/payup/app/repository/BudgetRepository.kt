package com.payup.app.repository

import com.payup.app.data.db.BudgetDao
import com.payup.app.data.db.IncomeSourceDao
import com.payup.app.data.model.Budget
import com.payup.app.data.model.IncomeSource
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val incomeSourceDao: IncomeSourceDao
) {
    // Custom Budgets (FEAT-03)
    fun getAllActiveBudgets(): Flow<List<Budget>> = budgetDao.getAllActive()

    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAll()

    suspend fun getBudgetById(id: String): Budget? = budgetDao.getById(id)

    suspend fun insertBudget(budget: Budget) = budgetDao.insert(budget)

    suspend fun updateBudget(budget: Budget) = budgetDao.update(budget)

    suspend fun archiveBudget(id: String) = budgetDao.archive(id)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    // Income Sources & Allocations (FEAT-02)
    fun getAllIncomeSources(): Flow<List<IncomeSource>> = incomeSourceDao.getAll()

    fun getTotalExpectedIncome(): Flow<Long> = incomeSourceDao.getTotalExpectedPaise()

    suspend fun insertIncomeSource(source: IncomeSource) = incomeSourceDao.insert(source)

    suspend fun updateIncomeSource(source: IncomeSource) = incomeSourceDao.update(source)

    suspend fun deleteIncomeSource(source: IncomeSource) = incomeSourceDao.delete(source)
}
