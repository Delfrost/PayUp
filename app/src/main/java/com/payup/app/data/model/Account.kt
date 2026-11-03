package com.payup.app.data.model

import androidx.room.*
import java.util.UUID

enum class AccountType { BANK, WALLET, CASH, CREDIT_CARD }

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: AccountType = AccountType.BANK,

    @ColumnInfo(name = "balance_paise")
    val balancePaise: Long = 0L,

    @ColumnInfo(name = "icon")
    val icon: String = "account_balance",

    @ColumnInfo(name = "color")
    val color: Long = 0xFF1976D2,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
