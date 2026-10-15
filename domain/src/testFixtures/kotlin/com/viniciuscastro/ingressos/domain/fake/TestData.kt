package com.viniciuscastro.ingressos.domain.fake

import com.viniciuscastro.ingressos.domain.model.Event
import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.payment.PaymentReceipt
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import java.time.Instant

object TestData {

    val NOW: Instant = Instant.parse("2026-10-15T12:00:00Z")

    fun event(
        id: String = "evt-jazz",
        price: Money = Money(5_000),
        availableTickets: Int = 100,
        startsAt: Instant = Instant.parse("2026-11-14T23:00:00Z"),
    ) = Event(
        id = id,
        title = "Festival de Jazz",
        description = "Uma noite de jazz ao ar livre.",
        venue = "Auditório Ibirapuera",
        city = "São Paulo",
        startsAt = startsAt,
        ticketPrice = price,
        availableTickets = availableTickets,
    )

    fun pendingPurchase(
        id: String = "pur-1",
        event: Event = event(),
        quantity: Int = 2,
        paymentMethod: PaymentMethod? = null,
        createdAt: Instant = NOW,
    ) = Purchase.start(id = id, event = event, quantity = quantity, paymentMethod = paymentMethod, now = createdAt)

    fun receipt(paid: Money) = PaymentReceipt(
        terminalOrderId = "order-123",
        paidAmount = paid,
        authorizationCode = "140126",
        nsu = "799871",
        cardBrand = "Visa",
        cardMask = "424242-4242",
        productName = "CREDITO A VISTA",
        installments = 0,
        terminalId = "69000007",
    )

    fun approved(reference: String?, paid: Money) = PaymentResult.Approved(reference, receipt(paid))

    fun canceledByUser() = PaymentResult.Canceled(reference = null, code = 1, message = "CANCELADO PELO USUÁRIO")

    fun declined() = PaymentResult.Declined(reference = null, code = 3, message = "SALDO INSUFICIENTE")
}
