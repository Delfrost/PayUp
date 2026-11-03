package com.payup.app.data.model

import androidx.room.*
import java.time.Instant
import java.util.UUID

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "allocated_paise")
    val allocatedPaise: Long = 0L,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,

    @ColumnInfo(name = "color")
    val color: Long = 0xFF4CAF50,

    @ColumnInfo(name = "icon")
    val icon: String = "account_balance_wallet",

    @ColumnInfo(name = "is_rollover_enabled")
    val isRolloverEnabled: Boolean = false,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now()
)
