package com.viniciuscastro.ingressos.domain.fake

import com.viniciuscastro.ingressos.domain.model.Event
import com.viniciuscastro.ingressos.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryEventRepository(vararg events: Event) : EventRepository {

    private val state = MutableStateFlow(events.associateBy { it.id })

    fun stored(id: String): Event = checkNotNull(state.value[id]) { "No event $id" }

    override fun observeEvents(): Flow<List<Event>> = state.map { it.values.sortedBy(Event::startsAt) }

    override fun observeEvent(id: String): Flow<Event?> = state.map { it[id] }

    override suspend fun findEvent(id: String): Event? = state.value[id]

    override suspend fun decreaseAvailability(eventId: String, quantity: Int) {
        state.update { events ->
            val event = events.getValue(eventId)
            events + (eventId to event.copy(availableTickets = (event.availableTickets - quantity).coerceAtLeast(0)))
        }
    }
}
