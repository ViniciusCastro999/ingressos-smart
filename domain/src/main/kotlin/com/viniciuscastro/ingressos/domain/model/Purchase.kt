package com.viniciuscastro.ingressos.domain.model

import com.viniciuscastro.ingressos.domain.payment.PaymentItem
import com.viniciuscastro.ingressos.domain.payment.PaymentReceipt
import com.viniciuscastro.ingressos.domain.payment.PaymentRequest
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import java.time.Instant

/**
 * A ticket purchase and its payment lifecycle. [id] doubles as the idempotency key of the checkout
 * and as the `reference` sent to the terminal.
 *
 * State machine (docs/specs/SPEC-003-idempotencia.md):
 * ```
 * PENDING ──► APPROVED | DECLINED | CANCELED | FAILED
 *    │                        ▲  (late approval wins: money was captured)
 *    └──► UNCONFIRMED ────────┘
 * ```
 */
data class Purchase(
    val id: String,
    val event: EventSnapshot,
    val quantity: Int,
    val unitPrice: Money,
    val paymentMethod: PaymentMethod?,
    val status: PurchaseStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val receipt: PaymentReceipt? = null,
    val reason: StatusReason? = null,
    val tickets: List<Ticket> = emptyList(),
) {
    val total: Money get() = unitPrice * quantity

    fun toPaymentRequest(): PaymentRequest = PaymentRequest(
        reference = id,
        amount = total,
        method = paymentMethod,
        items = listOf(
            PaymentItem(sku = event.id, name = event.title, quantity = quantity, unitPrice = unitPrice),
        ),
    )

    /**
     * Applies a terminal outcome. Re-applying any result is harmless (idempotent), and an approval is
     * never lost: it overrides a non-approved state because it carries proof that money was captured.
     */
    fun resolve(result: PaymentResult, now: Instant, newTicketId: () -> String): Resolution {
        val canApply = when {
            status == PurchaseStatus.APPROVED -> false
            status.isFinal -> result is PaymentResult.Approved
            else -> true
        }
        if (!canApply) return Resolution.Ignored(this)

        val resolved = when (result) {
            is PaymentResult.Approved -> approve(result.receipt, now, newTicketId)
            is PaymentResult.Declined -> reject(PurchaseStatus.DECLINED, result.code, result.message, now)
            is PaymentResult.Canceled -> reject(PurchaseStatus.CANCELED, result.code, result.message, now)
            is PaymentResult.Failed -> reject(PurchaseStatus.FAILED, result.code, result.message, now)
        }
        return Resolution.Applied(resolved)
    }

    /** The operator gave up waiting for the terminal. The purchase stops blocking new payments. */
    fun markUnconfirmed(now: Instant): Purchase {
        check(status == PurchaseStatus.PENDING) { "Only a pending purchase can be released (was $status)" }
        return copy(status = PurchaseStatus.UNCONFIRMED, reason = StatusReason.NoTerminalResponse, updatedAt = now)
    }

    /** The payment never reached the terminal, so nothing was charged. */
    fun markTerminalUnavailable(now: Instant): Purchase {
        check(status == PurchaseStatus.PENDING) { "Only a pending purchase can fail to dispatch (was $status)" }
        return copy(status = PurchaseStatus.FAILED, reason = StatusReason.TerminalUnavailable, updatedAt = now)
    }

    private fun approve(receipt: PaymentReceipt, now: Instant, newTicketId: () -> String): Purchase {
        // Never issue tickets for an amount we did not ask for: hold the purchase for a manual check.
        if (receipt.paidAmount != total) {
            return copy(
                status = PurchaseStatus.UNCONFIRMED,
                receipt = receipt,
                reason = StatusReason.AmountMismatch,
                updatedAt = now,
            )
        }
        return copy(
            status = PurchaseStatus.APPROVED,
            receipt = receipt,
            reason = null,
            updatedAt = now,
            tickets = (1..quantity).map { number ->
                Ticket(id = newTicketId(), purchaseId = id, eventId = event.id, number = number)
            },
        )
    }

    private fun reject(status: PurchaseStatus, code: Int?, message: String, now: Instant): Purchase =
        copy(status = status, reason = StatusReason.Terminal(code, message), updatedAt = now)

    companion object {
        fun start(
            id: String,
            event: Event,
            quantity: Int,
            paymentMethod: PaymentMethod?,
            now: Instant,
        ): Purchase = Purchase(
            id = id,
            event = event.snapshot(),
            quantity = quantity,
            unitPrice = event.ticketPrice,
            paymentMethod = paymentMethod,
            status = PurchaseStatus.PENDING,
            createdAt = now,
            updatedAt = now,
        )
    }
}

sealed interface Resolution {
    val purchase: Purchase

    data class Applied(override val purchase: Purchase) : Resolution
    data class Ignored(override val purchase: Purchase) : Resolution
}
