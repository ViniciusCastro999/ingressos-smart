package com.viniciuscastro.ingressos.domain.payment

/**
 * Output port to the payment terminal. The terminal answers asynchronously (Cielo Smart replies
 * through a deep link callback), so this call only *dispatches* the payment; the outcome is
 * delivered later to [PaymentResultParser].
 */
interface PaymentTerminal {
    suspend fun requestPayment(request: PaymentRequest): DispatchResult
}

sealed interface DispatchResult {
    data object Dispatched : DispatchResult
    data class Unavailable(val cause: String) : DispatchResult
}

/** Translates the raw callback delivered by the terminal into a [PaymentResult]. Never throws. */
fun interface PaymentResultParser {
    fun parse(callback: String): PaymentResult
}
