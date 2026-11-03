package com.payup.app.service

import com.payup.app.data.model.TransactionStatus
import com.payup.app.data.model.TransactionType
import com.payup.app.util.CurrencyUtils
import java.time.Instant
import java.util.UUID
import java.util.regex.Pattern

data class ScreenshotCandidate(
    val id: String = UUID.randomUUID().toString(),
    var amountPaise: Long,
    var merchant: String,
    var type: TransactionType = TransactionType.EXPENSE,
    var status: TransactionStatus = TransactionStatus.COMPLETED,
    var referenceId: String? = null,
    var timestamp: Instant = Instant.now(),
    var isLikelyDuplicate: Boolean = false,
    var isExcluded: Boolean = false,
    var categoryId: String? = null,
    var accountId: String? = "acc_upi"
)

object ScreenshotParser {

    private val amountPattern = Pattern.compile(
        """(?:Rs\.?|₹|INR)?\s*([\d,]+\.\d{2}|[\d,]+)\s*(?:debited|paid|credited)?""",
        Pattern.CASE_INSENSITIVE
    )

    private val strictAmountRegex = Regex("""(?:Rs\.?|₹|INR)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val refPattern = Pattern.compile("""(?:UPI\s*(?:transaction\s*id|ref|id)?|UTR|Txn\s*ID|Ref(?:\s*No)?|Reference)[\s:]*([A-Za-z0-9]{10,})""", Pattern.CASE_INSENSITIVE)

    fun parseText(fullText: String): List<ScreenshotCandidate> {
        val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val candidates = mutableListOf<ScreenshotCandidate>()
        val processedLineIndices = mutableSetOf<Int>()

        for (i in lines.indices) {
            val line = lines[i]

            // Look for amount pattern
            val match = strictAmountRegex.find(line)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                val amountPaise = CurrencyUtils.parseToPaise(amountStr)

                if (amountPaise <= 0L) continue

                // Context search around the amount line
                val contextLines = lines.subList(maxOf(0, i - 3), minOf(lines.size, i + 4))
                val contextText = contextLines.joinToString(" ")

                // Check for failure
                val isFailed = contextText.contains("failed", ignoreCase = true) ||
                        contextText.contains("declined", ignoreCase = true) ||
                        contextText.contains("cancelled", ignoreCase = true)

                // Detect direction
                val isCredit = contextText.contains("received", ignoreCase = true) ||
                        contextText.contains("credited", ignoreCase = true) ||
                        contextText.contains("refund", ignoreCase = true)

                val type = if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE
                val status = if (isFailed) TransactionStatus.FAILED else TransactionStatus.COMPLETED

                // Extract Reference ID
                val refMatcher = refPattern.matcher(contextText)
                val referenceId = if (refMatcher.find()) refMatcher.group(1) else null

                // Extract Merchant: check line before amount, or "paid to", "to"
                var merchant = ""
                for (j in (i - 1) downTo maxOf(0, i - 2)) {
                    val prev = lines[j]
                    if (!prev.contains("₹") && !prev.contains("Rs") && prev.length in 3..40 &&
                        !prev.contains("payment", ignoreCase = true) &&
                        !prev.contains("successful", ignoreCase = true)
                    ) {
                        merchant = prev.replace(Regex("""(?i)^(?:paid to|transfer to|to)\s+"""), "").trim()
                        break
                    }
                }

                if (merchant.isBlank()) {
                    // Check line after amount
                    if (i + 1 < lines.size) {
                        val next = lines[i + 1]
                        if (!next.contains("₹") && !next.contains("Rs") && next.length in 3..40) {
                            merchant = next.replace(Regex("""(?i)^(?:paid to|transfer to|to)\s+"""), "").trim()
                        }
                    }
                }

                if (merchant.isBlank()) {
                    merchant = if (type == TransactionType.INCOME) "UPI Credit" else "UPI Payment"
                }

                candidates.add(
                    ScreenshotCandidate(
                        amountPaise = amountPaise,
                        merchant = merchant,
                        type = type,
                        status = status,
                        referenceId = referenceId,
                        isExcluded = isFailed // Exclude failed by default
                    )
                )
                processedLineIndices.add(i)
            }
        }

        return candidates
    }
}
