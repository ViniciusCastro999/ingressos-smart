package com.viniciuscastro.ingressos.domain.model

import java.text.Normalizer
import java.time.Instant

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val venue: String,
    val city: String,
    val startsAt: Instant,
    val ticketPrice: Money,
    val availableTickets: Int,
) {
    val isSoldOut: Boolean get() = availableTickets <= 0

    /** Upper bound of the quantity selector: never above the stock nor the per-purchase limit. */
    val maxQuantityPerPurchase: Int get() = availableTickets.coerceIn(0, MAX_TICKETS_PER_PURCHASE)

    fun checkQuantity(quantity: Int): QuantityCheck = when {
        isSoldOut -> QuantityCheck.SoldOut
        quantity !in MIN_TICKETS_PER_PURCHASE..maxQuantityPerPurchase ->
            QuantityCheck.OutOfRange(min = MIN_TICKETS_PER_PURCHASE, max = maxQuantityPerPurchase)
        else -> QuantityCheck.Valid
    }

    fun totalFor(quantity: Int): Money = ticketPrice * quantity

    /** Accent- and case-insensitive search over title, venue and city ("sao paulo" finds "São Paulo"). */
    fun matches(query: String): Boolean {
        val terms = query.normalizedForSearch().split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) return true
        val haystack = "$title $venue $city".normalizedForSearch()
        return terms.all { it in haystack }
    }

    fun snapshot(): EventSnapshot = EventSnapshot(id = id, title = title, venue = venue, startsAt = startsAt)

    companion object {
        const val MIN_TICKETS_PER_PURCHASE = 1
        const val MAX_TICKETS_PER_PURCHASE = 10
    }
}

private val COMBINING_MARKS = Regex("\\p{Mn}+")

private fun String.normalizedForSearch(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(COMBINING_MARKS, "")

sealed interface QuantityCheck {
    data object Valid : QuantityCheck
    data object SoldOut : QuantityCheck
    data class OutOfRange(val min: Int, val max: Int) : QuantityCheck
}

/** Event data frozen at purchase time, so the receipt does not change if the catalog does. */
data class EventSnapshot(
    val id: String,
    val title: String,
    val venue: String,
    val startsAt: Instant,
)
