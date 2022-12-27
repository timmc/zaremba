package org.timmc.zaremba

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PrimesTest {
    @Test
    fun primorialsTest() {
        assertEquals(
            listOf<Long>(2, 6, 30).map(Long::toBigInteger),
            Primorials.list.take(3)
        )
    }

    @Test
    fun transposeTest() {
        val primes = listOf(9, 5, 3, 2, 2, 1, 1)
        val primorials = listOf(4, 2, 1, 0, 1, 0, 1)
        assertEquals(primorials, Primes.waterfallToPrimorials(primes))
        assertEquals(primes, Primorials.toPrimes(primorials))
    }

    @Test
    fun unfactorTest() {
        assertEquals(1.toBigInteger(), Primorials.unfactor(emptyList()))
        assertEquals(120.toBigInteger(), Primorials.unfactor(listOf(2, 0, 1)))
    }
}
