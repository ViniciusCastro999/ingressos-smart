package com.viniciuscastro.ingressos.domain.usecase

import com.viniciuscastro.ingressos.domain.fake.FakePaymentTerminal
import com.viniciuscastro.ingressos.domain.fake.FixedClock
import com.viniciuscastro.ingressos.domain.fake.InMemoryEventRepository
import com.viniciuscastro.ingressos.domain.fake.InMemoryPurchaseRepository
import com.viniciuscastro.ingressos.domain.fake.RecordingTelemetry
import com.viniciuscastro.ingressos.domain.fake.TestData
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.StatusReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleasePendingPurchaseUseCaseTest {

    private val event = TestData.event()
    private val purchases = InMemoryPurchaseRepository(listOf(TestData.pendingPurchase(id = "pur-1", event = event)))
    private val telemetry = RecordingTelemetry()
    private val release = ReleasePendingPurchaseUseCase(purchases, FixedClock(), telemetry)

    @Test
    fun `releasing a pending purchase marks it unconfirmed and unblocks new checkouts`() = runTest {
        val startPurchase = StartPurchaseUseCase(
            InMemoryEventRepository(event), purchases, FakePaymentTerminal(), FixedClock(), telemetry,
        )

        assertTrue(release("pur-1"))

        val released = purchases.stored("pur-1")
        assertEquals(PurchaseStatus.UNCONFIRMED, released.status)
        assertEquals(StatusReason.NoTerminalResponse, released.reason)
        assertEquals(
            StartPurchaseResult.AwaitingPayment("pur-2"),
            startPurchase(StartPurchaseCommand("pur-2", event.id, 1, null)),
        )
    }

    @Test
    fun `only pending purchases can be released`() = runTest {
        release("pur-1")

        assertFalse(release("pur-1"))
        assertFalse(release("unknown"))
    }
}
