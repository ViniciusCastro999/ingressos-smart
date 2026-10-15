package com.viniciuscastro.ingressos.domain.model

import com.viniciuscastro.ingressos.domain.fake.SequentialIdGenerator
import com.viniciuscastro.ingressos.domain.fake.TestData
import com.viniciuscastro.ingressos.domain.payment.PaymentResult
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PurchaseTest {

    private val later = TestData.NOW.plusSeconds(30)
    private val ids = SequentialIdGenerator("ticket")
    private val pending = TestData.pendingPurchase(quantity = 3) // 3 x R$ 50,00

    private fun Purchase.apply(result: PaymentResult, at: Instant = later): Resolution =
        resolve(result, at, ids::newId)

    @Test
    fun `approval for the exact total approves and issues one ticket per unit`() {
        val resolution = pending.apply(TestData.approved(pending.id, Money(15_000)))

        val approved = assertIs<Resolution.Applied>(resolution).purchase
        assertEquals(PurchaseStatus.APPROVED, approved.status)
        assertEquals(listOf(1, 2, 3), approved.tickets.map(Ticket::number))
        assertTrue(approved.tickets.all { it.purchaseId == pending.id && it.eventId == pending.event.id })
        assertEquals("799871", approved.receipt?.nsu)
        assertEquals(later, approved.updatedAt)
        assertNull(approved.reason)
    }

    @Test
    fun `approval for a different amount is held as unconfirmed without tickets`() {
        val resolution = pending.apply(TestData.approved(pending.id, Money(100)))

        val held = assertIs<Resolution.Applied>(resolution).purchase
        assertEquals(PurchaseStatus.UNCONFIRMED, held.status)
        assertEquals(StatusReason.AmountMismatch, held.reason)
        assertTrue(held.tickets.isEmpty())
    }

    @Test
    fun `terminal errors are recorded with the terminal reason`() {
        val canceled = assertIs<Resolution.Applied>(pending.apply(TestData.canceledByUser())).purchase
        val declined = assertIs<Resolution.Applied>(pending.apply(TestData.declined())).purchase
        val failed = assertIs<Resolution.Applied>(pending.apply(PaymentResult.Failed(null, 4, "ERRO DE AUTENTICAÇÃO"))).purchase

        assertEquals(PurchaseStatus.CANCELED, canceled.status)
        assertEquals(StatusReason.Terminal(1, "CANCELADO PELO USUÁRIO"), canceled.reason)
        assertEquals(PurchaseStatus.DECLINED, declined.status)
        assertEquals(PurchaseStatus.FAILED, failed.status)
        assertTrue(listOf(canceled, declined, failed).all { it.tickets.isEmpty() })
    }

    @Test
    fun `an approved purchase ignores every later result, including a duplicated approval`() {
        val approved = pending.apply(TestData.approved(pending.id, Money(15_000))).purchase

        val duplicated = approved.apply(TestData.approved(pending.id, Money(15_000)))
        val canceled = approved.apply(TestData.canceledByUser())

        assertSame(approved, assertIs<Resolution.Ignored>(duplicated).purchase)
        assertSame(approved, assertIs<Resolution.Ignored>(canceled).purchase)
    }

    @Test
    fun `a final non-approved status is not overwritten by another error`() {
        val canceled = pending.apply(TestData.canceledByUser()).purchase

        val resolution = canceled.apply(TestData.declined())

        assertSame(canceled, assertIs<Resolution.Ignored>(resolution).purchase)
    }

    @Test
    fun `a late approval overrides a failed purchase because money was captured`() {
        val failed = pending.apply(PaymentResult.Failed(null, null, "Resposta ilegível")).purchase

        val resolution = failed.apply(TestData.approved(pending.id, Money(15_000)))

        assertEquals(PurchaseStatus.APPROVED, assertIs<Resolution.Applied>(resolution).purchase.status)
    }

    @Test
    fun `a released purchase can still be resolved by a late callback`() {
        val released = pending.markUnconfirmed(later)

        val resolution = released.apply(TestData.approved(pending.id, Money(15_000)))

        assertEquals(StatusReason.NoTerminalResponse, released.reason)
        assertEquals(PurchaseStatus.APPROVED, assertIs<Resolution.Applied>(resolution).purchase.status)
    }

    @Test
    fun `only a pending purchase can be released or fail to dispatch`() {
        val canceled = pending.apply(TestData.canceledByUser()).purchase

        assertFailsWith<IllegalStateException> { canceled.markUnconfirmed(later) }
        assertFailsWith<IllegalStateException> { canceled.markTerminalUnavailable(later) }
    }

    @Test
    fun `payment request carries the purchase id as reference and the total in cents`() {
        val request = pending.toPaymentRequest()

        assertEquals(pending.id, request.reference)
        assertEquals(Money(15_000), request.amount)
        assertEquals(3, request.items.single().quantity)
        assertEquals(pending.event.id, request.items.single().sku)
    }
}
