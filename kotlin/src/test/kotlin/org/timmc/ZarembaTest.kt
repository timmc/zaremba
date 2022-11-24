package org.timmc

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ZarembaTest {
    @Test
    fun primesFinderTestManual() {
        val primes = PrimesFinder()
        assertEquals(listOf(2L), primes.known)
        assertEquals(3, primes.findNextPrime())
        assertEquals(5, primes.findNextPrime())
        assertEquals(7, primes.findNextPrime())
        assertEquals(11, primes.findNextPrime())
        assertEquals(13, primes.findNextPrime())
        assertEquals(17, primes.findNextPrime())
        assertEquals(listOf(2L, 3, 5, 7, 11, 13, 17), primes.known)
    }

    @Test
    fun primesFinderTestIterator() {
        val primes = PrimesFinder()
        assertEquals(listOf(2L), primes.known)
        assertEquals(3, primes.findNextPrime())
        assertEquals(5, primes.findNextPrime())

        // Pull an iterator and check it
        assertEquals(
            listOf(2L, 3, 5, 7, 11, 13, 17, 19),
            primes.iterator().asSequence().take(8).toList()
        )
        assertEquals(listOf(2L, 3, 5, 7, 11, 13, 17, 19), primes.known)

        // Iterate from the beginning again, but not as far (don't extend)
        assertEquals(
            listOf(2L, 3, 5, 7),
            primes.iterator().asSequence().take(4).toList()
        )
        assertEquals(listOf(2L, 3, 5, 7, 11, 13, 17, 19), primes.known)

        // Iterate farther this time
        assertEquals(
            listOf(2L, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31),
            primes.iterator().asSequence().take(11).toList()
        )
        assertEquals(listOf(2L, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31), primes.known)
    }

    @Test
    fun factorGenericTest() {
        assertEquals(emptyMap(), factorGeneric(1))
        assertEquals(mapOf(7L to 1), factorGeneric(7))
        assertEquals(mapOf(3L to 1, 7L to 2), factorGeneric(147))
        assertEquals(mapOf(5573L to 1, 59399L to 1, 71069L to 1), factorGeneric(23526015630263))
    }

    @Test
    fun factorA025487Test() {
        // Edge case
        assertEquals(emptyMap(), factorA025487(1))
        assertEquals(mapOf(2L to 1), factorA025487(2))

        assertEquals(mapOf(2L to 2), factorA025487(4))

        // Start with 2
        assertEquals(null, factorA025487(3))

        // Non-ascending
        assertEquals(mapOf(2L to 4), factorA025487(16))
        assertEquals(mapOf(2L to 4, 3L to 3), factorA025487(16 * 27))
        assertEquals(mapOf(2L to 4, 3L to 4), factorA025487(16 * 81))
        assertEquals(null, factorA025487(8 * 81))

        // Contiguous
        assertEquals(mapOf(2L to 1, 3L to 1, 5L to 1, 7L to 1), factorA025487(42 * 5))
        assertEquals(null, factorA025487(42))
    }

    @Test
    fun cartesianProductTest() {
        // Base case: No iterables
        assertEquals(emptyList(), emptyList<List<Int>>().getCartesianProduct())

        // Trivial case: An iterable is empty
        assertEquals(
            emptyList(),
            listOf(listOf(1, 2, 4), emptyList(), listOf(1, 5)).getCartesianProduct()
        )

        // Trivial case: All single-element
        assertEquals(
            listOf(listOf(1, 2, 3)),
            listOf(listOf(1), listOf(2), listOf(3)).getCartesianProduct()
        )

        // General case
        assertEquals(
            listOf(
                listOf(1, 1, 1), listOf(1, 1, 5), listOf(1, 3, 1), listOf(1, 3, 5),
                listOf(2, 1, 1), listOf(2, 1, 5), listOf(2, 3, 1), listOf(2, 3, 5),
                listOf(4, 1, 1), listOf(4, 1, 5), listOf(4, 3, 1), listOf(4, 3, 5),
            ),
            listOf(listOf(1, 2, 4), listOf(1, 3), listOf(1, 5)).getCartesianProduct()
        )
    }

    @Test
    fun primesToDivisorsTest() {
        // Not currently supported!
        //assertEquals(listOf(1L), primesToDivisors(mapOf()))

        assertEquals(setOf<Long>(1, 7), primesToDivisors(mapOf(7L to 1)).toSet())
        assertEquals(
            setOf<Long>(1, 2, 5, 10),
            primesToDivisors(mapOf(2L to 1, 5L to 1)).toSet()
        )
        assertEquals(
            setOf<Long>(1, 3, 5, 9, 15, 25, 45, 75, 225),
            primesToDivisors(mapOf(3L to 2, 5L to 2)).toSet()
        )
        assertEquals(
            setOf<Long>(1, 3, 5, 7, 9, 15, 21, 35, 45, 63, 105, 315),
            primesToDivisors(mapOf(3L to 2, 5L to 1, 7L to 1)).toSet()
        )
    }

    @Test
    fun zarembaAndTauTest() {
        // This tests equality of floating point numbers, so it may break if the
        // computation is done in a different order, e.g. if the recomposition
        // of primes into divisors is unstable.
        assertEquals(1.0114042647073518 to 4, zarembaAndTau(mapOf(2L to 1, 3L to 1)))
    }

    @Test
    fun stepSizeAfterRecordZTest() {
        val z360 = zarembaAndTau(factorGeneric(360)).first
        assertEquals(30, stepSizeAfterRecordZ(z360))
    }
}
