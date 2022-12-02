/**
 * Definitions:
 *
 * - `Pk` is the kth prime, and is usually used when the first k primes are to
 *   be multiplied together; `zStepPk` refers to the number of primes that must
 *   be multiplied to form the step size for z.
 * - `Waterfall` refers to either a number in OEIS A025487, or the list of
 *   exponents themselves. Where p_i are the consecutive primes starting with
 *   p_1 = 2, and n has prime factorization `p_1^a_1 * p_2^a_2 * ... * p_k^a_k`
 *   then it is a waterfall number if `a_1 >= a_2 >= a_3 >= ... >= a_k`.
 *   Simply put, there are no gaps in the prime factorization and the exponents
 *   are non-ascending. For example, `10080 = (2^5)(3^2)(5^1)(7^1)`.
 */

package org.timmc.zaremba

import java.math.BigInteger
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Approximate sigma_1(n), the sum of the divisors, without computing them
 * directly.
 */
fun sigmaApprox(primeFactors: List<Int>): Double {
    return primeFactors.mapIndexed { pK, exp ->
        val p = primes[pK].toDouble()
        (p.pow(exp + 1) - 1)/(p - 1)
    }.product()
}

/**
 * Approximate h(n) = sigma(n)/n = sum(1/divisor) -- without computing divisors.
 */
fun hApprox(n: Double, primeFactors: List<Int>): Double {
    return sigmaApprox(primeFactors) / n
}

/**
 * Compute z(n) based on a formula from Weber 2020 [1]. This method avoids
 * having to compute all the divisors of n, which can be computationally much
 * more expensive.
 *
 * [1] https://arxiv.org/pdf/1810.10876.pdf
 */
fun zaremba(n: BigInteger, primeFactors: List<Int>): Double {
    val nApprox = n.toDouble()
    // Precomputing a list of powers of primes did not achieve a noticeable speedup.
    return primeFactors.mapIndexed { pK, exp ->
        val p = primes[pK].toDouble()
        val lnP = ln(p)
        val h = hApprox(nApprox / p.pow(exp), primeFactors.swapAt(pK) { 0 })
        (1..exp).sumOf { j -> j * lnP / p.pow(j) } * h
    }.sum()
}

/**
 * Given a set of prime exponents, give the number of divisors if they were
 * recomposed.
 */
fun primesToTau(exponents: List<Int>): Long {
    return exponents.map { it + 1L }.product()
}

data class RecordSetter(
    val n: String, // JSON doesn't handle BigInteger well
    val tau: Long,
    val z: Double,
    val isZRecord: Boolean,
    val v: Double,
    val isVRecord: Boolean,
    val primes: List<Int>,
    val primorials: List<Int>
)

/**
 * Yield all n that produce record-setting values for z(n) or v(n).
 */
fun findRecords(maxN: BigInteger): Sequence<RecordSetter> {
    return sequence {
        var recordZ = 0.0
        var recordV = 0.0
        for (n in findWaterfall(maxN).drop(1)) {
            val primeExp = transposePrimorialsToPrimes(n.primorialExponents)
            val tau = primesToTau(primeExp)
            val z = zaremba(n.value, primeExp)
            val v = z / ln(tau.toDouble())

            val isRecordZ = recordZ > 0 && z > recordZ
            val isRecordV = recordV > 0 && v > recordV

            if (isRecordZ || isRecordV) {
                yield(RecordSetter(
                    n = n.value.toString(), tau = tau,
                    z = z, isZRecord = isRecordZ,
                    v = v, isVRecord = isRecordV,
                    primes = primeExp, primorials = n.primorialExponents,
                ))
            }

            recordZ = max(z, recordZ)
            recordV = max(v, recordV)
        }
    }
}
