package com.payup.app.data.model

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "merchant_rules",
    indices = [Index("pattern", unique = true)]
)
data class MerchantRule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "pattern")
    val pattern: String, // merchant name pattern to match

    @ColumnInfo(name = "category_id")
    val categoryId: String,

    @ColumnInfo(name = "priority")
    val priority: Int = 0
)
