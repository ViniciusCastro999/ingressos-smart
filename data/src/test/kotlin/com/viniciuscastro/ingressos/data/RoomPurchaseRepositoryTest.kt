package com.viniciuscastro.ingressos.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.viniciuscastro.ingressos.data.catalog.EventCatalog
import com.viniciuscastro.ingressos.data.catalog.EventDto
import com.viniciuscastro.ingressos.data.local.IngressosDatabase
import com.viniciuscastro.ingressos.data.repository.RoomEventRepository
import com.viniciuscastro.ingressos.data.repository.RoomPurchaseRepository
import com.viniciuscastro.ingressos.data.repository.RoomTransactionRunner
import com.viniciuscastro.ingressos.domain.fake.FixedClock
import com.viniciuscastro.ingressos.domain.fake.RecordingTelemetry
import com.viniciuscastro.ingressos.domain.fake.SequentialIdGenerator
import com.viniciuscastro.ingressos.domain.fake.TestData
import com.viniciuscastro.ingressos.domain.model.Money
import com.viniciuscastro.ingressos.domain.model.PaymentMethod
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.StatusReason
import com.viniciuscastro.ingressos.domain.usecase.ProcessPaymentResult
import com.viniciuscastro.ingressos.domain.usecase.ProcessPaymentResultUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomPurchaseRepositoryTest {

    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        IngressosDatabase::class.java,
    ).allowMainThreadQueries().build()

    private val purchases = RoomPurchaseRepository(database.purchaseDao())
    private val catalog = object : EventCatalog {
        override suspend fun fetchEvents() = listOf(
            EventDto("evt-jazz", "Festival de Jazz", "", "Auditório", "São Paulo", "2026-11-14T20:00:00-03:00", 5_000, 10),
        )
    }
    private val events = RoomEventRepository(database.eventDao(), catalog)
    private val pending = TestData.pendingPurchase(id = "pur-1", quantity = 2, paymentMethod = PaymentMethod.PIX)

    @After
    fun tearDown() = database.close()

    @Test
    fun `the idempotency key is unique at database level`() = runTest {
        purchases.create(pending)

        assertFailsWith<SQLiteConstraintException> { purchases.create(pending) }
    }

    @Test
    fun `update is a compare-and-set on the stored status`() = runTest {
        purchases.create(pending)
        val canceled = pending.resolve(TestData.canceledByUser(), TestData.NOW) { "t" }.purchase

        assertTrue(purchases.update(canceled, expectedStatus = PurchaseStatus.PENDING))
        assertFalse(purchases.update(pending, expectedStatus = PurchaseStatus.PENDING))
        assertEquals(PurchaseStatus.CANCELED, purchases.find("pur-1")?.status)
    }

    @Test
    fun `approved purchase round-trips with receipt and tickets`() = runTest {
        purchases.create(pending)
        val ids = SequentialIdGenerator("ticket")
        val approved = pending.resolve(TestData.approved("pur-1", Money(10_000)), TestData.NOW, ids::newId).purchase

        purchases.update(approved, expectedStatus = PurchaseStatus.PENDING)

        assertEquals(approved, purchases.find("pur-1"))
        assertEquals(approved, purchases.observe("pur-1").first())
    }

    @Test
    fun `typed status reasons survive persistence`() = runTest {
        purchases.create(pending)
        val released = pending.markUnconfirmed(TestData.NOW)

        purchases.update(released, expectedStatus = PurchaseStatus.PENDING)

        assertEquals(StatusReason.NoTerminalResponse, purchases.find("pur-1")?.reason)
    }

    @Test
    fun `the awaiting purchase is the latest pending one`() = runTest {
        purchases.create(pending)
        assertEquals("pur-1", purchases.findAwaitingTerminal()?.id)

        purchases.update(pending.markUnconfirmed(TestData.NOW), PurchaseStatus.PENDING)

        assertEquals(null, purchases.findAwaitingTerminal())
        assertEquals(null, purchases.observeAwaitingTerminal().first())
    }

    @Test
    fun `duplicated approval callback issues tickets and consumes stock once on the real database`() = runTest {
        val event = events.findEvent("evt-jazz")!!
        purchases.create(TestData.pendingPurchase(id = "pur-1", event = event, quantity = 2))
        val process = ProcessPaymentResultUseCase(
            purchases = purchases,
            events = events,
            transaction = RoomTransactionRunner(database),
            idGenerator = SequentialIdGenerator("ticket"),
            clock = FixedClock(),
            telemetry = RecordingTelemetry(),
        )

        val first = process(TestData.approved("pur-1", Money(10_000)))
        val second = process(TestData.approved("pur-1", Money(10_000)))

        assertIs<ProcessPaymentResult.Applied>(first)
        assertIs<ProcessPaymentResult.AlreadyProcessed>(second)
        assertEquals(2, purchases.find("pur-1")?.tickets?.size)
        assertEquals(8, events.findEvent("evt-jazz")?.availableTickets)
    }
}
