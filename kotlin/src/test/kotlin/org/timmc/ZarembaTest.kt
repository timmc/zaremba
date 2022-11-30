package org.timmc

import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class ZarembaTest {
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

    /** Adapter that accepts a Long. */
    private fun factorWaterfall(n: Long): List<Int>? {
        return factorWaterfall(n.toBigInteger())
    }

    @Test
    fun factorWaterfallTest() {
        // Edge case
        assertEquals(emptyList(), factorWaterfall(1))
        assertEquals(listOf(1), factorWaterfall(2))

        assertEquals(listOf(2), factorWaterfall(4))

        // Start with 2
        assertEquals(null, factorWaterfall(3))

        // Non-ascending
        assertEquals(listOf(4), factorWaterfall(16))
        assertEquals(listOf(4, 3), factorWaterfall(16 * 27))
        assertEquals(listOf( 4, 4), factorWaterfall(16 * 81))
        assertEquals(null, factorWaterfall(8 * 81))

        // Contiguous
        assertEquals(listOf(1, 1, 1, 1), factorWaterfall(42 * 5))
        assertEquals(null, factorWaterfall(42))

        // Large
        assertEquals(
            listOf(17, 9, 5, 3, 3, 3, 2, 2, 2),
            factorWaterfall("446286951930693872026828800000".toBigInteger())
        )
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
        fun subject(primes: List<Int>): Set<Long> =
            primesToDivisors(primes).map(BigInteger::toLong).toSet()

        // Not currently supported!
        //assertEquals(listOf(1L), primesToDivisors(mapOf()))

        assertEquals(setOf<Long>(1, 7), subject(listOf(0, 0, 0, 1)))
        assertEquals(
            setOf<Long>(1, 2, 5, 10),
            subject(listOf(1, 0, 1))
        )
        assertEquals(
            setOf<Long>(1, 3, 5, 9, 15, 25, 45, 75, 225),
            subject(listOf(0, 2, 2))
        )
        assertEquals(
            setOf<Long>(1, 3, 5, 7, 9, 15, 21, 35, 45, 63, 105, 315),
            subject(listOf(0, 2, 1, 1))
        )
    }

    @Test
    fun zarembaTest() {
        // This tests equality of floating point numbers, so it may break if the
        // computation is done in a different order, e.g. if the recomposition
        // of primes into divisors is unstable.
        assertEquals(1.0114042647073518, zaremba(listOf(1, 1)))
    }

    @Test
    fun primesToTauTest() {
        assertEquals(1, primesToTau(emptyList()))
        assertEquals(20, primesToTau(listOf(1, 1, 4)))
    }

    @Test
    fun findRecordsZTest() {
        assertEquals(
            listOf(
                RecordSetter(
                    n = "4", tau = 3,
                    z = 0.6931471805599453, isZRecord = true,
                    v = 0.6309297535714574, isVRecord = true,
                    primes = listOf(2), primorials = listOf(2),
                ),
                RecordSetter(
                    n = "6", tau = 4,
                    z = 1.0114042647073518, isZRecord = true,
                    v = 0.7295739585136225, isVRecord = true,
                    primes = listOf(1, 1), primorials = listOf(0, 1),
                ),
            ),
            findRecords(10.toBigInteger()).toList()
        )

        // Not independently verified, just grabbed from output of a previous
        // run -- this will only catch changes in behavior.
        assertEquals(
            RecordSetter(
                n = "963761198400", tau = 6720,
                z = 14.960783769593917, isZRecord = true,
                v = 1.6976114329564445, isVRecord = false,
                primes = listOf(6, 4, 2, 1, 1, 1, 1, 1, 1),
                primorials = listOf(2, 2, 1, 0, 0, 0, 0, 0, 1)
            ),
            findRecords(1000000000000.toBigInteger()).last()
        )
    }

    @Test
    fun waterfallFindTest() {
        // From https://oeis.org/A025487
        assertEquals(
            listOf(
                1, 2, 4, 6, 8, 12, 16, 24, 30, 32, 36, 48, 60, 64, 72, 96, 120,
                128, 144, 180, 192, 210, 216, 240, 256, 288, 360, 384, 420, 432,
                480, 512, 576, 720, 768, 840, 864, 900, 960, 1024, 1080, 1152,
                1260, 1296, 1440, 1536, 1680, 1728, 1800, 1920, 2048, 2160,
                2304, 2310
            ).map { it.toBigInteger() },
            findWaterfall(2400.toBigInteger()).map { it.value }.toList()
        )
    }
}
