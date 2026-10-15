package com.viniciuscastro.ingressos.data.repository

import com.viniciuscastro.ingressos.data.local.PurchaseDao
import com.viniciuscastro.ingressos.data.local.PurchaseWithTickets
import com.viniciuscastro.ingressos.data.local.toDomain
import com.viniciuscastro.ingressos.data.local.toEntity
import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.model.Ticket
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomPurchaseRepository(private val dao: PurchaseDao) : PurchaseRepository {

    override suspend fun create(purchase: Purchase) = dao.insert(purchase.toEntity())

    override suspend fun find(id: String): Purchase? = dao.find(id)?.toDomain()

    override suspend fun findAwaitingTerminal(): Purchase? =
        dao.findLatestWithStatus(PurchaseStatus.PENDING.name)?.toDomain()

    override suspend fun update(purchase: Purchase, expectedStatus: PurchaseStatus): Boolean =
        dao.updateIfStatus(
            purchase = purchase.toEntity(),
            tickets = purchase.tickets.map(Ticket::toEntity),
            expectedStatus = expectedStatus.name,
        )

    override fun observe(id: String): Flow<Purchase?> =
        dao.observe(id).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeAll(): Flow<List<Purchase>> =
        dao.observeAll().map { purchases -> purchases.map(PurchaseWithTickets::toDomain) }.distinctUntilChanged()

    override fun observeAwaitingTerminal(): Flow<Purchase?> =
        dao.observeLatestWithStatus(PurchaseStatus.PENDING.name).map { it?.toDomain() }.distinctUntilChanged()
}
