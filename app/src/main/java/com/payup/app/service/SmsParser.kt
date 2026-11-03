package com.payup.app.service

import com.payup.app.data.model.TransactionStatus
import com.payup.app.data.model.TransactionType
import com.payup.app.util.CurrencyUtils
import java.time.Instant
import java.util.regex.Pattern

object SmsParser {

    data class ParsedSms(
        val amountPaise: Long,
        val merchant: String,
        val type: TransactionType,
        val status: TransactionStatus,
        val referenceId: String?,
        val bankName: String,
        val timestamp: Instant = Instant.now()
    )

    private val amountPattern = Pattern.compile(
        """(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    private val refPattern = Pattern.compile(
        """(?:UPI(?:/DR|/CR)?/|Ref(?:[:\s]|No[:\s]*)?|UTR[:\s]*|Txn[:\s]*)(\d{10,})""",
        Pattern.CASE_INSENSITIVE
    )

    // Match merchant after "to", "towards", "info:", "vpa"
    private val merchantPattern = Pattern.compile(
        """(?:to\s+VPA|to|towards|info:\s*UPI\s*\*)\s+([A-Za-z0-9\s&@._-]+?)(?:\s+(?:on|ref|upi|avl|bal|clear|through|\.|$))""",
        Pattern.CASE_INSENSITIVE
    )

    private val promoOrOtpKeywords = Pattern.compile(
        """\b(?:OTP|verification code|one time password|secret code|do not share|apply now|pre-approved|loan|credit limit|claim your|congratulations|discount|sale|promo code|coupon code|discount code)\b""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(sender: String?, body: String?): ParsedSms? {
        if (body.isNullOrBlank()) return null

        // Ignore OTP, authentication, and marketing SMS
        if (promoOrOtpKeywords.matcher(body).find()) return null

        val hasExplicitCredit = body.contains("credited", ignoreCase = true) ||
                body.contains("deposited", ignoreCase = true) ||
                body.contains("refund", ignoreCase = true)

        val hasExplicitDebit = body.contains("debited", ignoreCase = true) ||
                body.contains("withdrawn", ignoreCase = true)

        val isDebit = hasExplicitDebit ||
                body.contains("paid", ignoreCase = true) ||
                (body.contains("sent", ignoreCase = true) && !hasExplicitCredit)

        val isCredit = hasExplicitCredit ||
                (body.contains("received", ignoreCase = true) && !hasExplicitDebit)

        if (!isDebit && !isCredit) return null

        val amountMatcher = amountPattern.matcher(body)
        if (!amountMatcher.find()) return null

        val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val amountPaise = CurrencyUtils.parseToPaise(amountStr)

        if (amountPaise <= 0L) return null

        // If an SMS contains "credited" (e.g. "credited ... sent by Rahul"), it's income unless explicitly debited
        val type = if (isCredit && !hasExplicitDebit) TransactionType.INCOME else TransactionType.EXPENSE

        // Extract Ref ID / UTR
        val refMatcher = refPattern.matcher(body)
        val referenceId = if (refMatcher.find()) refMatcher.group(1) else null

        // Extract Merchant
        val mMatcher = merchantPattern.matcher(body)
        var merchant = if (mMatcher.find()) {
            mMatcher.group(1)?.trim() ?: ""
        } else ""

        merchant = merchant.replace(Regex("""(?i)\s*(?:on|ref|upi|avl|bal).*$"""), "").trim()
        if (merchant.isBlank()) {
            merchant = if (type == TransactionType.EXPENSE) "Bank Payment" else "Bank Deposit"
        }

        // Identify Bank Name from sender (e.g. "VK-HDFCBK" -> "HDFC Bank", "AD-SBIINB" -> "SBI")
        val bankName = extractBankName(sender ?: "")

        return ParsedSms(
            amountPaise = amountPaise,
            merchant = merchant,
            type = type,
            status = TransactionStatus.COMPLETED,
            referenceId = referenceId,
            bankName = bankName
        )
    }

    private fun extractBankName(sender: String): String {
        val s = sender.uppercase()
        return when {
            s.contains("HDFC") -> "HDFC Bank"
            s.contains("SBI") -> "State Bank of India"
            s.contains("ICICI") -> "ICICI Bank"
            s.contains("AXIS") -> "Axis Bank"
            s.contains("KOTAK") -> "Kotak Bank"
            s.contains("PNB") -> "Punjab National Bank"
            s.contains("BOB") || s.contains("BARODA") -> "Bank of Baroda"
            s.contains("IDFC") -> "IDFC FIRST Bank"
            s.contains("INDUS") -> "IndusInd Bank"
            s.contains("CANARA") -> "Canara Bank"
            s.contains("YES") -> "Yes Bank"
            else -> "Bank SMS"
        }
    }
}
