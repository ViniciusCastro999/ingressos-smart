package com.viniciuscastro.ingressos.data

import android.content.Context
import com.viniciuscastro.ingressos.data.catalog.AssetEventCatalog
import com.viniciuscastro.ingressos.data.local.IngressosDatabase
import com.viniciuscastro.ingressos.data.repository.RoomEventRepository
import com.viniciuscastro.ingressos.data.repository.RoomPurchaseRepository
import com.viniciuscastro.ingressos.data.repository.RoomTransactionRunner
import com.viniciuscastro.ingressos.domain.repository.EventRepository
import com.viniciuscastro.ingressos.domain.repository.PurchaseRepository
import com.viniciuscastro.ingressos.domain.repository.TransactionRunner

/**
 * Public face of the data module: only domain interfaces leave it. Room stays an implementation
 * detail (it is not even on the app module's compile classpath).
 */
class DataLayer(context: Context) {

    private val database = IngressosDatabase.create(context.applicationContext)

    val eventRepository: EventRepository =
        RoomEventRepository(database.eventDao(), AssetEventCatalog(context.applicationContext))

    val purchaseRepository: PurchaseRepository = RoomPurchaseRepository(database.purchaseDao())

    val transactionRunner: TransactionRunner = RoomTransactionRunner(database)
}
