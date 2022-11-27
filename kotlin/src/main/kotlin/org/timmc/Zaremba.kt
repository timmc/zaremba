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

package org.timmc

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

class PrimesFinder: Iterable<Long> {
    val known = mutableListOf(2L)

    fun findNextPrime(): Long {
        synchronized(known) {
            var primeCandidate = known.last() + 1
            while (true) {
                if (known.any { primeCandidate % it == 0L }) {
                    primeCandidate++
                } else {
                    known.add(primeCandidate)
                    return primeCandidate
                }
            }
        }
    }

    override fun iterator(): Iterator<Long> {
        return iterator {
            // Take here for thread safety (`known` may grow concurrently)
            val initialSize = known.size
            yieldAll(known.subList(0, initialSize))
            var index = initialSize
            while (true) {
                findNextPrime() // increases length by one, if needed
                yield(known[index])
                index++
            }
        }
    }
}

val primes = PrimesFinder()

fun primeSeq() = primes.iterator().asSequence()

/**
 * Produce the prime factorization of an integer.
 *
 * This is optimized for working with waterfall numbers, and may be slow in the
 * general case.
 *
 * If [assertWaterfall] is true, assume the number is in OEIS A025487, and fail
 * fast with null if the prime factors are not non-ascending, contiguous
 * prime factors starting with 2.
 *
 * @return a map of prime factors to their counts, or null if [assertWaterfall]
 *   was true but condition was violated
 */
fun factor(n: Long, assertWaterfall: Boolean = false): Map<Long, Int>? {
    // Running remainder, and factors so far
    var remainder = n
    val factors = mutableMapOf<Long, Int>()

    var previousPrimeRepeat = Int.MAX_VALUE // used for waterfall checks

    for (prime in primeSeq()) {
        // Keep dividing n by prime until we can't
        var repeats = 0
        while (remainder % prime == 0L) {
            remainder = remainder.div(prime)
            repeats++
        }

        // Check if this fails the "non-ascending" constraint
        if (assertWaterfall && repeats > previousPrimeRepeat) {
            return null
        }

        if (repeats == 0) {
            // This prime wasn't a factor at all. Check what that means:
            if (remainder == 1L) {
                // Done factorizing!
                break
            } else {
                // This prime doesn't divide n, and we're not done yet. Is that
                // a problem?
                if (assertWaterfall) {
                    // Violates the "contiguous" constraint.
                    return null
                }
                // If not, just move on to the next prime.
            }
        } else {
            factors[prime] = repeats
        }

        previousPrimeRepeat = repeats
    }
    return factors.toMap()
}

/**
 * Wrapper for [factor] with `assertWaterfall=false` that knows a null result
 * isn't possible.
 */
fun factorGeneric(n: Long): Map<Long, Int> =
    factor(n, assertWaterfall = false)!!

/**
 * Wrapper for [factor] with `assertWaterfall=true`.
 */
fun factorWaterfall(n: Long): Map<Long, Int>? =
    factor(n, assertWaterfall = true)

/**
 * Cartesian product of zero or more iterables.
 *
 * This is adapted from https://stackoverflow.com/questions/53749357/idiomatic-way-to-create-n-ary-cartesian-product-combinations-of-several-sets-of
 * with comments, variable renamings, and minor formatting changes. Also changed
 * return value from a Set to a Sequence, for both order-stability and working
 * with large products. Credit is mainly to Erik Huizinga and Tenfour04.
 */
fun <T> Collection<Iterable<T>>.getCartesianProduct(): Sequence<List<T>> {
    return if (isEmpty()) {
        emptySequence()
    } else {
        // Turn the first iterable into a sequence of single-item lists. Each of
        // these will be the seed that items from remaining iterables will be
        // added onto. This is the beginning of the product.
        val seeds = first().map(::listOf).asSequence()
        // For each remaining iterable, expand the product by combining each
        // element of the existing product with everything in the iterable.
        drop(1).fold(seeds) { product, nextIterable ->
            product.flatMap { productMember -> nextIterable.map(productMember::plus) }
        }
    }
}

