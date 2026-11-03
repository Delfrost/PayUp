package com.payup.app.data.db

import androidx.room.*
import com.payup.app.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>)

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE is_archived = 0 ORDER BY order_index ASC, name ASC")
    fun getAllActive(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets ORDER BY order_index ASC, name ASC")
    fun getAll(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): Budget?

    @Query("SELECT COUNT(*) FROM budgets WHERE is_archived = 0")
    suspend fun count(): Int

    @Query("UPDATE budgets SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: String)
}
