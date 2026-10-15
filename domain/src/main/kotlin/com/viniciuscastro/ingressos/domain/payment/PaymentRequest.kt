package com.viniciuscastro.ingressos.domain.payment

import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PaymentMethod

/**
 * What the terminal must charge. [reference] is the purchase id: the terminal echoes it back,
 * which is how a callback is matched to the purchase that originated it.
 */
data class PaymentRequest(
    val reference: String,
    val amount: Money,
    val method: PaymentMethod?,
    val items: List<PaymentItem>,
)

data class PaymentItem(
    val sku: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Money,
)
