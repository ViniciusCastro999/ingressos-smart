package com.viniciuscastro.ingressos.payment.cielo

import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.payment.PaymentReceipt
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import com.viniciuscastro.ingressos.domain.payment.PaymentResultParser
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloError
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloOrder
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloPayment
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Translates the Cielo Smart callback (`<callback>?response=<base64 json>&responsecode=<n>`) into a
 * domain [PaymentResult]. `responsecode` is ignored: Cielo returns `0` for errors as well, so the
 * JSON shape (order vs. `{code, reason}`) is what tells them apart.
 *
 * Never throws: an unreadable callback becomes [PaymentResult.Failed] without reference, which only
 * resolves the purchase awaiting the terminal; a later valid approval still wins (see Purchase).
 */
class CieloCallbackParser : PaymentResultParser {

    override fun parse(callback: String): PaymentResult = try {
        val encoded = CieloProtocol.queryParameter(callback, CieloProtocol.RESPONSE_PARAM)
        if (encoded.isNullOrBlank()) {
            unreadable("Callback sem o parâmetro 'response'")
        } else {
            toResult(CieloProtocol.json.parseToJsonElement(CieloProtocol.decodeBase64(encoded)).jsonObject)
        }
    } catch (e: IllegalArgumentException) { // invalid Base64, malformed JSON (SerializationException) or not an object
        unreadable("Resposta do terminal ilegível")
    }

    private fun toResult(payload: JsonObject): PaymentResult {
        val isError = "code" in payload && "payments" !in payload && "id" !in payload
        return if (isError) {
            CieloProtocol.json.decodeFromJsonElement(CieloError.serializer(), payload).toResult()
        } else {
            CieloProtocol.json.decodeFromJsonElement(CieloOrder.serializer(), payload).toResult()
        }
    }

    private fun CieloOrder.toResult(): PaymentResult {
        val reference = reference?.takeIf { it.isNotBlank() }
        val authorized = payments.filter(CieloPayment::isAuthorized)
        val payment = authorized.firstOrNull()
            ?: return PaymentResult.Failed(reference, code = null, message = "Pedido retornado sem pagamento autorizado")
        val paid = paidAmount?.takeIf { it > 0 } ?: authorized.sumOf { it.amount ?: 0L }
        return PaymentResult.Approved(
            reference = reference,
            receipt = PaymentReceipt(
                terminalOrderId = id,
                paidAmount = Money(paid),
                authorizationCode = payment.authCode,
                nsu = payment.cieloCode,
                cardBrand = payment.brand?.takeIf { it.isNotBlank() },
                cardMask = payment.mask,
                productName = payment.field("productName"),
                installments = payment.installments ?: 0,
                terminalId = payment.terminal,
            ),
        )
    }

    private fun CieloError.toResult(): PaymentResult {
        val message = reason?.takeIf { it.isNotBlank() }
        return when (CieloErrorCode.from(code)) {
            CieloErrorCode.CANCELED_BY_USER ->
                PaymentResult.Canceled(reference = null, code = code, message = message ?: "Cancelado pelo usuário")
            CieloErrorCode.PAYMENT_ERROR ->
                PaymentResult.Declined(reference = null, code = code, message = message ?: "Pagamento não autorizado")
            CieloErrorCode.AUTHENTICATION ->
                PaymentResult.Failed(reference = null, code = code, message = message ?: "Erro de autenticação com a Cielo")
            CieloErrorCode.GENERIC, null ->
                PaymentResult.Failed(reference = null, code = code, message = message ?: "Erro no terminal de pagamento")
        }
    }

    private fun unreadable(message: String) = PaymentResult.Failed(reference = null, code = null, message = message)
}

/** Error codes returned by the deep link integration (docs: Códigos de erro). */
internal enum class CieloErrorCode(val code: Int) {
    CANCELED_BY_USER(1),
    GENERIC(2),
    PAYMENT_ERROR(3),
    AUTHENTICATION(4),
    ;

    companion object {
        fun from(code: Int?): CieloErrorCode? = entries.firstOrNull { it.code == code }
    }
}
