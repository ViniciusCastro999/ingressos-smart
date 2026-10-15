package com.viniciuscastro.ingressos.domain.repository

import com.viniciuscastro.ingressos.domain.model.Purchase
import com.viniciuscastro.ingressos.domain.model.PurchaseStatus
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {

    /** Fails if a purchase with the same id already exists. */
    suspend fun create(purchase: Purchase)

    suspend fun find(id: String): Purchase?

    /** The purchase currently waiting for the terminal, if any (there is at most one). */
    suspend fun findAwaitingTerminal(): Purchase?

    /**
     * Optimistic concurrency (compare-and-set): persists [purchase], including its tickets, only if
     * the stored status is still [expectedStatus]. Returns `false` when someone else changed it first.
     */
    suspend fun update(purchase: Purchase, expectedStatus: PurchaseStatus): Boolean

    fun observe(id: String): Flow<Purchase?>
    fun observeAll(): Flow<List<Purchase>>
    fun observeAwaitingTerminal(): Flow<Purchase?>
}
