package io.github.pdvrieze.pma.agfil.data

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
@JvmInline
value class Money(val onlyCents: Long) {
    val units: Long get() = onlyCents/100
    val cents: Int get() = (onlyCents%100).toInt()

    fun toBigDecimal(): BigDecimal = BigDecimal(onlyCents)/ BigDecimal(100)
    fun toDouble(): Double = onlyCents.toDouble()/100.0

    constructor(units: Long, cents: Long): this(units*100L+cents)

    operator fun plus(other: Money) = Money(onlyCents + other.onlyCents)
    operator fun minus(other: Money) = Money(onlyCents - other.onlyCents)
    operator fun times(other: Int) = Money(onlyCents * other)
    operator fun div(other: Int) = Money(onlyCents / other)

    override fun toString(): String {
        return "$units.${cents.toString().padStart(2, '0')}"
    }
}
