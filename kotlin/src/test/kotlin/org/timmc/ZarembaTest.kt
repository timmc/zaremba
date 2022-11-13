package org.timmc

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ZarembaTest {
    @Test
    fun primesFinderManual() {
        val primes = PrimesFinder()
        assertEquals(listOf(2), primes.known)
        assertEquals(3, primes.findNextPrime())
        assertEquals(5, primes.findNextPrime())
        assertEquals(7, primes.findNextPrime())
        assertEquals(11, primes.findNextPrime())
        assertEquals(13, primes.findNextPrime())
        assertEquals(17, primes.findNextPrime())
        assertEquals(listOf(2, 3, 5, 7, 11, 13, 17), primes.known)
    }

    @Test
    fun primesFinderIterator() {
        val primes = PrimesFinder()
        assertEquals(listOf(2), primes.known)
        assertEquals(3, primes.findNextPrime())
        assertEquals(5, primes.findNextPrime())

        // Pull an iterator and check it
        assertEquals(
            listOf(2, 3, 5, 7, 11, 13, 17, 19),
            primes.iterator().asSequence().take(8).toList()
        )
        assertEquals(listOf(2, 3, 5, 7, 11, 13, 17, 19), primes.known)

        // Iterate from the beginning again, but not as far (don't extend)
        assertEquals(
            listOf(2, 3, 5, 7),
            primes.iterator().asSequence().take(4).toList()
        )
        assertEquals(listOf(2, 3, 5, 7, 11, 13, 17, 19), primes.known)

        // Iterate farther this time
        assertEquals(
            listOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31),
            primes.iterator().asSequence().take(11).toList()
        )
        assertEquals(listOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31), primes.known)
    }

    @Test
    fun factorA025487() {
        // Edge case
        assertEquals(emptyMap(), factorA025487(1))
        assertEquals(mapOf(2 to 1), factorA025487(2))

        assertEquals(mapOf(2 to 2), factorA025487(4))

        // Start with 2
        assertEquals(null, factorA025487(3))

        // Non-ascending
        assertEquals(mapOf(2 to 4), factorA025487(16))
        assertEquals(mapOf(2 to 4, 3 to 3), factorA025487(16 * 27))
        assertEquals(mapOf(2 to 4, 3 to 4), factorA025487(16 * 81))
        assertEquals(null, factorA025487(8 * 81))

        // Contiguous
        assertEquals(mapOf(2 to 1, 3 to 1, 5 to 1, 7 to 1), factorA025487(42 * 5))
        assertEquals(null, factorA025487(42))
    }

    @Test
    fun cartesianProduct() {
        // Base case: No iterables
        assertEquals(emptySet(), emptyList<List<Int>>().getCartesianProduct())

        // Base case: An iterable is empty
        assertEquals(
            emptySet(),
            listOf(listOf(1, 2, 4), emptyList(), listOf(1, 5)).getCartesianProduct()
        )

        // General case
        assertEquals(
            setOf(
                listOf(1, 1, 1), listOf(2, 1, 1), listOf(4, 1, 1),
                listOf(1, 3, 1), listOf(2, 3, 1), listOf(4, 3, 1),
                listOf(1, 1, 5), listOf(2, 1, 5), listOf(4, 1, 5),
                listOf(1, 3, 5), listOf(2, 3, 5), listOf(4, 3, 5),
            ),
            listOf(listOf(1, 2, 4), listOf(1, 3), listOf(1, 5)).getCartesianProduct()
        )
    }
}