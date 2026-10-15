package com.viniciuscastro.ingressos.domain.usecase

import com.viniciuscastro.ingressos.domain.common.Clock
import com.viniciuscastro.ingressos.domain.common.Telemetry
import com.viniciuscastro.ingressos.domain.common.TelemetryEvent
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.QuantityCheck
import com.viniciuscastro.ingressos.domain.payment.DispatchResult
import com.viniciuscastro.ingressos.domain.payment.PaymentTerminal
import com.viniciuscastro.ingressos.domain.repository.EventRepository
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StartPurchaseCommand(
    /** Generated once per checkout. Re-sending the same key never charges twice. */
    val idempotencyKey: String,
    val eventId: String,
    val quantity: Int,
    val paymentMethod: PaymentMethod?,
)

sealed interface StartPurchaseResult {
    /** The terminal has the payment; its callback will resolve the purchase. */
    data class AwaitingPayment(val purchaseId: String) : StartPurchaseResult

    /** The key was already used and the purchase has an outcome: it is returned, nothing is charged. */
    data class AlreadyProcessed(val purchase: Purchase) : StartPurchaseResult

    /** Another checkout is waiting for the terminal. The terminal handles one payment at a time. */
    data class AnotherPaymentInProgress(val purchaseId: String) : StartPurchaseResult

    /** The payment app is missing; the purchase was recorded as failed without any charge. */
    data class TerminalUnavailable(val purchaseId: String) : StartPurchaseResult

    data object EventNotFound : StartPurchaseResult
    data object SoldOut : StartPurchaseResult
    data class InvalidQuantity(val min: Int, val max: Int) : StartPurchaseResult
}

/**
 * Starts the payment of a checkout, guaranteeing at most one charge per idempotency key:
 * 1. single-flight lock: "check + create + dispatch" never interleaves (double tap, retries);
 * 2. replay: a known key returns the existing purchase instead of dispatching again;
 * 3. persist-before-dispatch: the purchase exists before the terminal is called, so its callback
 *    can always be matched, even after process death.
 *
 * Must be a singleton: the lock only protects callers that share the instance.
 */
class StartPurchaseUseCase(
    private val events: EventRepository,
    private val purchases: PurchaseRepository,
    private val terminal: PaymentTerminal,
    private val clock: Clock,
    private val telemetry: Telemetry,
) {
    private val singleFlight = Mutex()

    suspend operator fun invoke(command: StartPurchaseCommand): StartPurchaseResult =
        singleFlight.withLock { startOrReplay(command) }

    private suspend fun startOrReplay(command: StartPurchaseCommand): StartPurchaseResult {
        purchases.find(command.idempotencyKey)?.let { existing -> return replay(existing) }

        purchases.findAwaitingTerminal()?.let { pending ->
            telemetry.track(TelemetryEvent.ConcurrentPaymentBlocked(pending.id))
            return StartPurchaseResult.AnotherPaymentInProgress(pending.id)
        }

        val event = events.findEvent(command.eventId) ?: return StartPurchaseResult.EventNotFound
        when (val check = event.checkQuantity(command.quantity)) {
            QuantityCheck.SoldOut -> return StartPurchaseResult.SoldOut
            is QuantityCheck.OutOfRange -> return StartPurchaseResult.InvalidQuantity(check.min, check.max)
            QuantityCheck.Valid -> Unit
        }

        val purchase = Purchase.start(
            id = command.idempotencyKey,
            event = event,
            quantity = command.quantity,
            paymentMethod = command.paymentMethod,
            now = clock.now(),
        )
        purchases.create(purchase)
        return dispatch(purchase)
    }

    private fun replay(existing: Purchase): StartPurchaseResult {
        telemetry.track(TelemetryEvent.DuplicateAttemptBlocked(existing.id, existing.status))
        return if (existing.status == PurchaseStatus.PENDING) {
            StartPurchaseResult.AwaitingPayment(existing.id)
        } else {
            StartPurchaseResult.AlreadyProcessed(existing)
        }
    }

    private suspend fun dispatch(purchase: Purchase): StartPurchaseResult =
        when (val dispatch = terminal.requestPayment(purchase.toPaymentRequest())) {
            DispatchResult.Dispatched -> {
                telemetry.track(TelemetryEvent.PaymentDispatched(purchase.id, purchase.total.cents))
                StartPurchaseResult.AwaitingPayment(purchase.id)
            }
            is DispatchResult.Unavailable -> {
                purchases.update(purchase.markTerminalUnavailable(clock.now()), expectedStatus = PurchaseStatus.PENDING)
                telemetry.track(TelemetryEvent.PaymentDispatchFailed(purchase.id, dispatch.cause))
                StartPurchaseResult.TerminalUnavailable(purchase.id)
            }
        }
}
