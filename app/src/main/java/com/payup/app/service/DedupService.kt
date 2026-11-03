package com.payup.app.service

import java.security.MessageDigest

object DedupService {
    /**
     * Generate a dedup hash from transaction attributes.
     * When referenceId is present (UPI reference number or UTR), it provides the authoritative canonical identity.
     * When referenceId is null, uses amount + merchant + rolling timestamp window.
     */
    fun generateHash(
        amountPaise: Long,
        merchant: String,
        timestampMillis: Long,
        referenceId: String? = null
    ): String {
        val input = buildString {
            if (!referenceId.isNullOrBlank()) {
                append("REF|")
                append(referenceId.trim().uppercase())
            } else {
                append("META|")
                append(amountPaise)
                append("|")
                append(merchant.lowercase().trim())
                append("|")
                // 5-minute bucket to avoid splitting across minute boundaries
                append(timestampMillis / 300000)
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
