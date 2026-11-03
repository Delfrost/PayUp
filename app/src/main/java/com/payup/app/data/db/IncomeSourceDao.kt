package com.payup.app.data.db

import androidx.room.*
import com.payup.app.data.model.IncomeSource
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: IncomeSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<IncomeSource>)

    @Update
    suspend fun update(source: IncomeSource)

    @Delete
    suspend fun delete(source: IncomeSource)

    @Query("SELECT * FROM income_sources ORDER BY name ASC")
    fun getAll(): Flow<List<IncomeSource>>

    @Query("SELECT COALESCE(SUM(expected_paise), 0) FROM income_sources")
    fun getTotalExpectedPaise(): Flow<Long>

    @Query("SELECT COUNT(*) FROM income_sources")
    suspend fun count(): Int
}
