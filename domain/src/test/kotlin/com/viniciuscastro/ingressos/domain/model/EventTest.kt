package com.viniciuscastro.ingressos.domain.model

import com.viniciuscastro.ingressos.domain.fake.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventTest {

    @Test
    fun `quantity must be between one and the purchasable maximum`() {
        val event = TestData.event(availableTickets = 100)

        assertEquals(QuantityCheck.Valid, event.checkQuantity(1))
        assertEquals(QuantityCheck.Valid, event.checkQuantity(Event.MAX_TICKETS_PER_PURCHASE))
        assertEquals(QuantityCheck.OutOfRange(1, 10), event.checkQuantity(0))
        assertEquals(QuantityCheck.OutOfRange(1, 10), event.checkQuantity(11))
    }

    @Test
    fun `maximum per purchase is limited by the remaining stock`() {
        val event = TestData.event(availableTickets = 3)

        assertEquals(3, event.maxQuantityPerPurchase)
        assertEquals(QuantityCheck.OutOfRange(1, 3), event.checkQuantity(4))
    }

    @Test
    fun `sold out event rejects any quantity`() {
        val event = TestData.event(availableTickets = 0)

        assertEquals(QuantityCheck.SoldOut, event.checkQuantity(1))
        assertEquals(0, event.maxQuantityPerPurchase)
    }

    @Test
    fun `total is the ticket price times the quantity, in cents`() {
        assertEquals(Money(36_000), TestData.event(price = Money(12_000)).totalFor(3))
    }

    @Test
    fun `search ignores case and accents and requires every term`() {
        val event = TestData.event() // Festival de Jazz · Auditório Ibirapuera · São Paulo

        assertTrue(event.matches(""))
        assertTrue(event.matches("jazz"))
        assertTrue(event.matches("AUDITORIO"))
        assertTrue(event.matches("sao paulo jazz"))
        assertFalse(event.matches("rock"))
        assertFalse(event.matches("jazz recife")) // every term must match
    }

    @Test
    fun `money cannot be negative`() {
        assertFailsWith<IllegalArgumentException> { Money(-1) }
    }
}
