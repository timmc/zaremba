package org.timmc

import java.lang.Double.max
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.system.exitProcess

/**
 * Compute z(n) and tau(n), returned as a pair.
 */
fun zarembaAndTau(n: Int): Pair<Double, Int> {
    val divisorLimit = ceil(sqrt(n.toFloat())).toInt() + 1 // safety margin for floats...
    var sum = 0.0
    var divisorCount = 0

    for (smallerDivisor in 1 .. divisorLimit) {
        if (n.mod(smallerDivisor) != 0)
            continue // not a divisor!

        val largerDivisor = n / smallerDivisor
        if (largerDivisor < smallerDivisor) {
            // We've *passed* the geometric midpoint, and should not include
            // either value. Could be redundant with divisorLimit but that one
            // is padded a little for safety. This check is more precise.
            break
        }

        sum += ln(smallerDivisor.toFloat()) / smallerDivisor
        divisorCount++

        when {
            largerDivisor > smallerDivisor -> {
                sum += ln(largerDivisor.toFloat()) / largerDivisor
                divisorCount++
            }
            else -> {
                // We've either reached the sqrt mark exactly (n is a perfect
                // square) and need to stop -- use the small divisor but not the
                // larger one, since that would be overcounting.
                break
            }
        }
    }

    return sum to divisorCount
}

fun doSingle(n: Int) {
    val (z, tau) = zarembaAndTau(n)
    val ratio = z / ln(tau.toFloat())
    println("z($n) = $z\ttau($n) = $tau\tz($n)/ln(tau($n)) = $ratio")
}

fun doRecords(maxN: Int) {
    var recordZ = 0.0
    var recordRatio = 0.0
    for (n in 1..maxN) {
        val (z, tau) = zarembaAndTau(n)
        val ratio = z / ln(tau.toFloat())

        val isRecordZ = recordZ > 0 && z > recordZ
        val isRecordRatio = recordRatio > 0 && ratio > recordRatio

        val recordType = when {
            isRecordZ && isRecordRatio -> "both"
            isRecordZ && !isRecordRatio -> "z"
            !isRecordZ && isRecordRatio -> "ratio"
            else -> null
        }
        if (recordType != null) {
            println("$n\trecord=$recordType\tz($n) = $z\ttau($n) = $tau\tz($n)/ln(tau($n)) = $ratio")
        }

        recordZ = max(z, recordZ)
        recordRatio = if (ratio.isNaN()) { // NaN for n = 1...
            recordRatio
        } else {
            max(ratio, recordRatio)
        }
    }
}

fun dieWithUsage() {
    println("""
      Usage:
        ./zaremba single [n]
        ./zaremba records [max-n]
    """.trimIndent())
    exitProcess(1)
}

fun main(args: Array<String>) {
    if (args.size != 2)
        dieWithUsage()

    when (args[0]) {
        "single" -> doSingle(args[1].toInt())
        "records" -> doRecords(args[1].toInt())
        else -> dieWithUsage()
    }
}
