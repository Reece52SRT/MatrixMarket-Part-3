package com.matrixmarket.app.util

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Formats the ISO-8601 dates the API returns into friendly text.
object DateFormats {

    fun parse(iso: String): LocalDateTime? {
        val trimmed = iso.trim()
        return runCatching { OffsetDateTime.parse(trimmed).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(trimmed.removeSuffix("Z")) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(trimmed.take(19)) }.getOrNull()
    }

    // "Fri 23 Oct, 14:00"
    fun meeting(iso: String, locale: Locale = Locale.getDefault()): String =
        parse(iso)?.format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", locale)) ?: iso

    // "October 2026"
    fun monthYear(iso: String, locale: Locale = Locale.getDefault()): String =
        parse(iso)?.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)) ?: iso.take(7)

    // "23 Oct 2026, 14:05"
    fun dateTime(iso: String, locale: Locale = Locale.getDefault()): String =
        parse(iso)?.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale)) ?: iso

    // Wall-clock campus time sent to the API, e.g. 2026-10-23T14:00:00
    fun toApi(dateTime: LocalDateTime): String =
        dateTime.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    fun formatRand(amount: Double): String = String.format(Locale.US, "R %.2f", amount)
}
