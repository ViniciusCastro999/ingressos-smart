package com.viniciuscastro.ingressos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY starts_at")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observe(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun find(id: String): EventEntity?

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(events: List<EventEntity>)

    /** Called for an approved payment: money was captured, so stock is consumed and floors at zero. */
    @Query("UPDATE events SET available_tickets = MAX(available_tickets - :quantity, 0) WHERE id = :id")
    suspend fun decreaseAvailability(id: String, quantity: Int)
}
