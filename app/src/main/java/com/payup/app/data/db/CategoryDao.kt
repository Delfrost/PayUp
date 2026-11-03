package com.payup.app.data.db

import androidx.room.*
import com.payup.app.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    suspend fun getAllList(): List<Category>

    @Query("SELECT * FROM categories WHERE budget_id = :budgetId ORDER BY sort_order ASC")
    fun getByBudgetId(budgetId: String): Flow<List<Category>>

    @Query("UPDATE categories SET budget_id = :budgetId WHERE id = :categoryId")
    suspend fun assignCategoryToBudget(categoryId: String, budgetId: String?)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): Category?

    @Query("SELECT * FROM categories WHERE is_income = 0 ORDER BY sort_order ASC")
    fun getExpenseCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE is_income = 1 ORDER BY sort_order ASC")
    fun getIncomeCategories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
