package com.viniciuscastro.ingressos.payment.cielo

import com.viniciuscastro.ingressos.domain.fake.TestData
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CieloDeepLinkFactoryTest {

    private val config = CieloConfig(
        clientId = "client-id",
        accessToken = "access-token",
        callbackUri = "ingressos://payment-result",
    )
    private val factory = CieloDeepLinkFactory(config)
    private val request = TestData.pendingPurchase(id = "pur-123", quantity = 3, paymentMethod = PaymentMethod.DEBIT)
        .toPaymentRequest() // 3 x R$ 50,00

    private fun requestJson(uri: String) = CieloProtocol.json.parseToJsonElement(
        CieloProtocol.decodeBase64(CieloProtocol.queryParameter(uri, "request")!!),
    ).jsonObject

    @Test
    fun `targets the lio payment action and the configured callback`() {
        val uri = factory.payment(request)

        assertTrue(uri.startsWith("lio://payment?request="))
        assertEquals("ingressos://payment-result", CieloProtocol.queryParameter(uri, "urlCallback"))
    }

    @Test
    fun `request body follows the Cielo payment contract with amounts in cents`() {
        val body = requestJson(factory.payment(request))

        assertEquals("access-token", body["accessToken"]!!.jsonPrimitive.content)
        assertEquals("client-id", body["clientID"]!!.jsonPrimitive.content)
        assertEquals("pur-123", body["reference"]!!.jsonPrimitive.content)
        assertEquals("15000", body["value"]!!.jsonPrimitive.content)
        assertTrue(body["value"]!!.jsonPrimitive.isString)
        assertEquals("DEBITO_AVISTA", body["paymentCode"]!!.jsonPrimitive.content)
        assertEquals(0, body["installments"]!!.jsonPrimitive.int)
        val item = body["items"]!!.jsonArray.single().jsonObject
        assertEquals(3, item["quantity"]!!.jsonPrimitive.int)
        assertEquals(5_000, item["unitPrice"]!!.jsonPrimitive.long)
        assertEquals("unidade", item["unitOfMeasure"]!!.jsonPrimitive.content)
    }

    @Test
    fun `optional fields are omitted so the operator chooses the method on the terminal`() {
        val body = requestJson(factory.payment(request.copy(method = null)))

        assertFalse("paymentCode" in body)
        assertFalse("merchantCode" in body)
        assertFalse("email" in body)
    }

    @Test
    fun `query values are percent-encoded so Base64 survives URI parsing`() {
        val query = factory.payment(request).substringAfter('?')
        val requestValue = query.substringAfter("request=").substringBefore('&')

        assertFalse(requestValue.any { it == '+' || it == '/' || it == '=' })
    }

    @Test
    fun `credentials never appear in toString`() {
        assertFalse("access-token" in config.toString())
    }
}
