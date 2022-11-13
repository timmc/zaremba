package org.timmc

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.system.exitProcess

class PrimesFinder: Iterable<Int> {
    val known = mutableListOf(2)

    fun findNextPrime(): Int {
        synchronized(known) {
            var primeCandidate = known.last() + 1
            while (true) {
                if (known.any { primeCandidate % it == 0 }) {
                    primeCandidate++
                } else {
                    known.add(primeCandidate)
                    return primeCandidate
                }
            }
        }
    }

    override fun iterator(): Iterator<Int> {
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
 * Produce the prime factorization of an integer with non-ascending, contiguous
 * prime factors starting with 2. (OEIS A025487.)
 *
 * If n has prime factorization `p_1^a_1 * p_2^a_2 * ... * p_k^a_k` (where
 * `p_1 < p_2 < p_3 < ... < p_k`) then require that `a_1 >= a_2 >= a_3 >= ... >= a_k`.
 * There must also be no prime between `p_1` and `p_k` that is not in the
 * factorization. Finally, `p_1 = 2`. For example, `10080 = (2^5)(3^2)(5^1)(7^1)`.
 *
 * If these properties hold, return a map of prime factors to their counts.
 * Else, return null.
 */
fun factorA025487(n: Int): Map<Int, Int>? {
    var previousPrimeRepeat = Int.MAX_VALUE
    var remainder = n
    val factors = mutableMapOf<Int, Int>()
    for (prime in primes.iterator()) {
        // Keep dividing n by prime until we can't
        var repeats = 0
        while (remainder % prime == 0) {
            remainder = remainder.div(prime)
            repeats++
        }

        // Check if this fails the "non-ascending" constraint
        if (repeats > previousPrimeRepeat) {
            return null
        }

        if (repeats == 0) {
            if (remainder == 1) {
                // Done factorizing!
                break
            } else {
                // Not done factorizing, but this prime wouldn't divide n, which
                // violates the "contiguous" constraint.
                return null
            }
        }

        // Get ready for next iteration
        factors[prime] = repeats
        previousPrimeRepeat = repeats
    }
    return factors.toMap()
}

/**
 * Cartesian product of zero or more iterables.
 *
 * This is adapted from https://stackoverflow.com/questions/53749357/idiomatic-way-to-create-n-ary-cartesian-product-combinations-of-several-sets-of
 * with comments, variable renamings, and minor formatting changes. Credit is
 * mainly to Erik Huizinga and Tenfour04.
 */
fun <T> Collection<Iterable<T>>.getCartesianProduct(): Set<List<T>> {
    return if (isEmpty()) {
        emptySet()
    } else {
        // Turn the first iterable into a list of single-item lists. Each of
        // these will be the seed that items from remaining iterables will be
        // added onto. This is the beginning of the product.
        val seeds = first().map(::listOf)
        // For each remaining iterable, expand the product by combining each
        // element of the existing product with everything in the iterable.
        drop(1).fold(seeds) { product, nextIterable ->
            product.flatMap { productMember -> nextIterable.map(productMember::plus) }
        }.toSet()
    }
}

/**
 * Given a map of prime divisors to their repeat counts, return all divisors.
 */
fun primesToDivisors(primeFactors: Map<Int, Int>): List<Int> {
    // Make a list of powers lists. E.g. for {2:4, 3:2, 5:1} this would produce
    // [[1,2,4,8,16], [1,3,9], [1,5]]
    val powers = primeFactors.map { (prime, repeat) ->
        generateSequence(1) { it * prime }.take(repeat + 1).toList()
    }
    // Get the Cartesian product, e.g. [[1,1,1], [1,1,5], [1,3,1], [1,3,5]...]
    // and take the product of each sublist. This produces all divisors.
    return powers.getCartesianProduct().map { xs -> xs.reduce(Int::times) }
}

/**
 * Compute z(n) and tau(n), returned as a pair.
 *
 * Assumes that all record-setters will be A025487 numbers, so don't even do
 * those computations if A025487 factorization fails.
 */
fun zarembaAndTau(n: Int): Pair<Double, Int>? {
    val primeFactors = factorA025487(n) ?: return null
    val divisors = primesToDivisors(primeFactors)

    val z = divisors.sumOf { ln(it.toDouble()) / it }
    val tau = divisors.size

    return z to tau
}

fun doSingle(n: Int) {
    val result = zarembaAndTau(n)
    if (result == null) {
        println("Factorization failed (not a member of A025487)")
    } else {
        val (z, tau) = result
        val ratio = z / ln(tau.toDouble())
        println("z($n) = $z\ttau($n) = $tau\tz($n)/ln(tau($n)) = $ratio")
    }
}

fun doRecords(maxN: Int) {
    var recordZ = 0.0
    var recordRatio = 0.0
    for (n in 1..maxN) {
        val (z, tau) = zarembaAndTau(n) ?: continue
        val ratio = z / ln(tau.toDouble())

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
