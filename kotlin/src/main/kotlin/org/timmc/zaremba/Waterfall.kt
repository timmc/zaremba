package org.timmc.zaremba

import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import kotlin.system.measureNanoTime
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * Intentionally does not have a base class n/value/product member since that
 * would allow someone to accidentally call `map { it.value }` over a sequence
 * of search results rather than filtering out restart points first.
 */
sealed class WaterfallSearchResult {
    companion object {
        fun separateResults(results: Sequence<WaterfallSearchResult>):
            Pair<List<WaterfallNumber>, List<WaterfallRestart>> {
            val numbers = mutableListOf<WaterfallNumber>()
            val restarts = mutableListOf<WaterfallRestart>()
            results.forEach {
                when (it) {
                    is WaterfallNumber -> numbers.add(it)
                    is WaterfallRestart -> restarts.add(it)
                }
            }
            return numbers.toList() to restarts.toList()
        }
    }
}

/**
 * A waterfall number, expressed both as its value and as its decomposition into
 * powers of primorials.
 */
data class WaterfallNumber(
    /**
     * The actual value of the waterfall number.
     */
    val value: BigInteger,

    /**
     * Representation of the number as the product of repeated primorials. This
     * is a list of exponents. For example, [3, 1] would mean 2^3 * 6^1.
     *
     * If this does not have trailing zeroes, then it is a unique representation
     * of the [value].
     */
    val primorialExponents: PrimorialExp,
): WaterfallSearchResult()

/**
 * A point where the search stopped, with enough information to restart it.
 *
 * The structure and semantics of this class are tightly coupled to the logic of
 * [Waterfall.findUntil].
 */
data class WaterfallRestart(
    /**
     * The waterfall number reaching or exceeding the limit. This matches the
     * value of `exponents.plus(restartExp)`.
     */
    val product: BigInteger,

    /**
     * The is the base onto which more primorials will be added during
     * recursion.
     */
    val exponents: PrimorialExp,

    /**
     * Where to restart in searching for higher exponents.
     *
     * If this is null, then the restart point indicates a complete restart --
     * all recursive calls starting at this product/exponents pair.
     *
     * If this is non-null, then higher primorials have already been explored
     * and the search should instead restart at this exponent of the *current*
     * primorial.
     */
    val restartExp: Int?,
): WaterfallSearchResult()

object Waterfall {
    /**
     * Throw if a list of prime exponents does not represent a waterfall number.
     */
    fun assertWaterfall(exps: PrimeExp) {
        if (!exps.zipWithNext { a, b -> a >= b }.all { it }) {
            throw AssertionError("Prime exponents failed waterfall test: $exps")
        }
    }

    /**
     * Recursive core of [findAll]. Given exponents of the first few primorial
     * numbers, accompanied by a matching product, yield waterfall numbers that
     * are multiples of that base by various higher primorials -- as well as
     * restart points when the limit is reached or exceeded.
     *
     * The output is not sorted.
     *
     * Explores in two directions:
     *
     * - "Rightwards" by setting the next primorial's exponent to zero and
     *   using that as a new base, and
     * - "Upwards" by setting the next primorial's exponent to 1, 2, etc. and
     *   using those as bases.
     *
     * If [restartExp] is set, then rightwards exploration is disabled (it is
     * assumed to have already been covered in a previous run) and upwards
     * exploration restarts at the given exponent.
     *
     * Invariants:
     *
     * - [baseProduct] is less than [limit]
     * - [baseProduct] is the product of [baseExp] plus [restartExp] (or just
     *   [baseExp] if [restartExp] is null)
     * - When a waterfall number below the limit is found it is first yielded
     *   and then explored as a base.
     *
     * The starting product is *not* yielded, as one can always add more zeroes
     * onto the end and yield the same number over and over. It is assumed that
     * the caller has already yielded that number -- in practice, this function
     * will do so as appropriate for any restart points created, as will
     * [findAll] when it uses [searchBaseStart] and [searchBaseResult].
     */
    fun findUntil(
        limit: BigInteger, baseProduct: BigInteger, baseExp: PrimorialExp,
        restartExp: Int?,
    ): Sequence<WaterfallSearchResult> {
        return sequence {
            // The first primorial that isn't represented in baseExp.
            val factor = Primorials.list[baseExp.size]

            // Skip this if we're continuing from the restart point inside the
            // up-loop -- a restart point bearing a restart-exponent has
            // already explored this direction.
            if (restartExp == null) {
                if (factor < limit) {
                    // Explore "to the right", with larger primorials. Note that this
                    // branch starts with a factor of zero, which would be a duplicate
                    // of the base product here, so we don't yield the current
                    // product.
                    yieldAll(findUntil(limit, baseProduct, baseExp.plus(0), null))
                } else {
                    // Don't explore to the right if the new primorial is
                    // already over the limit, otherwise we'd keep adding zeroes
                    // forever. Just save the current call as a restart point
                    // and exit.
                    yield(WaterfallRestart(baseProduct, baseExp, null))
                    return@sequence
                }
            }

            // Explore "up", with larger exponents
            var nextProduct = baseProduct
            var exponent = 0

            if (restartExp == null) {
                // Start at p^1 to avoid duplicates
                nextProduct *= factor
                exponent += 1
            } else {
                // When restarting, start the loop here. baseProduct must
                // already incorporate this value, so don't adjust nextProduct.
                exponent = restartExp
            }
            while (true) {
                if (nextProduct < limit) {
                    val nextExp = baseExp.plus(exponent)
                    yield(WaterfallNumber(nextProduct, nextExp))
                    yieldAll(findUntil(limit, nextProduct, nextExp, null))
                } else {
                    // Too high, save this off for the next round
                    yield(WaterfallRestart(nextProduct, baseExp, exponent))
                    break
                }

                nextProduct *= factor
                exponent += 1
            }
        }
    }

