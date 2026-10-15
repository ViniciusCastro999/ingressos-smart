package com.viniciuscastro.ingressos.domain.model

/**
 * Amount in BRL cents. Integer cents avoid floating-point rounding and match the unit
 * expected by Cielo Smart (`value` and `unitPrice` are sent in cents).
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    init {
        require(cents >= 0) { "Money cannot be negative: $cents" }
    }

    operator fun plus(other: Money): Money = Money(Math.addExact(cents, other.cents))

    operator fun times(quantity: Int): Money = Money(Math.multiplyExact(cents, quantity.toLong()))

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    companion object {
        val ZERO = Money(0)
    }
}
