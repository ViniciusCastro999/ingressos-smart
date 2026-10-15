package com.viniciuscastro.ingressos.data.repository

import com.viniciuscastro.ingressos.data.catalog.EventCatalog
import com.viniciuscastro.ingressos.data.catalog.EventDto
import com.viniciuscastro.ingressos.data.local.EventDao
import com.viniciuscastro.ingressos.data.local.EventEntity
import com.viniciuscastro.ingressos.data.local.toDomain
import com.viniciuscastro.ingressos.domain.model.Event
import com.viniciuscastro.ingressos.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.OffsetDateTime

/** Room is the single source of truth; the catalog only seeds it on first use. */
class RoomEventRepository(
    private val dao: EventDao,
    private val catalog: EventCatalog,
) : EventRepository {

    private val seedLock = Mutex()

    @Volatile
    private var seeded = false

    override fun observeEvents(): Flow<List<Event>> =
        dao.observeAll()
            .onStart { seedIfEmpty() }
            .map { events -> events.map(EventEntity::toDomain) }
            .distinctUntilChanged()

    override fun observeEvent(id: String): Flow<Event?> =
        dao.observe(id)
            .onStart { seedIfEmpty() }
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun findEvent(id: String): Event? {
        seedIfEmpty()
        return dao.find(id)?.toDomain()
    }

    override suspend fun decreaseAvailability(eventId: String, quantity: Int) =
        dao.decreaseAvailability(eventId, quantity)

    private suspend fun seedIfEmpty() {
        if (seeded) return
        seedLock.withLock {
            if (!seeded && dao.count() == 0) {
                dao.insertAll(catalog.fetchEvents().map { it.toEntity() })
            }
            seeded = true
        }
    }
}

private fun EventDto.toEntity() = EventEntity(
    id = id,
    title = title,
    description = description,
    venue = venue,
    city = city,
    startsAtEpochMillis = OffsetDateTime.parse(startsAt).toInstant().toEpochMilli(),
    priceCents = priceCents,
    availableTickets = availableTickets,
)
