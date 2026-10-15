package com.viniciuscastro.ingressos.payment.cielo.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of `lio://payment` (docs: Integração via Deep Link > Pagamento). Amounts in cents. */
@Serializable
data class CieloPaymentRequest(
    val accessToken: String,
    @SerialName("clientID") val clientId: String,
    val reference: String,
    val merchantCode: String? = null,
    val email: String? = null,
    val installments: Int = 0,
    val items: List<CieloItem>,
    val paymentCode: String? = null,
    /** Total in cents, as a string ("1000" = R$ 10,00). */
    val value: String,
) {
    override fun toString(): String = "CieloPaymentRequest(reference=$reference, value=$value, items=$items)"
}

@Serializable
data class CieloItem(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitOfMeasure: String,
    val unitPrice: Long,
)
