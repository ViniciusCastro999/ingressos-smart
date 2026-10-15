package com.viniciuscastro.ingressos.data.local

import com.viniciuscastro.ingressos.domain.model.Event
import com.viniciuscastro.ingressos.domain.model.EventSnapshot
import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.StatusReason
import com.viniciuscastro.ingressos.domain.model.Ticket
import com.viniciuscastro.ingressos.domain.payment.PaymentReceipt
import java.time.Instant

internal fun EventEntity.toDomain() = Event(
    id = id,
    title = title,
    description = description,
    venue = venue,
    city = city,
    startsAt = Instant.ofEpochMilli(startsAtEpochMillis),
    ticketPrice = Money(priceCents),
    availableTickets = availableTickets,
)

internal fun Purchase.toEntity(): PurchaseEntity {
    val (reasonType, reasonCode, reasonMessage) = reason.toColumns()
    return PurchaseEntity(
        id = id,
        eventId = event.id,
        eventTitle = event.title,
        eventVenue = event.venue,
        eventStartsAtEpochMillis = event.startsAt.toEpochMilli(),
        quantity = quantity,
        unitPriceCents = unitPrice.cents,
        paymentMethod = paymentMethod?.name,
        status = status.name,
        createdAtEpochMillis = createdAt.toEpochMilli(),
        updatedAtEpochMillis = updatedAt.toEpochMilli(),
        reasonType = reasonType,
        reasonCode = reasonCode,
        reasonMessage = reasonMessage,
        receipt = receipt?.toEntity(),
    )
}

internal fun Ticket.toEntity() = TicketEntity(id = id, purchaseId = purchaseId, eventId = eventId, number = number)

internal fun PurchaseWithTickets.toDomain(): Purchase = with(purchase) {
    Purchase(
        id = id,
        event = EventSnapshot(
            id = eventId,
            title = eventTitle,
            venue = eventVenue,
            startsAt = Instant.ofEpochMilli(eventStartsAtEpochMillis),
        ),
        quantity = quantity,
        unitPrice = Money(unitPriceCents),
        paymentMethod = paymentMethod?.let(PaymentMethod::valueOf),
        status = PurchaseStatus.valueOf(status),
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
        receipt = receipt?.toDomain(),
        reason = toStatusReason(),
        tickets = tickets.sortedBy(TicketEntity::number).map { it.toDomain() },
    )
}

private fun TicketEntity.toDomain() = Ticket(id = id, purchaseId = purchaseId, eventId = eventId, number = number)

private fun PaymentReceipt.toEntity() = ReceiptEntity(
    terminalOrderId = terminalOrderId,
    paidAmountCents = paidAmount.cents,
    authorizationCode = authorizationCode,
    nsu = nsu,
    cardBrand = cardBrand,
    cardMask = cardMask,
    productName = productName,
    installments = installments,
    terminalId = terminalId,
)

private fun ReceiptEntity.toDomain(): PaymentReceipt? = paidAmountCents?.let { paid ->
    PaymentReceipt(
        terminalOrderId = terminalOrderId,
        paidAmount = Money(paid),
        authorizationCode = authorizationCode,
        nsu = nsu,
        cardBrand = cardBrand,
        cardMask = cardMask,
        productName = productName,
        installments = installments ?: 0,
        terminalId = terminalId,
    )
}

private object ReasonType {
    const val TERMINAL = "TERMINAL"
    const val AMOUNT_MISMATCH = "AMOUNT_MISMATCH"
    const val NO_TERMINAL_RESPONSE = "NO_TERMINAL_RESPONSE"
    const val TERMINAL_UNAVAILABLE = "TERMINAL_UNAVAILABLE"
}

private fun StatusReason?.toColumns(): Triple<String?, Int?, String?> = when (this) {
    null -> Triple(null, null, null)
    is StatusReason.Terminal -> Triple(ReasonType.TERMINAL, code, message)
    StatusReason.AmountMismatch -> Triple(ReasonType.AMOUNT_MISMATCH, null, null)
    StatusReason.NoTerminalResponse -> Triple(ReasonType.NO_TERMINAL_RESPONSE, null, null)
    StatusReason.TerminalUnavailable -> Triple(ReasonType.TERMINAL_UNAVAILABLE, null, null)
}

private fun PurchaseEntity.toStatusReason(): StatusReason? = when (reasonType) {
    ReasonType.TERMINAL -> StatusReason.Terminal(reasonCode, reasonMessage.orEmpty())
    ReasonType.AMOUNT_MISMATCH -> StatusReason.AmountMismatch
    ReasonType.NO_TERMINAL_RESPONSE -> StatusReason.NoTerminalResponse
    ReasonType.TERMINAL_UNAVAILABLE -> StatusReason.TerminalUnavailable
    else -> null
}
