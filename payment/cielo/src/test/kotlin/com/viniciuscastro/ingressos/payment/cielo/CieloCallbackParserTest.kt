package com.viniciuscastro.ingressos.payment.cielo

import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CieloCallbackParserTest {

    private val parser = CieloCallbackParser()

    private fun callback(json: String) = "ingressos://payment-result?response=" +
        CieloProtocol.encodeQueryValue(CieloProtocol.encodeBase64(json)) + "&responsecode=0"

    private fun resource(name: String): String = rawResource(name).trim()

    /** Exact bytes, trailing newline included: the official emulator ends its Base64 with one. */
    private fun rawResource(name: String): String =
        checkNotNull(javaClass.getResource("/cielo/$name")) { "Missing $name" }.readText()

    @Test
    fun `parses the approval sent by the official Cielo emulator 1_61_8`() {
        // Captured from ActivityTaskManager on an Android 10 AVD: Base64 wrapped every 76 chars with raw '\n',
        // not percent-encoded, and paidAmount = 0 although the order is PAID.
        val result = assertIs<PaymentResult.Approved>(parser.parse(rawResource("emulator-1.61.8-success.uri")))

        assertEquals("4cac1dca-7df0-4923-aef9-2d7929cbcf3c", result.reference)
        with(result.receipt) {
            assertEquals("7b5a82e1-00ce-4fb4-ad5f-47e2843bb079", terminalOrderId)
            assertEquals(Money(12000), paidAmount)
            assertEquals("bHYsyg", authorizationCode)
            assertEquals("NvOE9f", nsu)
            assertEquals("mock_terminal", terminalId)
            assertEquals(1, installments)
        }
    }

    @Test
    fun `parses the cancellation sent by the official Cielo emulator 1_61_8`() {
        assertEquals(
            PaymentResult.Canceled(null, 1, "CANCELADO PELO USUÁRIO"),
            parser.parse(rawResource("emulator-1.61.8-canceled.uri")),
        )
    }

    @Test
    fun `parses the error sent by the official Cielo emulator 1_61_8 without responsecode`() {
        // The "Erro" scenario omits the responsecode parameter entirely.
        assertEquals(
            PaymentResult.Failed(null, 2, "Falha no processo de pagamento"),
            parser.parse(rawResource("emulator-1.61.8-error.uri")),
        )
    }

    @Test
    fun `parses the success example published in the Cielo Smart docs`() {
        // Raw, not percent-encoded, exactly as printed in "Recuperando dados do pagamento".
        val uri = "order://response?response=${resource("payment-success-docs.b64")}&responsecode=0"

        val result = assertIs<PaymentResult.Approved>(parser.parse(uri))

        assertEquals("Reference", result.reference)
        with(result.receipt) {
            assertEquals("1db5577f-fcd0-49e8-8cac-5e24aafb251f", terminalOrderId)
            assertEquals(Money(925), paidAmount)
            assertEquals("113730", authorizationCode)
            assertEquals("847262", nsu)
            assertNull(cardBrand) // blank in the example
            assertEquals("************0000", cardMask)
            assertEquals("CREDITO A VISTA - I", productName)
            assertEquals("62000112", terminalId)
        }
    }

    @Test
    fun `parses the error example published in the Cielo Smart docs`() {
        val uri = "order://response?response=eyJjb2RlIjoxLCJyZWFzb24iOiJDQU5DRUxBRE8gUEVMTyBVU1XDgVJJTyJ9&responsecode=0"

        assertEquals(PaymentResult.Canceled(null, 1, "CANCELADO PELO USUÁRIO"), parser.parse(uri))
    }

    @Test
    fun `maps every documented error code`() {
        assertIs<PaymentResult.Canceled>(parser.parse(callback("""{"code":1,"reason":"x"}""")))
        assertIs<PaymentResult.Failed>(parser.parse(callback("""{"code":2,"reason":"x"}""")))
        assertEquals(
            PaymentResult.Declined(null, 3, "SALDO INSUFICIENTE"),
            parser.parse(callback("""{"code":3,"reason":"SALDO INSUFICIENTE"}""")),
        )
        assertEquals(
            PaymentResult.Failed(null, 4, "Erro de autenticação com a Cielo"),
            parser.parse(callback("""{"code":4}""")),
        )
        assertIs<PaymentResult.Failed>(parser.parse(callback("""{"code":99,"reason":"?"}""")))
    }

    @Test
    fun `tolerates line-wrapped Base64 and plus signs turned into spaces`() {
        val json = """{"id":"o-1","reference":"pur-1","paidAmount":1000,"payments":[{"amount":1000,"authCode":"a>?b"}]}"""
        val wrapped = CieloProtocol.encodeBase64(json).chunked(20).joinToString("\n")
        val spaced = CieloProtocol.encodeBase64(json).replace('+', ' ')

        val fromWrapped = parser.parse("cb://r?response=$wrapped&responsecode=0")
        val fromSpaced = parser.parse("cb://r?response=$spaced&responsecode=0")

        assertEquals(Money(1000), assertIs<PaymentResult.Approved>(fromWrapped).receipt.paidAmount)
        assertEquals("a>?b", assertIs<PaymentResult.Approved>(fromSpaced).receipt.authorizationCode)
    }

    @Test
    fun `an order whose payment was reversed is not an approval`() {
        val json = """{"id":"o-1","reference":"pur-1","payments":[{"amount":1000,"paymentFields":{"statusCode":"2"}}]}"""

        assertEquals(
            PaymentResult.Failed("pur-1", null, "Pedido retornado sem pagamento autorizado"),
            parser.parse(callback(json)),
        )
    }

    @Test
    fun `paid amount falls back to the sum of authorized payments`() {
        val json = """{"id":"o-1","reference":"pur-1","payments":[{"amount":600},{"amount":400,"paymentFields":{"statusCode":"0"}}]}"""

        assertEquals(Money(1000), assertIs<PaymentResult.Approved>(parser.parse(callback(json))).receipt.paidAmount)
    }

    @Test
    fun `malformed callbacks become failures instead of crashing`() {
        listOf(
            "ingressos://payment-result",
            "ingressos://payment-result?responsecode=0",
            "ingressos://payment-result?response=%%%",
            "ingressos://payment-result?response=bm90LWpzb24=",
            callback("[1,2,3]"),
        ).forEach { uri ->
            val result = assertIs<PaymentResult.Failed>(parser.parse(uri), uri)
            assertNull(result.reference)
        }
    }
}
