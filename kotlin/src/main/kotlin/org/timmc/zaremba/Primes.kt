package org.timmc.zaremba

import java.math.BigInteger

object Primes {
    /**
     * From https://oeis.org/A000040 -- add more as needed.
     */
    val list = arrayOf(
        2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
        73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
        157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233,
        239, 241, 251, 257, 263, 269, 271,
    ).map { it.toBigInteger() }

    /**
     * Convert a list of prime factor exponents into primorial exponents.
     */
    fun toPrimorials(primeExponents: List<Int>): PrimorialExponents {
        return (primeExponents + listOf(0)).zipWithNext(Int::minus)
    }
}

/**
 * Primorial numbers are the product of the first k primes: 2, 6, 30...
 * https://oeis.org/A002110
 */
object Primorials {
    /**
     * primorials.get(k) == (k+1)th primorial number
     */
    val list = Primes.list
        .runningFold(BigInteger.ONE, BigInteger::multiply)
        .drop(1)

    /**
     * Get the nth primorial (1-based indexing).
     */
    fun nth(n: Int): BigInteger {
        return if (n == 0)
            BigInteger.ONE
        else
            list[n - 1]
    }

    /**
     * Convert a list of primorial factor exponents into prime exponents.
     */
    fun toPrimes(primorialExponents: PrimorialExponents): List<Int> {
        return primorialExponents.asReversed().runningReduce(Int::plus).reversed()
    }
}

/**
 * Representation of a waterfall number as a list of exponents of the first k
 * primorial numbers. For example, [0, 1, 3] = 2^0 * 6^1 * 30^3
 */
typealias PrimorialExponents = List<Int>