/**
 * Given a map of prime divisors to their repeat counts, return all divisors.
 *
 * Does not currently work for an empty input map (i.e. for 1), but may in the
 * future.
 */
fun primesToDivisors(primeFactors: Map<Long, Int>): List<Long> {
    // Make a list of powers lists. E.g. for {2:4, 3:2, 5:1} this would produce
    // [[1,2,4,8,16], [1,3,9], [1,5]]
    val powers = primeFactors.map { (prime, repeat) ->
        generateSequence(1L) { Math.multiplyExact(it, prime) }.take(repeat + 1).toList()
    }
    // Get the Cartesian product, e.g. [[1,1,1], [1,1,5], [1,3,1], [1,3,5]...]
    // and take the product of each sublist. This produces all divisors.
    return powers.getCartesianProduct().map { xs -> xs.reduce(Long::times) }.toList()
}

/**
 * Given a prime factorization, compute z(n) and tau(n), returned as a pair.
 */
fun zarembaAndTau(primeFactors: Map<Long, Int>): Pair<Double, Int> {
    val divisors = primesToDivisors(primeFactors)

    val z = divisors.sumOf { ln(it.toDouble()) / it }
    val tau = divisors.size

    return z to tau
}

fun doSingle(n: Long) {
    val (z, tau) = zarembaAndTau(factorGeneric(n))
    val v = z / ln(tau.toDouble())
    println("z(n) = $z\ttau(n) = $tau\tv(n) = $v")
}

/**
 * Given record-setter z(n), find new step-size for looking for new
 * record-setters.
 */
fun zStep(recordZ: Double): Long {
    var ret = 1L // 2 * 3 * ... * p
    var mertensProduct = 1.0 // 2/1 * 3/2 * ... * p/(p-1)
    for (prime in primeSeq()) {
        ret = Math.multiplyExact(ret, prime)
        mertensProduct *= prime/(prime - 1.0)
        val boundsTest = mertensProduct * ln(prime.toDouble()) // TODO erdos(p)
        // If this prime pushes us past the z bound, it's the last in the prime
        // product that we'll use for step-sizes.
        if (boundsTest > recordZ) {
            return ret
        }
    }
    // Needed in case the primes iterator breaks in a weird way on large numbers
    throw AssertionError("Ran out of primes")
}

/**
 * Given a set of prime exponents, give the number of divisors if they were
 * recomposed.
 */
fun primesToTau(exponents: List<Long>): Long {
    return exponents.map { it + 1 }.product()
}

/**
 * A possible candidate for minTau. Both [product] and [usable] can be derived
 * from [exponents] but are provided for performance, as they are already
 * computed in the pruning logic.
 */
data class MinTauCandidate(
    /**
     * Exponents of first k primes.
     */
    val exponents: List<Long>,
    /**
     * Products of first k primes raised to [exponents].
     */
    val product: Long,
    /**
     * Is this a *usable* candidate, i.e. one where the [product] is at least
     * as large as n? (We can return unusable ones as well just for the sake of
     * unit testing.)
     */
    val usable: Boolean,
)

/**
 * Compute MinTau candidates for [n] from a list of waterfall exponents.
 *
 * For a given zero-based index [i] within [exponents], find further
 * candidates by incrementing the exponent at that index and by moving to the
 * next higher index, recursively, always maintaining the waterfall invariant.
 *
 * During exploration, the running [product] is maintained with is just the
 * waterfall number represented by the exponents.
 *
 * Each new candidate is yielded, unless [fast] is set, in which case only
 * candidates representing a waterfall number ≥ n are yielded.
 *
 * Exploration is pruned whenever a candidate's product is ≥ n.
 *
 * @param n is the current record-setting input
 * @param exponents are waterfall number exponents that might be a candidate
 * @param product is the waterfall number represented by [exponents]
 * @param i is the current index into [exponents]
 * @param fast controls whether only valid candidates are emitted
 */
