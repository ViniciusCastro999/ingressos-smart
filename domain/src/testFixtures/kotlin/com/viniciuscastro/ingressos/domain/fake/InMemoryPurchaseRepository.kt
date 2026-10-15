package com.viniciuscastro.ingressos.domain.fake

import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.yield

/**
 * Same contract as the Room implementation, including the compare-and-set semantics of [update].
 * Every call suspends, like real I/O, so concurrent callers can interleave as they would on a database.
 */
class InMemoryPurchaseRepository(initial: List<Purchase> = emptyList()) : PurchaseRepository {

    private val state = MutableStateFlow(initial.associateBy { it.id })

    /** Hook to simulate a concurrent writer between a read and the compare-and-set. */
    var beforeUpdate: (suspend (purchaseId: String) -> Unit)? = null

    val all: List<Purchase> get() = state.value.values.toList()

    fun stored(id: String): Purchase = checkNotNull(state.value[id]) { "No purchase $id" }

    fun put(purchase: Purchase) = state.update { it + (purchase.id to purchase) }

    override suspend fun create(purchase: Purchase) {
        yield()
        check(purchase.id !in state.value) { "Duplicate purchase id ${purchase.id}" }
        put(purchase)
    }

    override suspend fun find(id: String): Purchase? {
        yield()
        return state.value[id]
    }

    override suspend fun findAwaitingTerminal(): Purchase? {
        yield()
        return state.value.values.awaitingTerminal()
    }

    override suspend fun update(purchase: Purchase, expectedStatus: PurchaseStatus): Boolean {
        yield()
        beforeUpdate?.invoke(purchase.id)
        var updated = false
        state.update { current ->
            updated = current[purchase.id]?.status == expectedStatus
            if (updated) current + (purchase.id to purchase) else current
        }
        return updated
    }

    override fun observe(id: String): Flow<Purchase?> = state.map { it[id] }.distinctUntilChanged()

    override fun observeAll(): Flow<List<Purchase>> =
        state.map { purchases -> purchases.values.sortedByDescending { it.createdAt } }

    override fun observeAwaitingTerminal(): Flow<Purchase?> =
        state.map { it.values.awaitingTerminal() }.distinctUntilChanged()

    private fun Collection<Purchase>.awaitingTerminal(): Purchase? =
        filter { it.status == PurchaseStatus.PENDING }.maxByOrNull { it.createdAt }
}
