package com.viniciuscastro.ingressos.domain.common

import java.time.Instant

fun interface Clock {
    fun now(): Instant
}
