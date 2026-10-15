package com.viniciuscastro.ingressos.domain.repository

/** Runs [block] as a single atomic unit of work across repositories. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
