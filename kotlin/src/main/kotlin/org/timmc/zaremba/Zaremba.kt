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
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

object Zaremba {
    /**
     * Approximate sigma_1(n), the sum of the divisors, without computing them
     * directly.
     */
    fun sigmaApprox(primeFactors: PrimeExp): Double {
        return primeFactors.mapIndexed { pK, exp ->
            val p = Primes.list[pK].toDouble()
            (p.pow(exp + 1) - 1)/(p - 1)
        }.product()
    }

    /**
     * Approximate h(n) = sigma(n)/n = sum(1/divisor) -- without computing divisors.
     */
    fun hApprox(n: Double, primeFactors: PrimeExp): Double {
        return sigmaApprox(primeFactors) / n
    }

    /**
     * Compute z(n) based on a formula from Weber 2020 [1]. This method avoids
     * having to compute all the divisors of n, which can be computationally much
     * more expensive.
     *
     * [1] https://arxiv.org/pdf/1810.10876.pdf
     */
    fun z(n: BigInteger, primeFactors: PrimeExp): Double {
        val nApprox = n.toDouble()
        // Precomputing a list of powers of primes did not achieve a noticeable speedup.
        return primeFactors.mapIndexed { pK, exp ->
            val p = Primes.list[pK].toDouble()
            val lnP = ln(p)
            val h = hApprox(nApprox / p.pow(exp), primeFactors.swapAt(pK) { 0 })
            (1..exp).sumOf { j -> j * lnP / p.pow(j) } * h
        }.sum()
    }

    /**
     * Given a set of prime exponents, give the number of divisors if they were
     * recomposed.
     */
    fun primesToTau(primeExponents: PrimeExp): BigInteger {
        return primeExponents.map { BigInteger.ONE + it.toBigInteger() }.product()
    }

    /**
     * Given a set of primorial exponents, give the number of divisors if they were
     * recomposed.
     */
    fun primorialsToTau(primorialExponents: PrimorialExp): BigInteger {
        return primesToTau(Primorials.toPrimes(primorialExponents))
    }

    data class RecordSetter(
        val n: String, // JSON doesn't handle BigInteger well
        val tau: BigInteger,
        val z: Double,
        val isZRecord: Boolean,
        val v: Double,
        val isVRecord: Boolean,
        val primes: PrimeExp,
        val primorials: PrimorialExp,
    )

    /**
     * Yield all n that produce record-setting values for z(n) or v(n).
     */
    fun findRecords(maxN: BigInteger): Sequence<RecordSetter> {
        return sequence {
            var recordZ = 0.0
            var recordV = 0.0
            for (n in Waterfall.findUpTo(maxN).drop(1)) {
                val primeExp = Primorials.toPrimes(n.primorialExponents)
                val tau = primesToTau(primeExp)
                val z = z(n.value, primeExp)
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

    data class KPrimesIntermediate(
        val zMax: Double,
        val logTauMax: Double,
        val tauMax: BigInteger,
    )

    data class KPrimesResult(
        val primorials: PrimorialExp,
        val primes: PrimeExp,
        val tau: BigInteger,
        val n: BigInteger,
        val z: Double,
        val v: Double,
    )

    /**
     * Compute v(n) for all candidate waterfall numbers with k primes, informed
     * by a previous record-setting v(n) value. The results will need to be
     * filtered to see if any are actually record-setters.
     *
     * (The intermediate results and non-record-setting results are returned for
     * testing purposes.)
     */
    fun findHigherRecordsKPrimes(k: Int, vRecord: Double): Pair<KPrimesIntermediate, Sequence<KPrimesResult>> {
        // Get the first k primes, as doubles
        val kPrimesD = Primes.list.take(k).map(BigInteger::toDouble)

        // First, get an upper bound on z(n) using Weber's lemma. We have to
        // assume that any and all of the first k primes could be factors, so
        // instead of being able to check if p|n, we just use p_1 through p_k.
        val zMax = kPrimesD.map { p -> p/(p - 1.0) }.product() *
            kPrimesD.sumOf { p -> ln(p)/(p - 1.0) }

        // Since v = z/log(tau), we can use the largest known V and the largest
        // possible z to compute an upper bound on log tau.
        val logTauMax = zMax/vRecord
        val tauMax = exp(logTauMax).roundToLong().toBigInteger()

        // Gather up these intermediate values for printing
        val intermediate = KPrimesIntermediate(zMax, logTauMax, tauMax)

        val results = Waterfall.forKPrimesAndMaxTau(k, tauMax).map { n ->
            val primeExponents = Primorials.toPrimes(n.primorialExponents)
            val tau = primesToTau(primeExponents)
            val z = z(n.value, primeExponents)
            val v = z / ln(tau.toDouble())
            KPrimesResult(
                primorials = n.primorialExponents, primes = primeExponents,
                tau = tau, n = n.value, z = z, v = v,
            )
        }

        return intermediate to results
    }
}
