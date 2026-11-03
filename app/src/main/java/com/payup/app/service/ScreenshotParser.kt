package com.payup.app.service

import com.payup.app.data.model.TransactionStatus
import com.payup.app.data.model.TransactionType
import com.payup.app.util.CurrencyUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
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
    var sourceApp: String = "Google Pay",
    var rawDateText: String? = null,
    var isLikelyDuplicate: Boolean = false,
    var isExcluded: Boolean = false,
    var categoryId: String? = null,
    var accountId: String? = "acc_upi",
    var confidence: String = "High"
)

object ScreenshotParser {

    // Regex for amounts with optional currency symbol and optional sign (+, -)
    // Matches: ₹1,450.00, ₹350, +₹1,200, - ₹1,499, Rs. 240, INR 500, + ₹25
    private val amountRegex = Regex(
        """([+-]?)\s*(?:Rs\.?|₹|INR)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Secondary bare amount on its own line: e.g. "1,450.00" or "350"
    private val standaloneAmountRegex = Regex(
        """^([+-]?)\s*([\d,]{1,8}(?:\.\d{2})?)$"""
    )

    // UPI Reference / UTR patterns (12-digit standard UTR or alphanumeric UPI/Google transaction IDs)
    private val utrPattern = Pattern.compile(
        """(?:UPI\s*(?:ref(?:erence)?(?:\s*(?:no|id))?|transaction\s*id|id)|UTR|Txn\s*ID|Ref(?:\s*No)?|Reference)[\s:]*([A-Za-z0-9_-]{10,24})""",
        Pattern.CASE_INSENSITIVE
    )

    private val pure12DigitUtr = Pattern.compile("""\b(\d{12})\b""")

    fun parseText(fullText: String): List<ScreenshotCandidate> {
        val lines = fullText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // 1. Detect Source Application
        val detectedApp = detectSourceApp(fullText)

        // 2. Determine if Single Receipt vs Multiple History Statement
        val isHistory = isTransactionHistoryLayout(fullText, lines)

        return if (isHistory) {
            parseHistoryStatement(lines, detectedApp)
        } else {
            parseSingleReceipt(lines, fullText, detectedApp)
        }
    }

    private fun detectSourceApp(fullText: String): String {
        val lower = fullText.lowercase(Locale.ROOT)
        return when {
            lower.contains("super.money") || lower.contains("supermoney") ||
                    lower.contains("supercashback") || lower.contains("supercard") ||
                    lower.contains("superpay") || lower.contains("superwallet") -> "super.money"
            lower.contains("google pay") || lower.contains("gpay") ||
                    lower.contains("google transaction id") || lower.contains("cicag") ||
                    lower.contains("transaction history") -> "Google Pay"
            lower.contains("phonepe") -> "PhonePe"
            lower.contains("paytm") -> "Paytm"
            lower.contains("bhim") -> "BHIM"
            else -> "UPI App"
        }
    }

    private fun isTransactionHistoryLayout(fullText: String, lines: List<String>): Boolean {
        val lower = fullText.lowercase(Locale.ROOT)
        val historyKeywords = listOf("transaction history", "transactions", "past payments", "all transactions")
        if (historyKeywords.any { lower.contains(it) }) return true

        // Count how many amount lines exist in the screenshot
        var amountCount = 0
        for (line in lines) {
            if (amountRegex.containsMatchIn(line)) {
                // Exclude obvious reward / cashback lines
                if (!isRewardOrCashbackLine(line)) {
                    amountCount++
                }
            }
        }
        return amountCount > 1
    }

    private fun isRewardOrCashbackLine(line: String): Boolean {
        val lower = line.lowercase(Locale.ROOT)
        return lower.contains("cashback") ||
                lower.contains("reward") ||
                lower.contains("won") ||
                lower.contains("save") ||
                lower.contains("coupon")
    }

    // --- Single Receipt Parsing ---
    private fun parseSingleReceipt(lines: List<String>, fullText: String, sourceApp: String): List<ScreenshotCandidate> {
        // Find the primary transaction amount (filter out secondary cashbacks / discount lines)
        var primaryAmountPaise = 0L
        var amountLineIndex = -1
        var signStr = ""

        for (i in lines.indices) {
            val line = lines[i]
            if (isRewardOrCashbackLine(line)) continue

            val match = amountRegex.find(line)
            if (match != null) {
                val sign = match.groupValues[1].trim()
                val rawVal = match.groupValues[2].replace(",", "")
                val paise = CurrencyUtils.parseToPaise(rawVal)
                if (paise > 0L) {
                    primaryAmountPaise = paise
                    signStr = sign
                    amountLineIndex = i
                    break
                }
            }
        }

        // If no primary amount found with symbol, try standalone line
        if (primaryAmountPaise <= 0L) {
            for (i in lines.indices) {
                val line = lines[i]
                if (isRewardOrCashbackLine(line)) continue
                val match = standaloneAmountRegex.find(line)
                if (match != null) {
                    val sign = match.groupValues[1].trim()
                    val rawVal = match.groupValues[2].replace(",", "")
                    val paise = CurrencyUtils.parseToPaise(rawVal)
                    if (paise > 0L) {
                        primaryAmountPaise = paise
                        signStr = sign
                        amountLineIndex = i
                        break
                    }
                }
            }
        }

        if (primaryAmountPaise <= 0L) return emptyList()

        // Status Detection
        val lowerAll = fullText.lowercase(Locale.ROOT)
        val status = when {
            lowerAll.contains("failed") || lowerAll.contains("declined") || lowerAll.contains("cancelled") ||
                    lowerAll.contains("couldn't be completed") || lowerAll.contains("server busy") -> TransactionStatus.FAILED
            lowerAll.contains("pending") || lowerAll.contains("processing") || lowerAll.contains("in progress") -> TransactionStatus.PENDING
            else -> TransactionStatus.COMPLETED
        }

        // Direction Detection
        val isCredit = signStr == "+" ||
                lowerAll.contains("received from") ||
                lowerAll.contains("credited to") ||
                lowerAll.contains("refund from") ||
                lowerAll.contains("received") && !lowerAll.contains("paid")
        val type = if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE

        // Reference ID / UTR extraction
        var refId: String? = null
        val utrMatcher = utrPattern.matcher(fullText)
        if (utrMatcher.find()) {
            refId = utrMatcher.group(1)?.trim()
        }
        if (refId == null) {
            // Check for pure 12 digit UTR in text
            val p12 = pure12DigitUtr.matcher(fullText)
            if (p12.find()) {
                refId = p12.group(1)?.trim()
            }
        }

        // Merchant extraction
        var merchant = extractMerchantSingleReceipt(lines, amountLineIndex, type)
        if (merchant.isBlank()) {
            merchant = if (type == TransactionType.INCOME) "UPI Credit" else "UPI Payment"
        }

        // Date and Time parsing
        val (parsedInstant, rawDate) = extractDateTime(fullText, lines)

        return listOf(
            ScreenshotCandidate(
                amountPaise = primaryAmountPaise,
                merchant = merchant,
                type = type,
                status = status,
                referenceId = refId,
                timestamp = parsedInstant,
                sourceApp = sourceApp,
                rawDateText = rawDate,
                isExcluded = (status == TransactionStatus.FAILED),
                confidence = if (merchant != "UPI Credit" && merchant != "UPI Payment") "High" else "Review"
            )
        )
    }

    private fun extractMerchantSingleReceipt(lines: List<String>, amountLineIndex: Int, type: TransactionType): String {
        // Priority 1: Check lines explicitly labeled "Paid to", "To:", "Received from"
        for (line in lines) {
            val lower = line.lowercase(Locale.ROOT)
            val matchedPrefix = when {
                lower.startsWith("paid to ") -> "paid to "
                lower.startsWith("paid to\n") -> "paid to"
                lower.startsWith("sent to ") -> "sent to "
                lower.startsWith("transfer to ") -> "transfer to "
                lower.startsWith("to: ") || lower.startsWith("to:") -> "to:"
                lower.startsWith("received from ") -> "received from "
                lower.startsWith("from: ") || lower.startsWith("from:") -> "from:"
                else -> null
            }
            if (matchedPrefix != null) {
                val cleaned = line.substring(matchedPrefix.length).trim()
                val candidate = cleanMerchantString(cleaned)
                if (candidate.isNotBlank() && isValidMerchantName(candidate)) {
                    return candidate
                }
            }
        }

        // Priority 2: Look in lines before the amount
        if (amountLineIndex > 0) {
            for (idx in (amountLineIndex - 1) downTo 0) {
                val line = lines[idx]
                if (isIgnoredHeaderLine(line)) continue
                val cleaned = cleanMerchantString(line)
                if (isValidMerchantName(cleaned)) {
                    return cleaned
                }
            }
        }

        // Priority 3: Look in lines after the amount
        if (amountLineIndex >= 0 && amountLineIndex + 1 < lines.size) {
            for (idx in (amountLineIndex + 1) until minOf(lines.size, amountLineIndex + 4)) {
                val line = lines[idx]
                if (isIgnoredHeaderLine(line)) continue
                val cleaned = cleanMerchantString(line)
                if (isValidMerchantName(cleaned)) {
                    return cleaned
                }
            }
        }

        return ""
    }

    // --- Multi-Row History Statement Parsing ---
    private fun parseHistoryStatement(lines: List<String>, sourceApp: String): List<ScreenshotCandidate> {
        val candidates = mutableListOf<ScreenshotCandidate>()

        // Find all indices where amounts exist
        val amountIndices = mutableListOf<Pair<Int, MatchResult>>()
        for (i in lines.indices) {
            val line = lines[i]
            if (isRewardOrCashbackLine(line)) continue
            val match = amountRegex.find(line)
            if (match != null) {
                amountIndices.add(Pair(i, match))
            }
        }

        if (amountIndices.isEmpty()) return emptyList()

        for (idx in amountIndices.indices) {
            val (lineIdx, match) = amountIndices[idx]
            val signStr = match.groupValues[1].trim()
            val rawAmount = match.groupValues[2].replace(",", "")
            val amountPaise = CurrencyUtils.parseToPaise(rawAmount)
            if (amountPaise <= 0L) continue

            // Determine bounded context window for this entry
            val prevAmountIdx = if (idx > 0) amountIndices[idx - 1].first else -1
            val nextAmountIdx = if (idx + 1 < amountIndices.size) amountIndices[idx + 1].first else lines.size

            val startWindow = maxOf(prevAmountIdx + 1, lineIdx - 3)
            val endWindow = minOf(nextAmountIdx, lineIdx + 4)
            val contextLines = lines.subList(startWindow, endWindow)
            val contextText = contextLines.joinToString(" ")
            val lowerContext = contextText.lowercase(Locale.ROOT)

            // Status
            val status = when {
                lowerContext.contains("failed") || lowerContext.contains("declined") -> TransactionStatus.FAILED
                lowerContext.contains("pending") || lowerContext.contains("processing") -> TransactionStatus.PENDING
                else -> TransactionStatus.COMPLETED
            }

            // Direction: Only check the amount sign and lines up to the amount line
            val linesUpToAmount = lines.subList(startWindow, lineIdx + 1)
            val isCredit = signStr == "+" ||
                    linesUpToAmount.any { l ->
                        val lower = l.lowercase(Locale.ROOT)
                        lower.startsWith("received from") ||
                                lower.startsWith("from: ") ||
                                lower.contains("credited") ||
                                lower.contains("+₹") ||
                                lower.contains("+ ₹")
                    }
            val type = if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE

            // Merchant extraction: First check for explicit prefixes
            var merchant = ""
            for (j in (lineIdx - 1) downTo startWindow) {
                val candidateLine = lines[j]
                val lower = candidateLine.lowercase(Locale.ROOT)
                if (lower.startsWith("paid to ") || lower.startsWith("to: ") ||
                    lower.startsWith("sent to ") || lower.startsWith("received from ") ||
                    lower.startsWith("transfer to ")
                ) {
                    val cleaned = cleanMerchantString(candidateLine)
                    if (isValidMerchantName(cleaned)) {
                        merchant = cleaned
                        break
                    }
                }
            }

            // If no explicit prefix, check lines in window before amount
            if (merchant.isBlank()) {
                for (j in (lineIdx - 1) downTo startWindow) {
                    val candidateLine = lines[j]
                    if (isIgnoredHeaderLine(candidateLine) || isDateLine(candidateLine)) continue
                    val cleaned = cleanMerchantString(candidateLine)
                    if (isValidMerchantName(cleaned)) {
                        merchant = cleaned
                        break
                    }
                }
            }
            // Check lines in the entry window after the amount if still not found
            if (merchant.isBlank()) {
                for (j in (lineIdx + 1) until endWindow) {
                    val candidateLine = lines[j]
                    if (isIgnoredHeaderLine(candidateLine) || isDateLine(candidateLine)) continue
                    val cleaned = cleanMerchantString(candidateLine)
                    if (isValidMerchantName(cleaned)) {
                        merchant = cleaned
                        break
                    }
                }
            }
            if (merchant.isBlank()) {
                merchant = if (type == TransactionType.INCOME) "UPI Credit" else "UPI Payment"
            }

            // Reference ID
            var refId: String? = null
            val utrMatcher = utrPattern.matcher(contextText)
            if (utrMatcher.find()) {
                refId = utrMatcher.group(1)?.trim()
            }

            // Date / Time
            val (parsedInstant, rawDate) = extractDateTime(contextText, contextLines)

            candidates.add(
                ScreenshotCandidate(
                    amountPaise = amountPaise,
                    merchant = merchant,
                    type = type,
                    status = status,
                    referenceId = refId,
                    timestamp = parsedInstant,
                    sourceApp = sourceApp,
                    rawDateText = rawDate,
                    isExcluded = (status == TransactionStatus.FAILED),
                    confidence = if (merchant != "UPI Credit" && merchant != "UPI Payment") "High" else "Review"
                )
            )
        }

        return candidates
    }

    // --- Helper Sanitization & DateTime Parsers ---

    private fun cleanMerchantString(raw: String): String {
        return raw.replace(Regex("""(?i)^(?:paid to|transfer to|sent to|to:|from:|received from|to|from)\s+"""), "")
            .replace(Regex("""(?i)\s*(?:completed|successful|payment successful|success|failed|pending)$"""), "")
            .replace(Regex("""^[•\-\*]\s*"""), "")
            .trim()
    }

    private fun isDateLine(line: String): Boolean {
        val lower = line.lowercase(Locale.ROOT)
        if (lower.startsWith("paid on ") || lower.startsWith("received on ") ||
            lower.contains("today") || lower.contains("yesterday")
        ) return true
        for (regex in dateRegexList) {
            if (regex.containsMatchIn(line)) return true
        }
        return false
    }

    private fun isValidMerchantName(name: String): Boolean {
        if (name.length < 2 || name.length > 50) return false
        val lower = name.lowercase(Locale.ROOT)
        if (lower.contains("upi") && lower.contains("id")) return false
        if (lower.contains("transaction history") || lower.contains("transactions")) return false
        if (lower.contains("payment successful") || lower.contains("completed")) return false
        if (lower.contains("debited from") || lower.contains("credited to")) return false
        if (lower.contains("bank account") || lower.contains("••••")) return false
        if (lower.contains("supercashback") || lower.contains("supercard")) return false
        if (lower.matches(Regex("""^[\d\s,.:\-]+$"""))) return false // pure digits or punctuation
        if (isDateLine(name)) return false
        return true
    }

    private fun isIgnoredHeaderLine(line: String): Boolean {
        val lower = line.lowercase(Locale.ROOT)
        return lower.contains("transaction history") ||
                lower.contains("google pay") ||
                lower.contains("super.money") ||
                lower.contains("payment details") ||
                lower.contains("payment successful") ||
                lower.contains("payment failed") ||
                lower.contains("debited from") ||
                lower.contains("credited to") ||
                lower.contains("supercashback") ||
                lower.contains("bank server") ||
                lower.contains("••••") ||
                lower.contains("@ok") ||
                lower.contains("@okhdfcbank") ||
                lower.contains("@icici") ||
                lower.contains("@axisbank")
    }

    private val dateRegexList = listOf(
        // "28 Oct 2026, 04:15 pm" or "28 Oct 2026, 4:15 PM"
        Regex("""(\d{1,2}\s+[A-Za-z]{3,9}\s+\d{4})[,\s•]+(\d{1,2}:\d{2}(?:\s*[AaPp][Mm])?)"""),
        // "Oct 28, 2026, 8:45 PM"
        Regex("""([A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})[,\s•]+(\d{1,2}:\d{2}(?:\s*[AaPp][Mm])?)"""),
        // "28 Oct 2026" or "Oct 28, 2026"
        Regex("""\b(\d{1,2}\s+[A-Za-z]{3,9}\s+\d{4})\b"""),
        Regex("""\b([A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})\b"""),
        // "28 Oct, 4:30 pm" (no year)
        Regex("""\b(\d{1,2}\s+[A-Za-z]{3,9})[,\s•]+(\d{1,2}:\d{2}(?:\s*[AaPp][Mm])?)\b"""),
        // "28 Oct" or "27 Oct"
        Regex("""\b(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*)\b""", RegexOption.IGNORE_CASE)
    )

    private fun extractDateTime(text: String, lines: List<String>): Pair<Instant, String?> {
        val currentYear = LocalDate.now().year
        val zone = ZoneId.systemDefault()

        for (regex in dateRegexList) {
            val match = regex.find(text)
            if (match != null) {
                val fullMatched = match.value.trim()
                val parsedInstant = parseFlexibleDateString(fullMatched, currentYear, zone)
                if (parsedInstant != null) {
                    return Pair(parsedInstant, fullMatched)
                }
            }
        }

        // Relative dates: "Today", "Yesterday"
        val lower = text.lowercase(Locale.ROOT)
        if (lower.contains("today")) {
            return Pair(Instant.now(), "Today")
        }
        if (lower.contains("yesterday")) {
            val yesterday = LocalDate.now().minusDays(1).atStartOfDay(zone).toInstant()
            return Pair(yesterday, "Yesterday")
        }

        return Pair(Instant.now(), null)
    }

    private fun parseFlexibleDateString(raw: String, defaultYear: Int, zone: ZoneId): Instant? {
        val cleaned = raw.replace("•", ",").replace(Regex("""\s+"""), " ").trim()
        val formatters = listOf(
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy, h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH)
        )

        for (formatter in formatters) {
            try {
                if (cleaned.contains(":")) {
                    val dt = LocalDateTime.parse(cleaned, formatter)
                    return dt.atZone(zone).toInstant()
                } else {
                    val d = LocalDate.parse(cleaned, formatter)
                    return d.atStartOfDay(zone).toInstant()
                }
            } catch (_: Exception) {}
        }

        // Handle cases without year: "28 Oct, 4:30 pm"
        try {
            val withoutYear = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.ENGLISH)
            val parts = cleaned.split(",")
            if (parts.size == 2) {
                val withYear = "${parts[0].trim()} $defaultYear, ${parts[1].trim()}"
                val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.ENGLISH)
                return LocalDateTime.parse(withYear, fmt).atZone(zone).toInstant()
            }
        } catch (_: Exception) {}

        return null
    }
}
