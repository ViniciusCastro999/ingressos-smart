package com.viniciuscastro.ingressos.domain.usecase

import com.viniciuscastro.ingressos.domain.model.Event
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.repository.EventRepository
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow

// Read-only queries. They are thin on purpose: the presentation layer depends on use cases only,
// so a rule added to a query later (filters, sorting, caching) does not touch any ViewModel.

class ObserveEventsUseCase(private val events: EventRepository) {
    operator fun invoke(): Flow<List<Event>> = events.observeEvents()
}

class ObserveEventUseCase(private val events: EventRepository) {
    operator fun invoke(eventId: String): Flow<Event?> = events.observeEvent(eventId)
}

class ObservePurchaseUseCase(private val purchases: PurchaseRepository) {
    operator fun invoke(purchaseId: String): Flow<Purchase?> = purchases.observe(purchaseId)
}

class ObservePurchaseHistoryUseCase(private val purchases: PurchaseRepository) {
    operator fun invoke(): Flow<List<Purchase>> = purchases.observeAll()
}

class ObservePendingPurchaseUseCase(private val purchases: PurchaseRepository) {
    operator fun invoke(): Flow<Purchase?> = purchases.observeAwaitingTerminal()
}
