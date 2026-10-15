package com.viniciuscastro.ingressos.domain.common

fun interface IdGenerator {
    fun newId(): String
}
