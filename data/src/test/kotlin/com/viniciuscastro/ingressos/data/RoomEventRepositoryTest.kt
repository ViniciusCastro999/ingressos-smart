package com.viniciuscastro.ingressos.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.viniciuscastro.ingressos.data.catalog.AssetEventCatalog
import com.viniciuscastro.ingressos.data.local.IngressosDatabase
import com.viniciuscastro.ingressos.data.repository.RoomEventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomEventRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, IngressosDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = RoomEventRepository(database.eventDao(), AssetEventCatalog(context))

    @After
    fun tearDown() = database.close()

    @Test
    fun `bundled catalog seeds the database once, sorted by date`() = runTest {
        val events = repository.observeEvents().first()
        repository.observeEvents().first() // a second subscription must not duplicate the seed

        assertEquals(6, database.eventDao().count())
        assertEquals(events.sortedBy { it.startsAt }, events)
        val jazz = events.first { it.id == "evt-jazz-ibirapuera" }
        assertEquals(12_000, jazz.ticketPrice.cents)
        assertEquals(Instant.parse("2026-11-14T23:00:00Z"), jazz.startsAt)
    }

    @Test
    fun `stock never goes below zero`() = runTest {
        assertEquals(3, repository.findEvent("evt-rock-praca")?.availableTickets)

        repository.decreaseAvailability("evt-rock-praca", 10)

        assertTrue(repository.findEvent("evt-rock-praca")!!.isSoldOut)
        assertEquals(0, repository.findEvent("evt-rock-praca")!!.availableTickets)
    }
}
