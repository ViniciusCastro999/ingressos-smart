package com.viniciuscastro.ingressos.payment.cielo

import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.payment.PaymentRequest
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloItem
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloPaymentRequest

/**
 * Builds `lio://payment?request=<base64 json>&urlCallback=<callback>`. Values are percent-encoded,
 * like `Uri.Builder.appendQueryParameter` in Cielo's sample, so '+', '/' and '=' survive the trip.
 * Pure Kotlin on purpose: the wire format is unit-tested on the JVM.
 */
class CieloDeepLinkFactory(private val config: CieloConfig) {

    val terminalScheme: String get() = config.terminalScheme

    fun payment(request: PaymentRequest): String {
        val body = CieloProtocol.json.encodeToString(CieloPaymentRequest.serializer(), request.toCielo())
        return buildString {
            append(config.terminalScheme).append("://").append(CieloProtocol.PAYMENT_ACTION)
            append('?').append(CieloProtocol.REQUEST_PARAM).append('=')
            append(CieloProtocol.encodeQueryValue(CieloProtocol.encodeBase64(body)))
            append('&').append(CieloProtocol.CALLBACK_PARAM).append('=')
            append(CieloProtocol.encodeQueryValue(config.callbackUri))
        }
    }

    private fun PaymentRequest.toCielo() = CieloPaymentRequest(
        accessToken = config.accessToken,
        clientId = config.clientId,
        reference = reference,
        merchantCode = config.merchantCode,
        installments = 0,
        items = items.map { item ->
            CieloItem(
                name = item.name,
                quantity = item.quantity,
                sku = item.sku,
                unitOfMeasure = UNIT_OF_MEASURE,
                unitPrice = item.unitPrice.cents,
            )
        },
        paymentCode = method?.toPaymentCode(),
        value = amount.cents.toString(),
    )

    private companion object {
        const val UNIT_OF_MEASURE = "unidade"

        /** Values from "Valores aceitos no campo paymentCode". `null` lets the operator pick on the terminal. */
        fun PaymentMethod.toPaymentCode(): String = when (this) {
            PaymentMethod.CREDIT -> "CREDITO_AVISTA"
            PaymentMethod.DEBIT -> "DEBITO_AVISTA"
            PaymentMethod.PIX -> "PIX"
        }
    }
}
