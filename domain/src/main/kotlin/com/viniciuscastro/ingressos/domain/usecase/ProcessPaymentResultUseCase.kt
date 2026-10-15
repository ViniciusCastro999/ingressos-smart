package com.viniciuscastro.ingressos.domain.usecase

import com.viniciuscastro.ingressos.domain.common.Clock
import com.viniciuscastro.ingressos.domain.common.IdGenerator
import com.viniciuscastro.ingressos.domain.common.Telemetry
import com.viniciuscastro.ingressos.domain.common.TelemetryEvent
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.Resolution
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import com.viniciuscastro.ingressos.domain.repository.EventRepository
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository
import com.viniciuscastro.ingressos.domain.repository.TransactionRunner

sealed interface ProcessPaymentResult {
    data class Applied(val purchase: Purchase) : ProcessPaymentResult

    /** Duplicate or late callback that does not change the purchase (idempotent no-op). */
    data class AlreadyProcessed(val purchase: Purchase) : ProcessPaymentResult

    /** No purchase matches the callback; it is ignored and nothing is issued. */
    data class Unmatched(val reference: String?) : ProcessPaymentResult
}

/**
 * Records the outcome reported by the terminal. Safe to call any number of times with the same
 * result: status, tickets and stock change atomically and only once.
 */
class ProcessPaymentResultUseCase(
    private val purchases: PurchaseRepository,
    private val events: EventRepository,
    private val transaction: TransactionRunner,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val telemetry: Telemetry,
) {
    suspend operator fun invoke(result: PaymentResult): ProcessPaymentResult {
        repeat(MAX_ATTEMPTS) {
            val current = findTarget(result) ?: return unmatched(result)
            when (val resolution = current.resolve(result, clock.now(), idGenerator::newId)) {
                is Resolution.Ignored -> {
                    telemetry.track(TelemetryEvent.CallbackIgnored(current.id, current.status))
                    return ProcessPaymentResult.AlreadyProcessed(current)
                }
                is Resolution.Applied -> if (persist(current, resolution.purchase)) {
                    trackApplied(previous = current, resolved = resolution.purchase)
                    return ProcessPaymentResult.Applied(resolution.purchase)
                }
            }
            // Lost the compare-and-set race (e.g. operator released it meanwhile): re-read and retry.
        }
        error("Purchase kept changing while processing the payment result")
    }

    /**
     * A result that carries a reference is matched strictly by it: falling back to "the pending
     * purchase" could let a stray approval resolve the wrong one. Only reference-less results
     * (Cielo error callbacks) fall back to the purchase awaiting the terminal.
     */
    private suspend fun findTarget(result: PaymentResult): Purchase? {
        val reference = result.reference?.takeIf { it.isNotBlank() }
        return if (reference != null) purchases.find(reference) else purchases.findAwaitingTerminal()
    }

    private suspend fun persist(current: Purchase, resolved: Purchase): Boolean = transaction.run {
        val updated = purchases.update(resolved, expectedStatus = current.status)
        if (updated && resolved.status == PurchaseStatus.APPROVED) {
            events.decreaseAvailability(resolved.event.id, resolved.quantity)
        }
        updated
    }

    private fun unmatched(result: PaymentResult): ProcessPaymentResult {
        telemetry.track(TelemetryEvent.CallbackUnmatched(result.reference))
        return ProcessPaymentResult.Unmatched(result.reference)
    }

    private fun trackApplied(previous: Purchase, resolved: Purchase) {
        val event = if (previous.status.isFinal) {
            TelemetryEvent.LateApprovalRecovered(resolved.id, previous.status)
        } else {
            TelemetryEvent.PaymentResolved(resolved.id, resolved.status)
        }
        telemetry.track(event)
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
