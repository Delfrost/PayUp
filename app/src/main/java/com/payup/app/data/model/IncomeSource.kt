package com.payup.app.data.model

import androidx.room.*
import java.util.UUID

@Entity(tableName = "income_sources")
data class IncomeSource(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "expected_paise")
    val expectedPaise: Long = 0L,

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = true
)
