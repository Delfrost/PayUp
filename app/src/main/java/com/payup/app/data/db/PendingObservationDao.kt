package com.payup.app.data.db

import androidx.room.*
import com.payup.app.data.model.PendingObservation
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingObservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(observation: PendingObservation)

    @Update
    suspend fun update(observation: PendingObservation)

    @Delete
    suspend fun delete(observation: PendingObservation)

    @Query("SELECT * FROM pending_observations WHERE status = 'PENDING_REVIEW' ORDER BY timestamp DESC")
    fun getAllPending(): Flow<List<PendingObservation>>

    @Query("SELECT * FROM pending_observations WHERE reference_id = :ref AND status = 'PENDING_REVIEW' LIMIT 1")
    suspend fun findPendingByRef(ref: String): PendingObservation?

    @Query("SELECT COUNT(*) FROM pending_observations WHERE status = 'PENDING_REVIEW'")
    fun countPending(): Flow<Int>
}
