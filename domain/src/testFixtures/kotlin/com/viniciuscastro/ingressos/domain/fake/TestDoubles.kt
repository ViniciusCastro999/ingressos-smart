package com.viniciuscastro.ingressos.domain.fake

import com.viniciuscastro.ingressos.domain.common.Clock
import com.viniciuscastro.ingressos.domain.common.IdGenerator
import com.viniciuscastro.ingressos.domain.common.Telemetry
import com.viniciuscastro.ingressos.domain.common.TelemetryEvent
import com.viniciuscastro.ingressos.domain.repository.TransactionRunner
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class FixedClock(var now: Instant = TestData.NOW) : Clock {
    override fun now(): Instant = now
    fun advance(duration: Duration) {
        now += duration
    }
}

class SequentialIdGenerator(private val prefix: String = "id") : IdGenerator {
    private val counter = AtomicInteger()
    override fun newId(): String = "$prefix-${counter.incrementAndGet()}"
}

class RecordingTelemetry : Telemetry {
    val events: MutableList<TelemetryEvent> = CopyOnWriteArrayList()
    override fun track(event: TelemetryEvent) {
        events += event
    }
}

class ImmediateTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
