package com.payup.app.data.db

import androidx.room.*
import com.payup.app.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("SELECT * FROM accounts WHERE is_active = 1 GROUP BY name ORDER BY CASE id WHEN 'acc_upi' THEN 1 WHEN 'acc_cash' THEN 2 WHEN 'acc_bank' THEN 3 WHEN 'acc_card' THEN 4 ELSE 5 END, name ASC")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): Account?

    @Query("SELECT COALESCE(SUM(balance_paise), 0) FROM accounts WHERE is_active = 1")
    fun totalBalance(): Flow<Long>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}
