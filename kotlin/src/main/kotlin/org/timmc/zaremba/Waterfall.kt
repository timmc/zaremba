package org.timmc.zaremba

import java.math.BigInteger

data class WaterfallNumber(
    val value: BigInteger,
    val primorialExponents: PrimorialExp,
)

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
     * Recursive core of [findUpTo]. Given a list of exponents of lower primorial
     * numbers, accompanied by a matching product, yield waterfall numbers that
     * are multiples of that base and various powers of the first primorial in
     * the list of remaining ones.
     *
     * Explores in two directions: Higher powers of the current primorial, and in
     * recursive calls to the next primorial (one for each power of this one, plus
     * one.)
     *
     * The output is not sorted.
     */
    private fun findUpToFrom(
        primorials: List<BigInteger>, maxN: BigInteger,
        baseExp: PrimorialExp, baseProduct: BigInteger
    ): Sequence<WaterfallNumber> {
        return sequence {
            // If we ever reach the end of the list *before* we find a primorial
            // that is larger than N, raise an exception -- we need a larger list
            // of prime numbers!
            if (primorials.isEmpty())
                throw AssertionError("Ran out of primorials")

            val factor = primorials[0]
            if (factor > maxN)
                return@sequence

            val nextPrimorials = primorials.drop(1)
            yieldAll(findUpToFrom(nextPrimorials, maxN, baseExp.plus(0), baseProduct))

            var nextProduct = baseProduct
            var exponent = 0

            while (true) {
                nextProduct = factor * nextProduct
                if (nextProduct > maxN)
                    break
                exponent += 1

                val nextExp = baseExp.plus(exponent)
                yield(WaterfallNumber(
                    value = nextProduct,
                    primorialExponents = nextExp
                ))
                yieldAll(findUpToFrom(nextPrimorials, maxN, nextExp, nextProduct))
            }
        }
    }

    /**
     * Produce a sorted sequence of all the waterfall numbers up to N, starting with
     * 1, along with their factorization as primorial exponents.
     */
    fun findUpTo(maxN: BigInteger): Sequence<WaterfallNumber> {
        val all = findUpToFrom(Primorials.list, maxN, emptyList(), BigInteger.ONE)
            .plus(WaterfallNumber(value = BigInteger.ONE, primorialExponents = emptyList()))
        return all.sortedBy(WaterfallNumber::value)
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
