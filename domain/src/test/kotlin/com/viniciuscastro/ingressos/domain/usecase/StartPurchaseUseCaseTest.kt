package com.viniciuscastro.ingressos.domain.usecase

import com.viniciuscastro.ingressos.domain.common.TelemetryEvent
import com.viniciuscastro.ingressos.domain.fake.FakePaymentTerminal
import com.viniciuscastro.ingressos.domain.fake.FixedClock
import com.viniciuscastro.ingressos.domain.fake.InMemoryEventRepository
import com.viniciuscastro.ingressos.domain.fake.InMemoryPurchaseRepository
import com.viniciuscastro.ingressos.domain.fake.RecordingTelemetry
import com.viniciuscastro.ingressos.domain.fake.TestData
import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.StatusReason
import com.viniciuscastro.ingressos.domain.payment.DispatchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StartPurchaseUseCaseTest {

    private val event = TestData.event(price = Money(5_000), availableTickets = 100)
    private val events = InMemoryEventRepository(event)
    private val purchases = InMemoryPurchaseRepository()
    private val terminal = FakePaymentTerminal()
    private val telemetry = RecordingTelemetry()
    private val startPurchase = StartPurchaseUseCase(events, purchases, terminal, FixedClock(), telemetry)

    private fun command(key: String = "checkout-1", quantity: Int = 2) =
        StartPurchaseCommand(key, event.id, quantity, PaymentMethod.CREDIT)

    @Test
    fun `persists a pending purchase and dispatches one payment for the total`() = runTest {
        val result = startPurchase(command(quantity = 2))

        assertEquals(StartPurchaseResult.AwaitingPayment("checkout-1"), result)
        assertEquals(PurchaseStatus.PENDING, purchases.stored("checkout-1").status)
        val request = terminal.requests.single()
        assertEquals("checkout-1", request.reference)
        assertEquals(Money(10_000), request.amount)
        assertEquals(PaymentMethod.CREDIT, request.method)
    }

    @Test
    fun `re-sending the same key while pending does not dispatch a second payment`() = runTest {
        startPurchase(command())

        val retry = startPurchase(command())

        assertEquals(StartPurchaseResult.AwaitingPayment("checkout-1"), retry)
        assertEquals(1, terminal.requests.size)
        assertTrue(TelemetryEvent.DuplicateAttemptBlocked("checkout-1", PurchaseStatus.PENDING) in telemetry.events)
    }

    @Test
    fun `concurrent double tap dispatches exactly one payment`() = runTest {
        terminal.onDispatch = { delay(100) } // the first call is still dispatching when the second arrives

        val results = List(2) { async { startPurchase(command()) } }.awaitAll()

        assertEquals(1, terminal.requests.size)
        assertTrue(results.all { it == StartPurchaseResult.AwaitingPayment("checkout-1") })
    }

    @Test
    fun `a key that already has an outcome returns it without charging again`() = runTest {
        val approved = TestData.pendingPurchase(id = "checkout-1", event = event)
            .resolve(TestData.approved("checkout-1", Money(10_000)), TestData.NOW) { "t" }.purchase
        purchases.put(approved)

        val result = startPurchase(command())

        assertEquals(StartPurchaseResult.AlreadyProcessed(approved), result)
        assertTrue(terminal.requests.isEmpty())
    }

    @Test
    fun `a payment waiting for the terminal blocks a different checkout`() = runTest {
        startPurchase(command(key = "checkout-1"))

        val result = startPurchase(command(key = "checkout-2"))

        assertEquals(StartPurchaseResult.AnotherPaymentInProgress("checkout-1"), result)
        assertEquals(1, terminal.requests.size)
        assertEquals(listOf("checkout-1"), purchases.all.map { it.id })
    }

    @Test
    fun `invalid requests create nothing and charge nothing`() = runTest {
        val soldOutEvents = InMemoryEventRepository(TestData.event(id = "sold-out", availableTickets = 0), event)
        val useCase = StartPurchaseUseCase(soldOutEvents, purchases, terminal, FixedClock(), telemetry)

        assertEquals(StartPurchaseResult.InvalidQuantity(1, 10), useCase(command(quantity = 11)))
        assertEquals(StartPurchaseResult.InvalidQuantity(1, 10), useCase(command(quantity = 0)))
        assertEquals(StartPurchaseResult.SoldOut, useCase(command().copy(eventId = "sold-out")))
        assertEquals(StartPurchaseResult.EventNotFound, useCase(command().copy(eventId = "unknown")))
        assertTrue(purchases.all.isEmpty())
        assertTrue(terminal.requests.isEmpty())
    }

    @Test
    fun `missing payment app records a failed purchase that does not block the next checkout`() = runTest {
        terminal.dispatchResult = DispatchResult.Unavailable("app not installed")

        val result = startPurchase(command(key = "checkout-1"))

        assertIs<StartPurchaseResult.TerminalUnavailable>(result)
        val failed = purchases.stored("checkout-1")
        assertEquals(PurchaseStatus.FAILED, failed.status)
        assertEquals(StatusReason.TerminalUnavailable, failed.reason)

        terminal.dispatchResult = DispatchResult.Dispatched
        assertEquals(StartPurchaseResult.AwaitingPayment("checkout-2"), startPurchase(command(key = "checkout-2")))
    }
}
