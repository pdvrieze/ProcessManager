package io.github.pdvrieze.process.simulator.utils

import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Create a random variable representing the next occurrence that independently occurs [frequency] times in a
 * reference time interval (length 1).
 */
fun Random.nextExponentialVariate(frequency: Double): Double {
    return (-ln(nextUniform()))/frequency
}

fun Random.nextUniform(): Double = nextDouble()

/**
 * Implement variate using Marsaglia polar method
 */
fun Random.nextGaussians(mean: Double=0.0, stdDev: Double=1.0): Pair<Double, Double> {
    var s: Double
    var u: Double
    var v: Double
    do {
        u = nextDouble(-1.0, 1.0)
        v = nextDouble(-1.0, 1.0)
        s = u*u + v*v
    } while (s>=1.0 || s==0.0)
    val len = sqrt(-2.0 * ln(s)) * stdDev // pre-multiply stdev to avoid calculating len*stdDev more than once
    val r1 = mean + (u*len)
    val r2 = mean + (v*len)
    return Pair(r1, r2)
}

fun Random.nextGaussian(mean: Double=0.0, stdDev: Double=1.0): Double {
    return nextGaussians(mean, stdDev).first
}
