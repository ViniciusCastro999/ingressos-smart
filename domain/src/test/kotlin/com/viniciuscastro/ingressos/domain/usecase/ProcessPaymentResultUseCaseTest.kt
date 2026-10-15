package com.viniciuscastro.ingressos.domain.usecase

import com.viniciuscastro.ingressos.domain.common.TelemetryEvent
import com.viniciuscastro.ingressos.domain.fake.FixedClock
import com.viniciuscastro.ingressos.domain.fake.ImmediateTransactionRunner
import com.viniciuscastro.ingressos.domain.fake.InMemoryEventRepository
import com.viniciuscastro.ingressos.domain.fake.InMemoryPurchaseRepository
import com.viniciuscastro.ingressos.domain.fake.RecordingTelemetry
import com.viniciuscastro.ingressos.domain.fake.SequentialIdGenerator
import com.viniciuscastro.ingressos.domain.fake.TestData
import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.StatusReason
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProcessPaymentResultUseCaseTest {

    private val event = TestData.event(price = Money(5_000), availableTickets = 10)
    private val pending = TestData.pendingPurchase(id = "pur-1", event = event, quantity = 2) // R$ 100,00
    private val events = InMemoryEventRepository(event)
    private val purchases = InMemoryPurchaseRepository(listOf(pending))
    private val telemetry = RecordingTelemetry()
    private val processPaymentResult = ProcessPaymentResultUseCase(
        purchases = purchases,
        events = events,
        transaction = ImmediateTransactionRunner(),
        idGenerator = SequentialIdGenerator("ticket"),
        clock = FixedClock(),
        telemetry = telemetry,
    )

    @Test
    fun `approval matched by reference approves, issues tickets and consumes stock`() = runTest {
        val result = processPaymentResult(TestData.approved("pur-1", Money(10_000)))

        val approved = assertIs<ProcessPaymentResult.Applied>(result).purchase
        assertEquals(PurchaseStatus.APPROVED, purchases.stored("pur-1").status)
        assertEquals(2, approved.tickets.size)
        assertEquals(8, events.stored(event.id).availableTickets)
    }

    @Test
    fun `duplicated approval callback changes tickets and stock only once`() = runTest {
        processPaymentResult(TestData.approved("pur-1", Money(10_000)))

        val duplicate = processPaymentResult(TestData.approved("pur-1", Money(10_000)))

        assertIs<ProcessPaymentResult.AlreadyProcessed>(duplicate)
        assertEquals(listOf("ticket-1", "ticket-2"), purchases.stored("pur-1").tickets.map { it.id })
        assertEquals(8, events.stored(event.id).availableTickets)
        assertTrue(TelemetryEvent.CallbackIgnored("pur-1", PurchaseStatus.APPROVED) in telemetry.events)
    }

    @Test
    fun `error callback without reference resolves the purchase awaiting the terminal`() = runTest {
        val result = processPaymentResult(TestData.canceledByUser())

        assertEquals(PurchaseStatus.CANCELED, assertIs<ProcessPaymentResult.Applied>(result).purchase.status)
        assertEquals(10, events.stored(event.id).availableTickets)
    }

    @Test
    fun `result with an unknown reference is ignored even if a purchase is pending`() = runTest {
        val result = processPaymentResult(TestData.approved("forged-reference", Money(10_000)))

        assertEquals(ProcessPaymentResult.Unmatched("forged-reference"), result)
        assertEquals(PurchaseStatus.PENDING, purchases.stored("pur-1").status)
        assertTrue(TelemetryEvent.CallbackUnmatched("forged-reference") in telemetry.events)
    }

    @Test
    fun `callback without reference and nothing pending is unmatched`() = runTest {
        processPaymentResult(TestData.canceledByUser())

        assertEquals(ProcessPaymentResult.Unmatched(null), processPaymentResult(TestData.declined()))
        assertEquals(PurchaseStatus.CANCELED, purchases.stored("pur-1").status)
    }

    @Test
    fun `approval for a divergent amount issues no ticket and keeps the stock`() = runTest {
        processPaymentResult(TestData.approved("pur-1", Money(1)))

        val held = purchases.stored("pur-1")
        assertEquals(PurchaseStatus.UNCONFIRMED, held.status)
        assertEquals(StatusReason.AmountMismatch, held.reason)
        assertTrue(held.tickets.isEmpty())
        assertEquals(10, events.stored(event.id).availableTickets)
    }

    @Test
    fun `late approval recovers a purchase that was recorded as failed`() = runTest {
        processPaymentResult(PaymentResult.Failed(null, null, "ilegível"))

        processPaymentResult(TestData.approved("pur-1", Money(10_000)))

        assertEquals(PurchaseStatus.APPROVED, purchases.stored("pur-1").status)
        assertTrue(TelemetryEvent.LateApprovalRecovered("pur-1", PurchaseStatus.FAILED) in telemetry.events)
    }

    @Test
    fun `approval racing with an operator release is still applied`() = runTest {
        var released = false
        purchases.beforeUpdate = { id ->
            if (!released) { // a concurrent release lands between the read and the compare-and-set
                released = true
                purchases.put(purchases.stored(id).markUnconfirmed(TestData.NOW))
            }
        }

        val result = processPaymentResult(TestData.approved("pur-1", Money(10_000)))

        assertIs<ProcessPaymentResult.Applied>(result)
        assertEquals(PurchaseStatus.APPROVED, purchases.stored("pur-1").status)
        assertEquals(8, events.stored(event.id).availableTickets)
    }
}
