package com.payup.app.repository

import com.payup.app.data.db.AccountDao
import com.payup.app.data.model.Account
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val dao: AccountDao) {

    fun getAll(): Flow<List<Account>> = dao.getAll()

    suspend fun getById(id: String): Account? = dao.getById(id)

    suspend fun insert(account: Account) = dao.insert(account)

    suspend fun update(account: Account) = dao.update(account)

    suspend fun delete(account: Account) = dao.delete(account)

    fun totalBalance(): Flow<Long> = dao.totalBalance()

    suspend fun count(): Int = dao.count()
}
