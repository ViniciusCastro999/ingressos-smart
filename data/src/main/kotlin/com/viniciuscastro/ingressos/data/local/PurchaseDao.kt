package com.viniciuscastro.ingressos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PurchaseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(purchase: PurchaseEntity)

    @Transaction
    @Query("SELECT * FROM purchases WHERE id = :id")
    abstract suspend fun find(id: String): PurchaseWithTickets?

    @Transaction
    @Query("SELECT * FROM purchases WHERE status = :status ORDER BY created_at DESC LIMIT 1")
    abstract suspend fun findLatestWithStatus(status: String): PurchaseWithTickets?

    @Transaction
    @Query("SELECT * FROM purchases WHERE id = :id")
    abstract fun observe(id: String): Flow<PurchaseWithTickets?>

    @Transaction
    @Query("SELECT * FROM purchases ORDER BY created_at DESC")
    abstract fun observeAll(): Flow<List<PurchaseWithTickets>>

    @Transaction
    @Query("SELECT * FROM purchases WHERE status = :status ORDER BY created_at DESC LIMIT 1")
    abstract fun observeLatestWithStatus(status: String): Flow<PurchaseWithTickets?>

    /**
     * Compare-and-set inside one SQLite transaction: the status check and the writes cannot interleave
     * with another writer, so a purchase is resolved (and its tickets issued) at most once.
     */
    @Transaction
    open suspend fun updateIfStatus(
        purchase: PurchaseEntity,
        tickets: List<TicketEntity>,
        expectedStatus: String,
    ): Boolean {
        if (findStatus(purchase.id) != expectedStatus) return false
        update(purchase)
        insertTickets(tickets)
        return true
    }

    @Query("SELECT status FROM purchases WHERE id = :id")
    protected abstract suspend fun findStatus(id: String): String?

    @Update
    protected abstract suspend fun update(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTickets(tickets: List<TicketEntity>)
}
