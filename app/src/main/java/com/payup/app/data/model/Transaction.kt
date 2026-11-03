package com.payup.app.data.model

import androidx.room.*
import java.time.Instant
import java.util.UUID

enum class TransactionType { INCOME, EXPENSE, TRANSFER }
enum class TransactionStatus { COMPLETED, PENDING, FAILED, REFUNDED }
enum class DetectionMethod { MANUAL, NOTIFICATION, SMS, SCREENSHOT_OCR, CSV_IMPORT }

@Entity(
    tableName = "transactions",
    indices = [
        Index("category_id"),
        Index("budget_id"),
        Index("account_id"),
        Index("timestamp"),
        Index("merchant"),
        Index("dedup_hash", unique = true)
    ]
)
data class Transaction(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long, // Always positive, direction from type

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "status")
    val status: TransactionStatus = TransactionStatus.COMPLETED,

    @ColumnInfo(name = "merchant")
    val merchant: String = "",

    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,

    @ColumnInfo(name = "budget_id")
    val budgetId: String? = null,

    @ColumnInfo(name = "account_id")
    val accountId: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Instant = Instant.now(),

    @ColumnInfo(name = "source_app")
    val sourceApp: String? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "detection_method")
    val detectionMethod: DetectionMethod = DetectionMethod.MANUAL,

    @ColumnInfo(name = "dedup_hash")
    val dedupHash: String? = null,

    @ColumnInfo(name = "transfer_linked_id")
    val transferLinkedId: String? = null,

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "recurring_rule_id")
    val recurringRuleId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)
