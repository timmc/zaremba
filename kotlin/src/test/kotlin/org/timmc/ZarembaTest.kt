package org.timmc

import org.junit.jupiter.api.Test
import kotlin.math.ln
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertEquals(emptyList(), emptyList<List<Int>>().getCartesianProduct().toList())

        // Trivial case: An iterable is empty
        assertEquals(
            emptyList(),
            listOf(listOf(1, 2, 4), emptyList(), listOf(1, 5)).getCartesianProduct().toList()
        )

        // Trivial case: All single-element
        assertEquals(
            listOf(listOf(1, 2, 3)),
            listOf(listOf(1), listOf(2), listOf(3)).getCartesianProduct().toList()
        )

        // General case
        assertEquals(
            listOf(
                listOf(1, 1, 1), listOf(1, 1, 5), listOf(1, 3, 1), listOf(1, 3, 5),
                listOf(2, 1, 1), listOf(2, 1, 5), listOf(2, 3, 1), listOf(2, 3, 5),
                listOf(4, 1, 1), listOf(4, 1, 5), listOf(4, 3, 1), listOf(4, 3, 5),
            ),
            listOf(listOf(1, 2, 4), listOf(1, 3), listOf(1, 5)).getCartesianProduct().toList()
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
        assertEquals(30, zStep(z360))
    }

    @Test
    fun nonAscendingTest() {
        assertTrue(nonAscending(emptyList()))
        assertTrue(nonAscending(listOf(4)))
        assertTrue(nonAscending(listOf(4, 4, 4, 3)))

        assertFalse(nonAscending(listOf(4, 4, 4, 8, 4)))
    }

    @Test
    fun minTauExponentsTest() {
        assertEquals(
            setOf(
                listOf(5, 1), listOf(5, 2), listOf(5, 3), listOf(5, 4), listOf(5, 5),
                listOf(4, 1), listOf(4, 2), listOf(4, 3), listOf(4, 4),
                listOf(3, 1), listOf(3, 2), listOf(3, 3),
                listOf(2, 1), listOf(2, 2),
                listOf(1, 1),
            ),
            minTauExponents(n=72, primesK=2).toSet()
        )
    }

    @Test
    fun unfactorLogTest() {
        assertEquals(0.0, unfactorLog(emptyList()))
        assertEquals(ln(120.0), unfactorLog(listOf(3, 1, 1)))
    }

    @Test
    fun primesToTauTest() {
        assertEquals(1, primesToTau(emptyList()))
        assertEquals(20, primesToTau(listOf(1, 1, 4)))
    }

    @Test
    fun minTauTest() {
        // primesK=0 not supported
        //assertEquals(1, minTau(2, 0))

        assertEquals(3, minTau(4, 1))
        assertEquals(12, minTau(72, 2))
    }

    @Test
    fun stepSizeAfterRecordVTest() {
        assertEquals(3, vStepPk(n=1102701600, recordV=1.6546758215862982, vStepPkLast=2))
    }

    @Test
    fun minStepTest() {
        assertEquals(30, minStep(30, 30))
        assertEquals(30, minStep(30, 210))
        assertEquals(30, minStep(210, 30))

        val t = assertFails { minStep(30, 40) }
        assertEquals("Assuming stepA divides stepB or vice versa", t.message)
    }

    @Test
    fun findRecordsTest() {
        assertEquals(
            listOf(
                RecordSetter(n=4, z=0.6931471805599453, v=0.6309297535714574, type="both", tau=3, stepSize=2, stepSizeFromV = 2),
                RecordSetter(n=6, z=1.0114042647073518, v=0.7295739585136225, type="both", tau=4, stepSize=2, stepSizeFromV = 2),
                RecordSetter(n=12, z=1.5650534091363246, v=0.8734729387592397, type="both", tau=6, stepSize=6, stepSizeFromV = 6),
            ),
            findRecords().asSequence().take(3).toList()
        )
    }
}
