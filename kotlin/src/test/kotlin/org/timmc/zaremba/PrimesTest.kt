package org.timmc.zaremba

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PrimesTest {
    @Test
    fun primorialsTest() {
        assertEquals(
            listOf<Long>(2, 6, 30).map(Long::toBigInteger),
            primorials.take(3)
        )
    }

    @Test
    fun primorialsAndPrimesTest() {
        val primes = listOf(9, 5, 3, 2, 2, 1, 1)
        val primorials = listOf(4, 2, 1, 0, 1, 0, 1)
        assertEquals(primorials, transposePrimesToPrimorials(primes))
        assertEquals(primes, transposePrimorialsToPrimes(primorials))
    }
}
