package com.viniciuscastro.ingressos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class, PurchaseEntity::class, TicketEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class IngressosDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        private const val NAME = "ingressos.db"

        fun create(context: Context): IngressosDatabase =
            Room.databaseBuilder(context, IngressosDatabase::class.java, NAME).build()
    }
}
