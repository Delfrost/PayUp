package com.payup.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val indianFormat = ThreadLocal.withInitial {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    /**
     * Format paise amount to display string.
     * e.g., 150000L -> "₹1,500.00"
     */
    fun formatPaise(paise: Long): String {
        val formatter = indianFormat.get() ?: NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return formatter.format(paise / 100.0)
    }

    /**
     * Format paise without decimal for clean display when appropriate.
     * e.g., 150000L -> "₹1,500"
     */
    fun formatPaiseCompact(paise: Long): String {
        return if (paise % 100 == 0L) {
            "₹${String.format("%,d", paise / 100)}"
        } else {
            formatPaise(paise)
        }
    }

    /**
     * Parse a rupee string to paise.
     * e.g., "1500.50" -> 150050L
     * Deterministic integer math without floating-point drift.
     */
    fun parseToPaise(amount: String): Long {
        val cleaned = amount.replace(",", "").replace("₹", "").trim()
        if (cleaned.isBlank()) return 0L
        return try {
            BigDecimal(cleaned)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (_: Exception) {
            0L
        }
    }
}
