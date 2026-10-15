package com.viniciuscastro.ingressos.payment.simulator

import com.viniciuscastro.ingressos.payment.cielo.CieloProtocol
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloError
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloOrder
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloPayment
import com.viniciuscastro.ingressos.payment.cielo.dto.CieloPaymentRequest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.random.Random

data class SimulatorRequest(
    val payment: CieloPaymentRequest,
    val callbackUri: String,
) {
    val amountCents: Long get() = payment.value.toLong()
}

enum class SimulatedOutcome(val label: String) {
    APPROVE("Aprovar"),
    APPROVE_WRONG_AMOUNT("Aprovar com valor divergente"),
    DECLINE("Negar (saldo insuficiente)"),
    CANCEL("Cancelar (cliente desistiu)"),
    AUTHENTICATION_ERROR("Erro de autenticação"),
    GENERIC_ERROR("Erro genérico"),
}

/**
 * Pure protocol logic of the simulator: reads a `lio://payment`-style request and builds the callback
 * Cielo Smart would send, in the documented format (Base64 JSON, line-wrapped like Android's
 * `Base64.DEFAULT`). Kept free of Android types so it is unit-tested on the JVM.
 */
class TerminalSimulator(private val random: Random = Random.Default) {

    fun parseRequest(uri: String): SimulatorRequest? = runCatching {
        val body = CieloProtocol.decodeBase64(requireNotNull(CieloProtocol.queryParameter(uri, CieloProtocol.REQUEST_PARAM)))
        SimulatorRequest(
            payment = CieloProtocol.json.decodeFromString(CieloPaymentRequest.serializer(), body),
            callbackUri = requireNotNull(CieloProtocol.queryParameter(uri, CieloProtocol.CALLBACK_PARAM)),
        )
    }.getOrNull()

    fun respond(request: SimulatorRequest, outcome: SimulatedOutcome): String = when (outcome) {
        SimulatedOutcome.APPROVE -> approved(request, paid = request.amountCents)
        SimulatedOutcome.APPROVE_WRONG_AMOUNT -> approved(request, paid = (request.amountCents - 100).coerceAtLeast(1))
        SimulatedOutcome.DECLINE -> error(request, code = 3, reason = "SALDO INSUFICIENTE")
        SimulatedOutcome.CANCEL -> error(request, code = 1, reason = "CANCELADO PELO USUÁRIO")
        SimulatedOutcome.AUTHENTICATION_ERROR -> error(request, code = 4, reason = "ERRO DE AUTENTICAÇÃO")
        SimulatedOutcome.GENERIC_ERROR -> error(request, code = 2, reason = "ERRO GENÉRICO")
    }

    private fun approved(request: SimulatorRequest, paid: Long): String {
        val isPix = request.payment.paymentCode == "PIX"
        val order = CieloOrder(
            id = UUID.randomUUID().toString(),
            reference = request.payment.reference,
            status = "PAID",
            price = request.amountCents,
            paidAmount = paid,
            pendingAmount = 0,
            payments = listOf(
                CieloPayment(
                    id = UUID.randomUUID().toString(),
                    externalId = UUID.randomUUID().toString(),
                    amount = paid,
                    authCode = digits(6),
                    cieloCode = digits(6),
                    brand = if (isPix) "" else "Visa",
                    mask = if (isPix) "" else "424242-4242",
                    installments = 0,
                    terminal = "69000007",
                    paymentFields = buildJsonObject {
                        put("statusCode", if (isPix) "0" else "1")
                        put("productName", productName(request.payment.paymentCode))
                        put("merchantName", "LOJA SIMULADA")
                    },
                ),
            ),
        )
        return callback(request, CieloProtocol.json.encodeToString(CieloOrder.serializer(), order))
    }

    private fun error(request: SimulatorRequest, code: Int, reason: String): String =
        callback(request, CieloProtocol.json.encodeToString(CieloError.serializer(), CieloError(code, reason)))

    private fun callback(request: SimulatorRequest, json: String): String {
        val wrapped = CieloProtocol.encodeBase64(json).chunked(BASE64_LINE_LENGTH).joinToString("\n")
        return "${request.callbackUri}?${CieloProtocol.RESPONSE_PARAM}=${CieloProtocol.encodeQueryValue(wrapped)}" +
            "&${CieloProtocol.RESPONSE_CODE_PARAM}=0"
    }

    private fun digits(count: Int): String = (1..count).joinToString("") { random.nextInt(10).toString() }

    private fun productName(paymentCode: String?): String = when (paymentCode) {
        "DEBITO_AVISTA" -> "DEBITO A VISTA"
        "PIX" -> "PIX"
        else -> "CREDITO A VISTA"
    }

    private companion object {
        const val BASE64_LINE_LENGTH = 76
    }
}
