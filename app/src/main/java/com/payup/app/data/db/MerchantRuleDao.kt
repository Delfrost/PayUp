package com.payup.app.data.db

import androidx.room.*
import com.payup.app.data.model.MerchantRule
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: MerchantRule)

    @Update
    suspend fun update(rule: MerchantRule)

    @Delete
    suspend fun delete(rule: MerchantRule)

    @Query("SELECT * FROM merchant_rules ORDER BY priority DESC")
    fun getAll(): Flow<List<MerchantRule>>

    @Query("SELECT * FROM merchant_rules ORDER BY priority DESC")
    suspend fun getAllSync(): List<MerchantRule>
}
