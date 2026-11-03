package com.payup.app.repository

import com.payup.app.data.db.CategoryDao
import com.payup.app.data.model.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    fun getAll(): Flow<List<Category>> = dao.getAll()

    fun getExpenseCategories(): Flow<List<Category>> = dao.getExpenseCategories()

    fun getIncomeCategories(): Flow<List<Category>> = dao.getIncomeCategories()

    fun getByBudgetId(budgetId: String): Flow<List<Category>> = dao.getByBudgetId(budgetId)

    suspend fun assignCategoryToBudget(categoryId: String, budgetId: String?) =
        dao.assignCategoryToBudget(categoryId, budgetId)

    suspend fun getById(id: String): Category? = dao.getById(id)

    suspend fun insert(category: Category) = dao.insert(category)

    suspend fun update(category: Category) = dao.update(category)

    suspend fun delete(category: Category) = dao.delete(category)

    suspend fun count(): Int = dao.count()
}