fun minTauCandidates(
    n: Long, exponents: List<Long>, product: Long, i: Int, fast: Boolean
): Sequence<MinTauCandidate> {
    return sequence {
        val usable = product >= n
        if (usable || !fast)
            yield(MinTauCandidate(exponents, product, usable))
        // Any higher product would *also* be at least n, but would have a
        // higher tau and would not contribute to a minimum.
        if (usable)
            return@sequence
        // Explore further to the right, if possible
        // FIXME: Produces the same candidate multiple times, with duplication
        //  factor approximately sqrt(primesK)
        if (i < exponents.size - 1) {
            yieldAll(minTauCandidates(n, exponents, product, i + 1, fast))
        }
        // Then, explore upwards from this p_k, if possible. We can do that iff
        // we're at the first index or if we wouldn't violate the waterfall
        // constraint.
        if (i == 0 || exponents[i] < exponents[i-1]) {
            val nextExponents = exponents.swapAt(i) { it + 1 }
            val nextProduct = Math.multiplyExact(product, primeSeq().elementAt(i))
            yieldAll(minTauCandidates(n, nextExponents, nextProduct, i, fast))
        }
    }
}

/**
 * Base case for recursive [minTauCandidates].
 */
fun minTauCandidates(n: Long, primesK: Int, fast: Boolean): Sequence<MinTauCandidate> {
    val exponents = List(primesK) {1L}
    val product = primeSeq().take(primesK).product()
    val i = 0
    return minTauCandidates(n, exponents, product, i, fast)
}

/**
 * Finds the smallest tau value of numbers meeting these criteria:
 *
 * - Waterfall number using the first [primesK] primes
 * - Greater than or equal to n
 */
fun minTau(n: Long, primesK: Int): Long {
    return minTauCandidates(n, primesK, fast = true)
        .filter { it.usable } // do this even though fast=true, for safety
        .minOf { primesToTau(it.exponents) }
}

/**
 * Find new step-size for looking for new record-setters, based on V.
 *
 * This requires the most recent *record* for v.
 *
 * @return The number of consecutive primes (from 2) to multiply to get the
 *   step size for v.
 */
fun vStepPk(n: Long, recordV: Double, vStepPkLast: Int): Int {
    if (vStepPkLast < 1) {
        return if (n < 4) {
            0 // step size 1
        } else {
            1 // step size 2
        }
    }

    val lastStepPrimes = primeSeq().take(vStepPkLast).toList()

    // Merten's Product up to largest prime in current v step size
    val mert = lastStepPrimes.map { p -> p/(p - 1.0) }.product()
    // And the Erdos sum
    val erdos = lastStepPrimes.sumOf { p -> ln(p.toDouble())/(p - 1) }
    val minTau = minTau(n, vStepPkLast)

    // Multiplication by 1.0 is just to visually prove that there isn't any
    // possible integer overflow, i.e. it's a "safe" multiplication.
    return if (mert * 1.0 * erdos / ln(minTau.toDouble()) <= recordV) {
        val nextStepCount = vStepPkLast + 1
        val nextStep = primeSeq().take(nextStepCount).product()

        if (n % nextStep == 0L) {
            nextStepCount
        } else {
            // need to wait until a higher multiple
            vStepPkLast
        }
    } else {
        // no luck, use the old one
        vStepPkLast
    }
}

/**
 * Given two step values, produce a step size that satisfies both.
 */
fun minStep(stepA: Long, stepB: Long): Long {
    // If this ever throws, need to switch to using the GCD.
    if (stepA % stepB != 0L && stepB % stepA != 0L) {
        throw AssertionError("Assuming stepA divides stepB or vice versa")
    }
    return min(stepA, stepB)
}

data class RecordSetter(
    val n: Long,
    val z: Double,
    val v: Double,
    val type: String,
    val tau: Int,
    val stepSize: Long,
    val stepSizeFromV: Long,
)

/**
 * Yield all n that produce record-setting values for z(n) or v(n).
 *
 * Eventually overflows Long and crashes, if you get that far.
 *
 * Assumes that all record-setters will be waterfall numbers, so don't even do
 * those computations if waterfall factorization fails.
 */
