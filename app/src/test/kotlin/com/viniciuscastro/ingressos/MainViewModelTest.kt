package com.viniciuscastro.ingressos

import app.cash.turbine.test
import com.viniciuscastro.ingressos.domain.fake.FixedClock
import com.viniciuscastro.ingressos.domain.fake.ImmediateTransactionRunner
import com.viniciuscastro.ingressos.domain.fake.InMemoryEventRepository
import com.viniciuscastro.ingressos.domain.fake.InMemoryPurchaseRepository
import com.viniciuscastro.ingressos.domain.fake.RecordingTelemetry
import com.viniciuscastro.ingressos.domain.fake.SequentialIdGenerator
import com.viniciuscastro.ingressos.domain.fake.TestData
import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import com.viniciuscastro.ingressos.domain.payment.PaymentResultParser
import com.viniciuscastro.ingressos.domain.usecase.ProcessPaymentResultUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val event = TestData.event(price = Money(5_000), availableTickets = 10)
    private val events = InMemoryEventRepository(event)
    private val purchases = InMemoryPurchaseRepository(listOf(TestData.pendingPurchase(id = "pur-1", event = event)))
    private var nextResult: PaymentResult = TestData.approved("pur-1", Money(10_000))
    private val viewModel = MainViewModel(
        parser = PaymentResultParser { nextResult },
        processPaymentResult = ProcessPaymentResultUseCase(
            purchases, events, ImmediateTransactionRunner(), SequentialIdGenerator("t"), FixedClock(), RecordingTelemetry(),
        ),
    )

    @Test
    fun `terminal callback records the result and opens the purchase`() = runTest {
        viewModel.effects.test {
            viewModel.onPaymentCallback("ingressos://payment-result?response=...")

            assertEquals(MainEffect.ShowPurchase("pur-1"), awaitItem())
        }
        assertEquals(PurchaseStatus.APPROVED, purchases.stored("pur-1").status)
    }

    @Test
    fun `callback delivered twice is recorded once`() = runTest {
        viewModel.effects.test {
            viewModel.onPaymentCallback("callback")
            viewModel.onPaymentCallback("callback")

            assertEquals(MainEffect.ShowPurchase("pur-1"), awaitItem())
            assertEquals(MainEffect.ShowPurchase("pur-1"), awaitItem())
        }
        assertEquals(2, purchases.stored("pur-1").tickets.size)
        assertEquals(8, events.stored(event.id).availableTickets)
    }

    @Test
    fun `callback for an unknown purchase is reported and ignored`() = runTest {
        nextResult = TestData.approved("forged", Money(10_000))

        viewModel.effects.test {
            viewModel.onPaymentCallback("callback")

            assertEquals(MainEffect.UnmatchedPaymentCallback, awaitItem())
        }
        assertEquals(PurchaseStatus.PENDING, purchases.stored("pur-1").status)
    }
}
