package org.timmc

import java.lang.Double.max
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.system.exitProcess

fun zaremba(n: Int): Double {
    val divisorLimit = ceil(sqrt(n.toFloat())).toInt() + 1 // safety margin for floats...
    var sum = 0.0

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
//        println("  $n / $smallerDivisor")

        when {
            largerDivisor > smallerDivisor -> {
                sum += ln(largerDivisor.toFloat()) / largerDivisor
//                println("  ...and $n / $largerDivisor")
            }
            else -> {
                // We've either reached the sqrt mark exactly (n is a perfect
                // square) and need to stop -- use the small divisor but not the
                // larger one, since that would be overcounting.
                break
            }
        }
    }

    return sum
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        print("ERROR: Wrong number of args. Arguments: max-n")
        exitProcess(1)
    }

    val maxN = args[0].toInt()
    var record = 0.0
    for (n in 1..maxN) {
        val out = zaremba(n)
        val isRecord = record > 0 && out > record
//        val annotation = if (isRecord) " [RECORD]" else ""
//        println("$n: $out$annotation")
        if (isRecord) {
            println("RECORD: z($n) = $out")
        }
        record = max(out, record)
    }
}