fun findRecords(): Iterator<RecordSetter> {
    return iterator {
        var n = 1L
        var vStepPk = 0 // number of primes to multiply for the v step size
        var stepSize = 1L // *actual* step size to use, based on z and v steps

        var recordZ = 0.0
        var recordV = 0.0
        while (true) {
            val primeFactors = factorWaterfall(n)
            if (primeFactors != null) {
                val (z, tau) = zarembaAndTau(primeFactors)

                val v = z / ln(tau.toDouble())

                val isRecordZ = recordZ > 0 && z > recordZ
                val isRecordV = recordV > 0 && v > recordV

                recordZ = max(z, recordZ)
                recordV = if (v.isNaN()) { // NaN for n = 1...
                    recordV
                } else {
                    max(v, recordV)
                }

                val recordType = when {
                    isRecordZ && isRecordV -> "both"
                    isRecordZ && !isRecordV -> "z"
                    !isRecordZ && isRecordV -> "v"
                    else -> null
                }

                if (isRecordZ || isRecordV) {
                    // Calculate new step size for both Z and V even if only one
                    // changed, just because that's easier.
                    val zStepNew = zStep(recordZ=recordZ)
                    vStepPk = vStepPk(n=n, recordV=recordV, vStepPkLast=vStepPk)
                    val vStepNew = primeSeq().take(vStepPk).product()
                    stepSize = minStep(zStepNew, vStepNew)

                    yield(RecordSetter(
                        n = n, z = z, v = v, type = recordType!!,
                        tau = tau, stepSize = stepSize,
                        stepSizeFromV = vStepNew,
                    ))
                }
            }
            n += stepSize
        }
    }
}

fun Sequence<Long>.product(): Long = fold(1) { acc, n -> Math.multiplyExact(acc, n) }
fun Iterable<Double>.product(): Double = fold(1.0) { acc, n -> acc * n }
fun Iterable<Long>.product(): Long = fold(1) { acc, n -> Math.multiplyExact(acc, n) }

fun <T> List<T>.swapAt(at: Int, mapper: (T) -> T): List<T> {
    return mapIndexed { i, v -> if (i == at) mapper(v) else v }
}

/**
 * Find and print all n that produce record-setting
 * values for z(n) or v(n).
 *
 * Eventually overflows Long and crashes, if you get that far.
 */
fun doRecords(variantStr: String) {
    when (variantStr) {
        "classic" -> findRecords().forEach { r ->
            println("${r.n}\trecord=${r.type}\tz(n) = ${r.z}\ttau(n) = ${r.tau}\tv(n) = ${r.v}\tstep=${r.stepSize}")
        }
        "latex" -> {
            println("n & z(n) & tau(n) & v(n) & type of record & step size in effect & step size from v")
            findRecords().forEach { r ->
                println("${r.n} & ${r.z} & ${r.tau} & ${r.v} & ${r.type} & ${r.stepSize} & ${r.stepSizeFromV}")
            }
        }
        else -> dieWithUsage()
    }
}

fun doFactor(n: Long) {
    val factorization = factorGeneric(n).toSortedMap()
    // The sparkline will make the most sense with waterfall numbers...
    val levels = listOf("̲ ", '▁', '▂', '▃', '▄', '▅', '▆', '▇')
    val sparkline = factorization.values.joinToString("") {
        levels[it - 1].toString()
    }
    println("Factors: $sparkline ${factorization.map { (k, v) -> "$k^$v" }.joinToString(" * ")}")
}

fun dieWithUsage(): Nothing {
    println("""
      Usage:
        ./zaremba single <n>
        ./zaremba records [classic | latex]
        ./zaremba factor <n>
    """.trimIndent())
    exitProcess(1)
}

fun main(args: Array<String>) {
    if (args.isEmpty())
        dieWithUsage()

    when (args[0]) {
        "single" -> {
            if (args.size != 2)
                dieWithUsage()
            doSingle(args[1].toLong())
        }
        "records" -> {
            if (args.size != 2)
                dieWithUsage()
            doRecords(args[1])
        }
        "factor" -> {
            if (args.size != 2)
                dieWithUsage()
            doFactor(args[1].toLong())
        }
        else -> dieWithUsage()
    }
}
