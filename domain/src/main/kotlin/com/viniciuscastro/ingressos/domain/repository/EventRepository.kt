package com.viniciuscastro.ingressos.domain.repository

import com.viniciuscastro.ingressos.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
    fun observeEvent(id: String): Flow<Event?>
    suspend fun findEvent(id: String): Event?
    suspend fun decreaseAvailability(eventId: String, quantity: Int)
}
