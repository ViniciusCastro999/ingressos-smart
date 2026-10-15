package com.viniciuscastro.ingressos.ui.common

import com.viniciuscastro.ingressos.domain.model.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Presentation formatting (pt-BR). Kept out of the domain: money is cents there, text only here. */
object Formatters {

    private val locale: Locale = Locale.forLanguageTag("pt-BR")
    private val eventDateTime = DateTimeFormatter.ofPattern("EEE, dd 'de' MMM 'às' HH:mm", locale)
    private val weekdayTime = DateTimeFormatter.ofPattern("EEE · HH:mm", locale)
    private val dayOfMonth = DateTimeFormatter.ofPattern("dd", locale)
    private val monthShort = DateTimeFormatter.ofPattern("MMM", locale)
    private val dateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
    private val time = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val shortDate = DateTimeFormatter.ofPattern("EEE, dd MMM", locale)

    // NumberFormat is not thread-safe: one instance per call.
    fun money(value: Money): String =
        NumberFormat.getCurrencyInstance(locale).format(BigDecimal.valueOf(value.cents, 2))

    fun eventDateTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        eventDateTime.format(instant.atZone(zone)).replaceFirstChar { it.titlecase(locale) }

    fun weekdayTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        weekdayTime.format(instant.atZone(zone)).replace(".", "").replaceFirstChar { it.titlecase(locale) }

    fun dayOfMonth(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        dayOfMonth.format(instant.atZone(zone))

    fun monthShort(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        monthShort.format(instant.atZone(zone)).trimEnd('.').uppercase(locale)

    fun time(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String = time.format(instant.atZone(zone))

    /** "Sáb, 14 nov" (identity style: no trailing dots, capitalised weekday). */
    fun shortDate(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        shortDate.format(instant.atZone(zone)).replace(".", "").replaceFirstChar { it.titlecase(locale) }

    fun dateTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        dateTime.format(instant.atZone(zone))

    /** Short, human-friendly form of a UUID for receipts. */
    fun shortId(id: String): String = id.substringBefore('-').take(8).uppercase(locale)
}
