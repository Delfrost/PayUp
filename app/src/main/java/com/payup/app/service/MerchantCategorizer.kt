package com.payup.app.service

import com.payup.app.data.db.MerchantRuleDao
import com.payup.app.data.model.MerchantRule

class MerchantCategorizer(private val merchantRuleDao: MerchantRuleDao) {

    suspend fun categorize(merchant: String): String? {
        if (merchant.isBlank()) return null
        val rules = merchantRuleDao.getAllSync()
        return rules
            .sortedByDescending { it.priority }
            .firstOrNull { merchant.contains(it.pattern, ignoreCase = true) }
            ?.categoryId
    }

    suspend fun addRule(pattern: String, categoryId: String, priority: Int = 0) {
        merchantRuleDao.insert(
            MerchantRule(pattern = pattern, categoryId = categoryId, priority = priority)
        )
    }
}
