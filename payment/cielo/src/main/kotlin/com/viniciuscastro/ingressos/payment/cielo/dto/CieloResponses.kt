package com.viniciuscastro.ingressos.payment.cielo.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Successful callback: the order with its payments (docs: Recuperando dados do pagamento). */
@Serializable
data class CieloOrder(
    val id: String? = null,
    val reference: String? = null,
    val status: String? = null,
    val price: Long? = null,
    val paidAmount: Long? = null,
    val pendingAmount: Long? = null,
    val payments: List<CieloPayment> = emptyList(),
)

@Serializable
data class CieloPayment(
    val id: String? = null,
    val externalId: String? = null,
    val amount: Long? = null,
    val authCode: String? = null,
    val cieloCode: String? = null,
    val brand: String? = null,
    val mask: String? = null,
    val installments: Int? = null,
    val terminal: String? = null,
    val paymentFields: JsonObject = JsonObject(emptyMap()),
) {
    fun field(name: String): String? = (paymentFields[name] as? JsonPrimitive)?.contentOrNull

    /** `statusCode`: 0 = Pix, 1 = authorized, 2 = canceled (reversal). */
    val isAuthorized: Boolean get() = field("statusCode").let { it == null || it == "0" || it == "1" }
}

/** Error callback: `{"code": 1, "reason": "CANCELADO PELO USUÁRIO"}` (docs: Códigos de erro). */
@Serializable
data class CieloError(
    val code: Int? = null,
    val reason: String? = null,
)
