package org.timmc.zaremba

import java.math.BigInteger

/**
 * Representation of a waterfall number as a list of exponents of the first k
 * primorial numbers. For example, [0, 1, 3] = 2^0 * 6^1 * 30^3
 */
typealias PrimorialExponents = List<Int>

/**
 * From https://oeis.org/A000040 -- add more as needed.
 */
val primes = arrayOf(
    2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
    73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
    157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233,
    239, 241, 251, 257, 263, 269, 271,
).map { it.toBigInteger() }

/**
 * primorials.get(k) == (k+1)th primorial number: 2, 6, 30, ...
 */
val primorials = primes
    .runningFold(BigInteger.ONE, BigInteger::multiply)
    .drop(1)

/**
 * Get the nth primorial: The product of the first [n] primes
 */
fun nthPrimorial(n: Int): BigInteger {
    return if (n == 0)
        BigInteger.ONE
    else
        primorials[n - 1]
}

/**
 * Convert a list of prime factor exponents into primorial exponents.
 */
fun transposePrimesToPrimorials(primeExponents: List<Int>): PrimorialExponents {
    return (primeExponents + listOf(0)).zipWithNext(Int::minus)
}

/**
 * Convert a list of primorial factor exponents into prime exponents.
 */
fun transposePrimorialsToPrimes(primorialExponents: PrimorialExponents): List<Int> {
    return primorialExponents.asReversed().runningReduce(Int::plus).reversed()
}