    /**
     * Helper for [findUntil] that accepts a restart point. Does not yield the
     * restart point's product itself, even if it is within the limit.
     */
    fun findUntil(limit: BigInteger, cont: WaterfallRestart): Sequence<WaterfallSearchResult> {
        return findUntil(limit, cont.product, cont.exponents, cont.restartExp)
    }

    /**
     * Search up to a new limit from a collection of restart points.
     */
    fun findFromRestartsUntil(
        limit: BigInteger, restarts: List<WaterfallRestart>
    ): Sequence<WaterfallSearchResult> {
        return sequence {
            restarts.forEach { wc ->
                if (wc.product < limit) {
                    yieldAll(findUntil(limit, wc))
                } else {
                    // Still not time for this one yet
                    yield(wc)
                }
            }
        }
    }

    // 1 is special -- it will never be produced by the search function,
    // since it is all-zero exponents.
    internal val searchBaseResult = WaterfallNumber(BigInteger.ONE, emptyList())
    internal val searchBaseStart = WaterfallRestart(BigInteger.ONE, emptyList(), null)

    /**
     * Implementation of [findAll], allowing the caller to choose the
     * progression of exclusive upper bound for each batch.
     */
    @OptIn(ExperimentalTime::class)
    internal fun findAll(limits: Sequence<BigInteger>): Sequence<WaterfallNumber> {
        return sequence {
            yield(searchBaseResult)
            var restarts = listOf(searchBaseStart)

            for (limit in limits) {
                debug { "Waterfall search n<$limit" }
                val (searchResults, searchDuration) = measureTimedValue {
                    WaterfallSearchResult.separateResults(
                        findFromRestartsUntil(limit, restarts)
                    )
                }
                val (found, deferred) = searchResults
                val sortNanos = measureNanoTime {
                    yieldAll(found.sortedBy(WaterfallNumber::value))
                }
                restarts = deferred
                debug {
                    val foundN = found.size
                    val deferredN = deferred.size
                    val total = foundN + deferredN
                    val searchTime = String.format("%.3f", searchDuration.inWholeMilliseconds/1000.0)
                    val sortTime = String.format("%.3f", sortNanos/1_000_000_000.0)
                    "Waterfall batch #found=$foundN #deferred=$deferredN fraction_found=${foundN*1.0/total} search_time=$searchTime sort_time=$sortTime"
                }
            }
        }
    }

    /**
     * Produce an infinite sequence of waterfall numbers in ascending order,
     * starting with 1.
     *
     * stepSize allows controlling how fine-grained to make the batched search.
     * Higher numbers mean less reprocessing of the restart list, but lower
     * numbers mean a smoother output, less demand on memory, and shorter unit
     * test runs. 1e28 seems to give a reasonable compromise for regular use.
     */
    fun findAll(stepSize: BigInteger): Sequence<WaterfallNumber> {
        // An arithmetic progression keeps the required working memory from
        // growing without bound. On each round, only a small number of results
        // will be found, and most of the restart points will be deferred until
        // the next round.
        return findAll(generateSequence(stepSize) { it + stepSize })
    }

    /**
     * Produce the prime factorization of a waterfall number.
     *
     * TODO: Switch to factoring into primorials instead? Not needed for z(n)
     *   calculation any more, but would be faster if that's needed at some
     *   point.
     *
     * @return a map of prime factors to their counts, or null if not a waterfall
     *   number
     */
    fun factor(n: BigInteger): PrimeExp? {
        // Running remainder, and factors so far
        var remainder = n
        val factors = mutableListOf<Int>()
        var previousPrimeRepeat = Int.MAX_VALUE // used for waterfall checks
        var factored = false

        for (prime in Primes.list) {
            // Keep dividing n by prime until we can't
            var repeats = 0
            while (remainder.mod(prime) == BigInteger.ZERO) {
                remainder /= prime
                repeats++
            }

            // Check if this fails the "non-ascending" constraint
            if (repeats > previousPrimeRepeat)
                return null

            if (repeats == 0) {
                // This prime wasn't a factor at all. Check what that means:
                if (remainder == BigInteger.ONE) {
                    factored = true
                    break
                } else {
                    // Violates the "contiguous" constraint.
                    return null
                }
            } else {
                factors.add(repeats)
            }

            previousPrimeRepeat = repeats
        }

        if (!factored)
            throw AssertionError("Ran out of primes when factoring")

        return factors.toList()
    }
}
