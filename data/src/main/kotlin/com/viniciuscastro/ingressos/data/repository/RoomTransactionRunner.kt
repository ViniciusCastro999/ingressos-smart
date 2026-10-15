package com.viniciuscastro.ingressos.data.repository

import androidx.room.withTransaction
import com.viniciuscastro.ingressos.data.local.IngressosDatabase
import com.viniciuscastro.ingressos.domain.repository.TransactionRunner

class RoomTransactionRunner(private val database: IngressosDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}
