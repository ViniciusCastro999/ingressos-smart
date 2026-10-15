package com.viniciuscastro.ingressos.domain.payment

import com.viniciuscastro.ingressos.domain.model.Money

/** Outcome reported by the payment terminal, already translated from its wire format. */
sealed interface PaymentResult {

    /** Purchase id echoed by the terminal. Error callbacks do not carry it. */
    val reference: String?

    data class Approved(override val reference: String?, val receipt: PaymentReceipt) : PaymentResult

    data class Declined(override val reference: String?, val code: Int?, val message: String) : PaymentResult

    data class Canceled(override val reference: String?, val code: Int?, val message: String) : PaymentResult

    data class Failed(override val reference: String?, val code: Int?, val message: String) : PaymentResult
}

/** Proof of capture returned by the terminal. Kept on the purchase to build the receipt. */
data class PaymentReceipt(
    val terminalOrderId: String?,
    val paidAmount: Money,
    val authorizationCode: String?,
    val nsu: String?,
    val cardBrand: String?,
    val cardMask: String?,
    val productName: String?,
    val installments: Int,
    val terminalId: String?,
)
