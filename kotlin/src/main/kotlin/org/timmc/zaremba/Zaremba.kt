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
        val tauMin: BigInteger,
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
     * filtered to see if any are actually new record-setters.
     *
     * (The intermediate results and non-record-setting results are returned for
     * testing purposes.)
     *
     * By calling this with successively larger values of k, until some halting
     * condition is reached, we can search for all possible higher v(n) values
     * exhaustively.
     */
    fun searchVRecordKPrimes(k: Int, vRecord: Double): Pair<KPrimesIntermediate, Sequence<KPrimesResult>> {
        // We know that there's an existing record-setter V_a (here, vRecord.)
        // We're seeking a larger one, V_b > V_a. We then posit that in
        // v(n) = V_b, n has a certain number of distinct primes, k.
        //
        // Given k, we can find an upper bound on the z(n) that would be part of
        // V_b's calculation (z/log tau). The maximum value of V_b is limited by
        // the maximum value of this z(n).
        //
        // So, our first step is to find this value, which we'll call zMax:

        // Get the first k primes, as doubles
        val kPrimesD = Primes.list.take(k).map(BigInteger::toDouble)
        // We can get an upper bound on z(n) using Weber's lemma. We have to
        // assume that any and all of the first k primes could be factors, so
        // instead of being able to check if p|n, we just use all of p_1 through
        // p_k. (More below on why we err in this direction.)
        val zMax = kPrimesD.map { p -> p/(p - 1.0) }.product() *
            kPrimesD.sumOf { p -> ln(p)/(p - 1.0) }

        // The other part of the calculation is log(tau(n)). For a given k,
        // there are an infinite number of possible waterfall numbers, and
        // values for tau. We cannot enumerate them.
        //
        // However, as tau (really log(tau), but same logic) increases in the
        // denominator, v(n) decreases/ At some point we would reach the
        // breakeven point of v(n) = V_a, after which we would no longer be able
        // to set a new record.
        //
        // The larger z(n) is, the larger tau can rise without hitting the
        // breakeven, so it's beneficial to err on the side of a larger z(n).
        // That's what allowed us to use *all* the primes through p_k in the
        // zMax calculation above.
        //
        // So the breakeven point is V_a = zMax/log(tau); solve for tau to get
        // tauMax. Any new record-setter must have a tau no larger than this.
        val logTauMax = zMax/vRecord
        val tauMax = exp(logTauMax).roundToLong().toBigInteger()

        // It's also useful to know what the *smallest* tau could be, since this
        // tells us what the *largest* v could be. If tauMin is too large then
        // it probably isn't worth looking at higher primes.
        val tauMin = primesToTau(List(k) { 1 })

        // Gather up these intermediate values for printing
        val intermediate = KPrimesIntermediate(zMax, logTauMax, tauMax, tauMin)

        // Now we can generate and check all waterfall numbers with a
        // tau(n) ≤ tauMax. This is a relatively tractable set to enumerate.
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

    /**
     * Yield successively larger record-setters for v(n) until the largest is
     * reached. This is an incomplete list of record-setters.
     */
    fun maxVByBootstrapping(): Sequence<Double> {
        return sequence {
            // Start with known value for v(4)
            var vRecord = 0.6309297535714574
            println("Starting bootstrap with v(4) = $vRecord")

            // Get higher and higher V values until we stop finding them
            while (true) {
                // For the current best record, try higher and higher prime
                // counts k until they are incapable of producing a new record
                // (or until a new record is found).
                var k = 1
                while (true) {
                    println("  Searching for next record with $k primes")
                    val (intermediate, candidates) = searchVRecordKPrimes(k, vRecord)

                    // We could just stop at the first new record-setter, but
                    // might as well sort and find the highest one in these results.
                    val newRecord = candidates.filter { it.v > vRecord }.sortedBy { it.v }.lastOrNull()
                    println("    Checked ${candidates.count()} candidates, with max tau = ${intermediate.tauMax}")

                    if (newRecord != null) {
                        println("    Found new record! v=${newRecord.v}\tn=${newRecord.n}\tz=${newRecord.z}\ttau=${newRecord.tau}")
                        vRecord = newRecord.v
                        yield(newRecord.v)
                        break
                    }

                    // We didn't find a result on this k, so check to see if it
                    // would even be possible. If not, a higher k won't help,
                    // and we're done.
                    //
                    // NOTA BENE: This is not yet proven to work as a halting
                    //  condition. We are still having to hand-check the halting
                    //  case. In practice though, it halts at k=35, well above
                    //  the k=29 that we have established by hand.
                    val ltMin = ln(intermediate.tauMin.toDouble())
                    val vMax = intermediate.zMax / ltMin
                    if (vMax < vRecord) {
                        println("    Stopping: z-max/log(min-tau) = ${intermediate.zMax}/$ltMin < $vRecord = record-v")
                        return@sequence
                    }

                    k++
                }
            }
        }
    }
}
