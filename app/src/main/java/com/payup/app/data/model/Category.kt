package com.payup.app.data.model

import androidx.room.*
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon")
    val icon: String = "category", // Material icon name

    @ColumnInfo(name = "color")
    val color: Long = 0xFF6200EE,

    @ColumnInfo(name = "budget_paise")
    val budgetPaise: Long = 0L,

    @ColumnInfo(name = "budget_id")
    val budgetId: String? = null,

    @ColumnInfo(name = "is_income")
    val isIncome: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false
)
