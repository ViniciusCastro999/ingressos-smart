package com.viniciuscastro.ingressos.payment.simulator

import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.payment.PaymentItem
import com.viniciuscastro.ingressos.domain.payment.PaymentRequest
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import com.viniciuscastro.ingressos.payment.cielo.CieloCallbackParser
import com.viniciuscastro.ingressos.payment.cielo.CieloConfig
import com.viniciuscastro.ingressos.payment.cielo.CieloDeepLinkFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Round trip through the real adapter: factory -> simulator -> parser. */
class TerminalSimulatorTest {

    private val factory = CieloDeepLinkFactory(
        CieloConfig("client", "token", callbackUri = "ingressos-sim://payment-result", terminalScheme = "lio-sim"),
    )
    private val simulator = TerminalSimulator()
    private val parser = CieloCallbackParser()
    private val request = PaymentRequest(
        reference = "pur-1",
        amount = Money(24_000),
        method = PaymentMethod.CREDIT,
        items = listOf(PaymentItem("evt", "Festival de Jazz", 2, Money(12_000))),
    )

    private fun answer(outcome: SimulatedOutcome): PaymentResult {
        val received = assertNotNull(simulator.parseRequest(factory.payment(request)))
        return parser.parse(simulator.respond(received, outcome))
    }

    @Test
    fun `reads the request produced by the Cielo adapter`() {
        val received = assertNotNull(simulator.parseRequest(factory.payment(request)))

        assertEquals("ingressos-sim://payment-result", received.callbackUri)
        assertEquals(24_000, received.amountCents)
        assertEquals("pur-1", received.payment.reference)
    }

    @Test
    fun `approval echoes the reference and the requested amount`() {
        val approved = assertIs<PaymentResult.Approved>(answer(SimulatedOutcome.APPROVE))

        assertEquals("pur-1", approved.reference)
        assertEquals(Money(24_000), approved.receipt.paidAmount)
    }

    @Test
    fun `divergent approval reports a different amount`() {
        val approved = assertIs<PaymentResult.Approved>(answer(SimulatedOutcome.APPROVE_WRONG_AMOUNT))

        assertEquals(Money(23_900), approved.receipt.paidAmount)
    }

    @Test
    fun `errors use the documented codes`() {
        assertIs<PaymentResult.Declined>(answer(SimulatedOutcome.DECLINE))
        assertIs<PaymentResult.Canceled>(answer(SimulatedOutcome.CANCEL))
        assertEquals(4, assertIs<PaymentResult.Failed>(answer(SimulatedOutcome.AUTHENTICATION_ERROR)).code)
        assertEquals(2, assertIs<PaymentResult.Failed>(answer(SimulatedOutcome.GENERIC_ERROR)).code)
    }

    @Test
    fun `invalid request is rejected`() {
        assertNull(simulator.parseRequest("lio-sim://payment?request=%%%"))
    }
}
