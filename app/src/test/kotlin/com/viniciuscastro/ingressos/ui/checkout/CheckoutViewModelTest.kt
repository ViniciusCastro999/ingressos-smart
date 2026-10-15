package com.viniciuscastro.ingressos.ui.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.viniciuscastro.ingressos.MainDispatcherRule
import com.viniciuscastro.ingressos.domain.fake.FakePaymentTerminal
import com.viniciuscastro.ingressos.domain.fake.FixedClock
import com.viniciuscastro.ingressos.domain.fake.InMemoryEventRepository
import com.viniciuscastro.ingressos.domain.fake.InMemoryPurchaseRepository
import com.viniciuscastro.ingressos.domain.fake.RecordingTelemetry
import com.viniciuscastro.ingressos.domain.fake.SequentialIdGenerator
import com.viniciuscastro.ingressos.domain.fake.TestData
import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.usecase.ObserveEventUseCase
import com.viniciuscastro.ingressos.domain.usecase.StartPurchaseUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val event = TestData.event(id = "evt-1", price = Money(12_000), availableTickets = 3)
    private val events = InMemoryEventRepository(event)
    private val purchases = InMemoryPurchaseRepository()
    private val terminal = FakePaymentTerminal()
    private val startPurchase = StartPurchaseUseCase(events, purchases, terminal, FixedClock(), RecordingTelemetry())
    private val ids = SequentialIdGenerator("checkout")

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle(mapOf("eventId" to event.id))) =
        CheckoutViewModel(handle, ObserveEventUseCase(events), startPurchase, ids)

    /** uiState is WhileSubscribed: keep an (eager) subscriber, like the screen does. */
    private fun TestScope.subscribe(viewModel: CheckoutViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

    @Test
    fun `quantity is clamped between one and the purchasable maximum`() = runTest {
        val viewModel = viewModel().also { subscribe(it) }

        repeat(5) { viewModel.onIncrease() }
        val atMax = assertIs<CheckoutUiState.Ready>(viewModel.uiState.value)
        repeat(5) { viewModel.onDecrease() }
        val atMin = assertIs<CheckoutUiState.Ready>(viewModel.uiState.value)

        assertEquals(3, atMax.quantity)
        assertEquals(Money(36_000), atMax.total)
        assertEquals(1, atMin.quantity)
    }

    @Test
    fun `a double tap on pay starts a single payment and opens the receipt`() = runTest {
        terminal.onDispatch = { delay(500) }
        val viewModel = viewModel().also { subscribe(it) }
        viewModel.onPaymentMethodSelected(PaymentMethod.PIX)

        viewModel.effects.test {
            viewModel.onPayClicked()
            viewModel.onPayClicked()
            assertEquals(true, (viewModel.uiState.value as CheckoutUiState.Ready).isSubmitting)

            assertEquals(CheckoutEffect.ShowReceipt("checkout-1"), awaitItem())
        }
        assertEquals(1, terminal.requests.size)
        assertEquals(PaymentMethod.PIX, terminal.requests.single().method)
    }

    @Test
    fun `the idempotency key survives recreation so a re-sent tap does not charge again`() = runTest {
        val handle = SavedStateHandle(mapOf("eventId" to event.id))
        val first = viewModel(handle).also { subscribe(it) }
        first.onPayClicked()
        advanceUntilIdle()

        // Same SavedStateHandle = rotation or process death restoring the screen.
        val recreated = viewModel(handle).also { subscribe(it) }
        recreated.effects.test {
            recreated.onPayClicked()
            assertEquals(CheckoutEffect.ShowReceipt("checkout-1"), awaitItem())
        }

        assertEquals(1, terminal.requests.size)
        assertEquals(PurchaseStatus.PENDING, purchases.stored("checkout-1").status)
    }

    @Test
    fun `a payment already waiting for the terminal is reported instead of charging again`() = runTest {
        purchases.create(TestData.pendingPurchase(id = "other-checkout", event = event))
        val viewModel = viewModel().also { subscribe(it) }

        viewModel.effects.test {
            viewModel.onPayClicked()
            assertEquals(
                CheckoutEffect.ShowMessage(CheckoutMessage.AnotherPaymentInProgress("other-checkout")),
                awaitItem(),
            )
        }
        assertEquals(0, terminal.requests.size)
    }

    @Test
    fun `unknown event shows not found`() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("eventId" to "missing")))

        assertEquals(CheckoutUiState.NotFound, viewModel.uiState.first { it != CheckoutUiState.Loading })
    }
}
