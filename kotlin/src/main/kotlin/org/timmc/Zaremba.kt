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

/**
 * Produce the prime factorization of an integer.
 *
 * This is optimized for working with A025487 numbers, and may be slow in the
 * general case.
 *
 * If [assertA025487] is true, assume the number is in OEIS A025487. and fail
 * fast with null if the prime factors are not non-ascending, contiguous
 * prime factors starting with 2.
 *
 * A025487:
 *
 * If n has prime factorization `p_1^a_1 * p_2^a_2 * ... * p_k^a_k` (where
 * `p_1 < p_2 < p_3 < ... < p_k`) then require that `a_1 >= a_2 >= a_3 >= ... >= a_k`.
 * There must also be no prime between `p_1` and `p_k` that is not in the
 * factorization. Finally, `p_1 = 2`. For example, `10080 = (2^5)(3^2)(5^1)(7^1)`.
 *
 * @return a map of prime factors to their counts, or null if [assertA025487]
 *   was true but condition was violated
 */
fun factor(n: Long, assertA025487: Boolean = false): Map<Long, Int>? {
    // Running remainder, and factors so far
    var remainder = n
    val factors = mutableMapOf<Long, Int>()

    var previousPrimeRepeat = Int.MAX_VALUE // used for A025487 checks

    for (prime in primes.iterator()) {
        // Keep dividing n by prime until we can't
        var repeats = 0
        while (remainder % prime == 0L) {
            remainder = remainder.div(prime)
            repeats++
        }

        // Check if this fails the "non-ascending" constraint
        if (assertA025487 && repeats > previousPrimeRepeat) {
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
                if (assertA025487) {
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
 * Wrapper for [factor] with `assertA025487=false` that knows a null result
 * isn't possible.
 */
fun factorGeneric(n: Long): Map<Long, Int> =
    factor(n, assertA025487 = false)!!

/**
 * Wrapper for [factor] with `assertA025487=true`.
 */
fun factorA025487(n: Long): Map<Long, Int>? =
    factor(n, assertA025487 = true)

/**
 * Cartesian product of zero or more iterables.
 *
 * This is adapted from https://stackoverflow.com/questions/53749357/idiomatic-way-to-create-n-ary-cartesian-product-combinations-of-several-sets-of
 * with comments, variable renamings, and minor formatting changes. Also changed
 * return value from a Set to a List, for stability. Credit is
 * mainly to Erik Huizinga and Tenfour04.
 */
fun <T> Collection<Iterable<T>>.getCartesianProduct(): List<List<T>> {
    return if (isEmpty()) {
        emptyList()
    } else {
        // Turn the first iterable into a list of single-item lists. Each of
        // these will be the seed that items from remaining iterables will be
        // added onto. This is the beginning of the product.
        val seeds = first().map(::listOf)
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
        generateSequence(1L) { it * prime }.take(repeat + 1).toList()
    }
    // Get the Cartesian product, e.g. [[1,1,1], [1,1,5], [1,3,1], [1,3,5]...]
    // and take the product of each sublist. This produces all divisors.
    return powers.getCartesianProduct().map { xs -> xs.reduce(Long::times) }
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
    val ratio = z / ln(tau.toDouble())
    println("z($n) = $z\ttau($n) = $tau\tz($n)/ln(tau($n)) = $ratio")
}

/**
 * Given record-setter z(n), find new step-size for looking for new
 * record-setters.
 */
fun stepSizeAfterRecordZ(z: Double): Long {
    var ret = 1L // 2 * 3 * ... * p
    var mertensProduct = 1.0 // 2/1 * 3/2 * ... * p/(p-1)
    for (prime in primes.iterator()) {
        ret *= prime
        mertensProduct *= prime/(prime - 1.0)
        val boundsTest = mertensProduct * ln(prime.toDouble())
        // If this prime pushes us past the z bound, it's the last in the prime
        // product that we'll use for step-sizes.
        if (boundsTest > z) {
            return ret
        }
    }
    // Needed in case the primes iterator breaks in a weird way on large numbers
    throw AssertionError("Ran out of primes")
}

val ln2 = ln(2.0)

/**
 * Given record-setter ratio, find new step-size for looking for new
 * record-setters.
 *
 * This only differs from [stepSizeAfterRecordZ] in the bounds multiplier, but
 * it's not straightforward to generalize since the inputs to the multiplier are
 * different.
 */
fun stepSizeAfterRecordRatio(n: Long, ratio: Double): Long {
    // A hardcoded threshold based on known data; haven't yet proven what the
    // smallest number is where the generalized formula starts working.
    if (n < 1e9) {
        return 1
    }

    var k = 0
    var step = 1L // 2 * 3 * ... * p
    var mertensProduct = 1.0 // 2/1 * 3/2 * ... * p/(p-1)

    for (prime in primes.iterator()) {
        k++
        step *= prime
        mertensProduct *= prime/(prime - 1.0)

        val boundsTest = mertensProduct * ln(prime.toDouble()) / ((k+2) * ln2)
        // If this prime pushes us past the ratio bound, it's the last in the prime
        // product that we'll use for step-sizes.
        if (boundsTest > ratio) {
            return step
        }
    }
    // Needed in case the primes iterator breaks in a weird way on large numbers
    throw AssertionError("Ran out of primes")
}

/**
 * Find and print all n that produce record-setting
 * values for z(n) or z(n)/log(tau(n)).
 *
 * Eventually overflows Long and crashes, if you get that far.
 *
 * Assumes that all record-setters will be A025487 numbers, so don't even do
 * those computations if A025487 factorization fails.
 */
fun doRecords(): Nothing {
    var n = 1L
    var stepSize = 1L

    var recordZ = 0.0
    var recordRatio = 0.0
    while (true) {
        val primeFactors = factorA025487(n)
        if (primeFactors != null) {
            val (z, tau) = zarembaAndTau(primeFactors)

            val ratio = z / ln(tau.toDouble())

            val isRecordZ = recordZ > 0 && z > recordZ
            val isRecordRatio = recordRatio > 0 && ratio > recordRatio

            val recordType = when {
                isRecordZ && isRecordRatio -> "both"
                isRecordZ && !isRecordRatio -> "z"
                !isRecordZ && isRecordRatio -> "ratio"
                else -> null
            }

            if (isRecordZ || isRecordRatio) {
                val stepSizeZ = stepSizeAfterRecordZ(z)
                val stepSizeRatio = stepSizeAfterRecordRatio(n, ratio)
                // Assert that we can use min rather than taking the GCD, which
                // would require more code.
                if (stepSizeZ % stepSizeRatio != 0L) {
                    throw AssertionError(
                        "Assuming ratio steps are smaller than z steps, and that the lesser divides the greater."
                    )
                }
                stepSize = min(stepSizeZ, stepSizeRatio)
            }

            if (recordType != null) {
                println("$n\trecord=$recordType\tz(n) = $z\ttau(n) = $tau\tz(n)/ln(tau(n)) = $ratio\tstep=$stepSize")
            }

            recordZ = max(z, recordZ)
            recordRatio = if (ratio.isNaN()) { // NaN for n = 1...
                recordRatio
            } else {
                max(ratio, recordRatio)
            }
        }
        n += stepSize
    }
}

fun doFactor(n: Long) {
    val factorization = factorGeneric(n).toSortedMap()
    // The sparkline will make the most sense with A025487 numbers...
    val levels = listOf("̲ ", '▁', '▂', '▃', '▄', '▅', '▆', '▇')
    val sparkline = factorization.values.joinToString("") {
        levels[it - 1].toString()
    }
    println("Factors: $sparkline ${factorization.map { (k, v) -> "$k^$v" }.joinToString(" * ")}")
}

fun dieWithUsage() {
    println("""
      Usage:
        ./zaremba single <n>
        ./zaremba records
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
            if (args.size != 1)
                dieWithUsage()
            doRecords()
        }
        "factor" -> {
            if (args.size != 2)
                dieWithUsage()
            doFactor(args[1].toLong())
        }
        else -> dieWithUsage()
    }
}
