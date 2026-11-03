package com.payup.app.service

import com.payup.app.data.model.*
import com.payup.app.util.CurrencyUtils
import java.time.Instant
import java.util.regex.Pattern

/**
 * Parses UPI payment notifications from Google Pay, super.money, Paytm, PhonePe, BHIM, etc.
 * Strictly ignores marketing/promotional notifications (e.g. cashback offers, flash sales, discounts).
 * Returns null if the notification cannot be definitively verified as a real financial transaction.
 */
object NotificationParser {

    data class ParsedTransaction(
        val amountPaise: Long,
        val merchant: String,
        val type: TransactionType,
        val status: TransactionStatus,
        val referenceId: String?,
        val sourceApp: String,
        val timestamp: Instant = Instant.now()
    )

    // Amount pattern: captures ₹1,000 or Rs. 150.50 or INR 50
    private val amountPattern = Pattern.compile("""(?:Rs\.?|₹|INR)\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE)
    
    // UTR / UPI reference pattern (12+ digits)
    private val upiRefPattern = Pattern.compile("""(?:UPI\s*(?:Ref|ID|Transaction\s*ID)?[:\s]*|UTR[:\s]*|Ref(?:[:\s]|No[:\s]*)?)(\d{10,})""", Pattern.CASE_INSENSITIVE)

    // Merchant patterns
    private val paidToPattern = Pattern.compile("""(?:paid to|sent to|transfer(?:red)? to|paid at|payment to)\s+([^.,;\n\r]+?)(?:\s+(?:on|via|through|using|UPI|Rs|₹)\b|\.|\r|\n|$)""", Pattern.CASE_INSENSITIVE)
    private val receivedFromPattern = Pattern.compile("""(?:received from|received .* from|credited from|from)\s+([^.,;\n\r]+?)(?:\s+(?:on|via|through|using|UPI|Rs|₹)\b|\.|\r|\n|$)""", Pattern.CASE_INSENSITIVE)

    // Strict promotional / marketing keywords - ALWAYS REJECT
    private val promoKeywordsPattern = Pattern.compile(
        """\b(?:flash sale|sale ending|shop now|order now|buy now|recharge now|flat\s+\d+|upto|up to\s+(?:Rs|₹)?|get\s+(?:Rs|₹)?\s*\d+\s*(?:cashback|off)|win\s+(?:Rs|₹)?|earn\s+(?:Rs|₹)?|stand a chance|cashback on|discount on|offer on|coupon|voucher|scratch card|spin to win|deal of the day|limited period|apply code|use code|hurry|exclusive deal)\b""",
        Pattern.CASE_INSENSITIVE
    )

    // Confirmed transaction indicators
    private val debitIndicators = listOf(
        "paid", "debited", "sent", "transferred to", "spent", "withdrawn", "debited by", "payment of"
    )

    private val creditIndicators = listOf(
        "received", "credited", "deposited", "refund", "refunded"
    )

    // Known UPI app packages
    private val knownApps = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "money.super.payments" to "super.money",
        "net.one97.paytm" to "Paytm",
        "com.phonepe.app" to "PhonePe",
        "in.org.npci.upiapp" to "BHIM",
        "com.whatsapp" to "WhatsApp Pay",
        "in.amazon.mShop.android.shopping" to "Amazon Pay"
    )

    sealed class ParseResult {
        data class Confirmed(val transaction: ParsedTransaction) : ParseResult()
        data class Incomplete(val observation: PendingObservation) : ParseResult()
    }

    fun parseWithObservation(packageName: String, title: String?, text: String?): ParseResult? {
        val content = buildString {
            if (title != null) append(title).append(" ")
            if (text != null) append(text)
        }.trim()

        if (content.isBlank()) return null

        // 1. REJECT PROMOTIONAL / MARKETING NOTIFICATIONS
        if (promoKeywordsPattern.matcher(content).find()) {
            return null
        }

        if (content.contains("cashback", ignoreCase = true) &&
            !content.contains("credited", ignoreCase = true) &&
            !content.contains("received", ignoreCase = true) &&
            !content.contains("added", ignoreCase = true)
        ) {
            return null
        }

        // 2. CHECK STATUS (Never log failed/cancelled transactions)
        val isFailed = content.contains("failed", ignoreCase = true) ||
                content.contains("declined", ignoreCase = true) ||
                content.contains("unsuccessful", ignoreCase = true) ||
                content.contains("cancelled", ignoreCase = true) ||
                content.contains("rejected", ignoreCase = true)

        if (isFailed) return null

        // 3. DETECT TRANSACTION INTENT
        val hasDebit = debitIndicators.any { content.contains(it, ignoreCase = true) }
        val hasCredit = creditIndicators.any { content.contains(it, ignoreCase = true) }
        val hasUpiRef = upiRefPattern.matcher(content).find()

        if (!hasDebit && !hasCredit && !hasUpiRef) {
            return null
        }

        // 4. EXTRACT REFERENCE ID & MERCHANT
        val refMatcher = upiRefPattern.matcher(content)
        val referenceId = if (refMatcher.find()) refMatcher.group(1) else null
        val sourceApp = knownApps[packageName] ?: packageName

        val isRealCashback = content.contains("cashback", ignoreCase = true) &&
                (content.contains("credited", ignoreCase = true) || content.contains("received", ignoreCase = true))

        val type = when {
            isRealCashback -> TransactionType.INCOME
            hasCredit && !hasDebit -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        val merchant = when {
            isRealCashback -> "$sourceApp Cashback"
            type == TransactionType.INCOME -> {
                val m = receivedFromPattern.matcher(content)
                if (m.find()) m.group(1)?.trim() ?: "" else ""
            }
            else -> {
                val m = paidToPattern.matcher(content)
                if (m.find()) m.group(1)?.trim() ?: "" else ""
            }
        }.cleanMerchant().ifBlank { "UPI Transaction" }

        // 5. EXTRACT AMOUNT
        val amountMatcher = amountPattern.matcher(content)
        if (!amountMatcher.find()) {
            // Amount is missing: Create a non-posted PendingObservation, NEVER a ₹0 expense
            return ParseResult.Incomplete(
                PendingObservation(
                    amountPaise = null,
                    merchant = merchant,
                    referenceId = referenceId,
                    source = "NOTIFICATION ($sourceApp)",
                    rawContent = content,
                    status = ObservationStatus.PENDING_REVIEW
                )
            )
        }

        val amountStr = amountMatcher.group(1)?.replace(",", "")
        val amountPaise = CurrencyUtils.parseToPaise(amountStr ?: "")

        if (amountPaise <= 0L) {
            return ParseResult.Incomplete(
                PendingObservation(
                    amountPaise = null,
                    merchant = merchant,
                    referenceId = referenceId,
                    source = "NOTIFICATION ($sourceApp)",
                    rawContent = content,
                    status = ObservationStatus.PENDING_REVIEW
                )
            )
        }

        val isPending = content.contains("pending", ignoreCase = true) ||
                content.contains("processing", ignoreCase = true)
        val status = if (isPending) TransactionStatus.PENDING else TransactionStatus.COMPLETED

        return ParseResult.Confirmed(
            ParsedTransaction(
                amountPaise = amountPaise,
                merchant = merchant,
                type = type,
                status = status,
                referenceId = referenceId,
                sourceApp = sourceApp
            )
        )
    }

    fun parse(packageName: String, title: String?, text: String?): ParsedTransaction? {
        val result = parseWithObservation(packageName, title, text)
        return if (result is ParseResult.Confirmed) result.transaction else null
    }

    private fun String.cleanMerchant(): String {
        return this.replace(Regex("""(?i)\s*(?:on|via|through|using)\s+.*$"""), "")
            .replace(Regex("""(?i)\s*UPI$"""), "")
            .trim()
    }
}
