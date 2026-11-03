package com.payup.app.data.model

import androidx.room.*
import java.time.Instant
import java.util.UUID

enum class ObservationStatus { PENDING_REVIEW, RESOLVED, DISMISSED }

@Entity(tableName = "pending_observations")
data class PendingObservation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long? = null,

    @ColumnInfo(name = "merchant")
    val merchant: String? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "source")
    val source: String = "NOTIFICATION", // NOTIFICATION, SMS, SCREENSHOT

    @ColumnInfo(name = "raw_content")
    val rawContent: String = "",

    @ColumnInfo(name = "status")
    val status: ObservationStatus = ObservationStatus.PENDING_REVIEW,

    @ColumnInfo(name = "timestamp")
    val timestamp: Instant = Instant.now()
)
