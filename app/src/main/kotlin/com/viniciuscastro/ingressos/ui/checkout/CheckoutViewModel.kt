package com.viniciuscastro.ingressos.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viniciuscastro.ingressos.domain.common.IdGenerator
import com.viniciuscastro.ingressos.domain.model.Event
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.usecase.ObserveEventUseCase
import com.viniciuscastro.ingressos.domain.usecase.StartPurchaseCommand
import com.viniciuscastro.ingressos.domain.usecase.StartPurchaseResult
import com.viniciuscastro.ingressos.domain.usecase.StartPurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CheckoutUiState {
    data object Loading : CheckoutUiState
    data object NotFound : CheckoutUiState

    data class Ready(
        val event: Event,
        val quantity: Int,
        val paymentMethod: PaymentMethod?,
        val isSubmitting: Boolean,
    ) : CheckoutUiState {
        val maxQuantity: Int get() = event.maxQuantityPerPurchase
        val total get() = event.totalFor(quantity)
        val canPay: Boolean get() = !event.isSoldOut && !isSubmitting
    }
}

sealed interface CheckoutEffect {
    data class ShowReceipt(val purchaseId: String) : CheckoutEffect
    data class ShowMessage(val message: CheckoutMessage) : CheckoutEffect
}

sealed interface CheckoutMessage {
    data class AnotherPaymentInProgress(val purchaseId: String) : CheckoutMessage
    data object SoldOut : CheckoutMessage
    data object EventUnavailable : CheckoutMessage
    data class InvalidQuantity(val min: Int, val max: Int) : CheckoutMessage
    data object UnexpectedError : CheckoutMessage
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeEvent: ObserveEventUseCase,
    private val startPurchase: StartPurchaseUseCase,
    idGenerator: IdGenerator,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle[ARG_EVENT_ID]) { "Missing $ARG_EVENT_ID" }

    /**
     * One idempotency key per checkout. Stored in the SavedStateHandle, so rotation or process death
     * keeps it: a tap re-sent after that replays the same purchase instead of charging again.
     */
    private val idempotencyKey: String =
        savedStateHandle.get<String>(KEY_IDEMPOTENCY) ?: idGenerator.newId().also { savedStateHandle[KEY_IDEMPOTENCY] = it }

    private val quantity = savedStateHandle.getStateFlow(KEY_QUANTITY, savedStateHandle.get<Int>(ARG_QUANTITY) ?: 1)
    private val paymentMethod = savedStateHandle.getStateFlow<String?>(KEY_PAYMENT_METHOD, null)
    private val isSubmitting = MutableStateFlow(false)

    private val effectChannel = Channel<CheckoutEffect>(Channel.BUFFERED)
    val effects: Flow<CheckoutEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<CheckoutUiState> =
        combine(observeEvent(eventId), quantity, paymentMethod, isSubmitting) { event, quantity, method, submitting ->
            if (event == null) {
                CheckoutUiState.NotFound
            } else {
                CheckoutUiState.Ready(
                    event = event,
                    quantity = quantity.coerceIn(1, event.maxQuantityPerPurchase.coerceAtLeast(1)),
                    paymentMethod = method?.let(PaymentMethod::valueOf),
                    isSubmitting = submitting,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CheckoutUiState.Loading)

    fun onIncrease() {
        val ready = uiState.value as? CheckoutUiState.Ready ?: return
        savedStateHandle[KEY_QUANTITY] = (ready.quantity + 1).coerceAtMost(ready.maxQuantity)
    }

    fun onDecrease() {
        val ready = uiState.value as? CheckoutUiState.Ready ?: return
        savedStateHandle[KEY_QUANTITY] = (ready.quantity - 1).coerceAtLeast(1)
    }

    fun onPaymentMethodSelected(method: PaymentMethod?) {
        savedStateHandle[KEY_PAYMENT_METHOD] = method?.name
    }

    fun onPayClicked() {
        // First line of defense against double taps; the use case is the actual guarantee.
        if (isSubmitting.value) return
        val ready = uiState.value as? CheckoutUiState.Ready ?: return
        isSubmitting.value = true
        viewModelScope.launch {
            val effect = try {
                startPurchase(StartPurchaseCommand(idempotencyKey, eventId, ready.quantity, ready.paymentMethod)).toEffect()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CheckoutEffect.ShowMessage(CheckoutMessage.UnexpectedError)
            } finally {
                isSubmitting.value = false
            }
            effectChannel.send(effect)
        }
    }

    private fun StartPurchaseResult.toEffect(): CheckoutEffect = when (this) {
        is StartPurchaseResult.AwaitingPayment -> CheckoutEffect.ShowReceipt(purchaseId)
        is StartPurchaseResult.AlreadyProcessed -> CheckoutEffect.ShowReceipt(purchase.id)
        is StartPurchaseResult.TerminalUnavailable -> CheckoutEffect.ShowReceipt(purchaseId)
        is StartPurchaseResult.AnotherPaymentInProgress ->
            CheckoutEffect.ShowMessage(CheckoutMessage.AnotherPaymentInProgress(purchaseId))
        StartPurchaseResult.SoldOut -> CheckoutEffect.ShowMessage(CheckoutMessage.SoldOut)
        StartPurchaseResult.EventNotFound -> CheckoutEffect.ShowMessage(CheckoutMessage.EventUnavailable)
        is StartPurchaseResult.InvalidQuantity -> CheckoutEffect.ShowMessage(CheckoutMessage.InvalidQuantity(min, max))
    }

    companion object {
        const val ARG_EVENT_ID = "eventId"
        const val ARG_QUANTITY = "quantity"
        private const val KEY_IDEMPOTENCY = "checkout.idempotencyKey"
        private const val KEY_QUANTITY = "checkout.quantity"
        private const val KEY_PAYMENT_METHOD = "checkout.paymentMethod"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
