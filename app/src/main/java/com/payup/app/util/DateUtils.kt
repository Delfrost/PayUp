package com.payup.app.util

import java.time.*
import java.time.format.DateTimeFormatter

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    private val zone: ZoneId
        get() = ZoneId.systemDefault()

    fun formatDate(instant: Instant): String =
        instant.atZone(zone).format(dateFormatter)

    fun formatTime(instant: Instant): String =
        instant.atZone(zone).format(timeFormatter)

    fun formatDateTime(instant: Instant): String =
        instant.atZone(zone).format(dateTimeFormatter)

    fun currentYearMonth(): String =
        YearMonth.now().format(monthYearFormatter)

    fun monthStart(yearMonth: String? = null): Long {
        val ym = if (yearMonth != null) YearMonth.parse(yearMonth) else YearMonth.now()
        return ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun monthEnd(yearMonth: String? = null): Long {
        val ym = if (yearMonth != null) YearMonth.parse(yearMonth) else YearMonth.now()
        // Boundary at the last millisecond of the month to capture sub-second transactions
        return ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    }

    fun relativeDate(instant: Instant): String {
        val today = LocalDate.now(zone)
        val date = instant.atZone(zone).toLocalDate()
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            date.isAfter(today.minusDays(7)) -> date.dayOfWeek.name.lowercase()
                .replaceFirstChar { it.uppercase() }
            else -> formatDate(instant)
        }
    }
}
