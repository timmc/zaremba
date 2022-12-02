package org.timmc

import org.junit.jupiter.api.Test
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
    fun sigmaApproxTest() {
        assertEquals(60.0, sigmaApprox(listOf(3, 1)))
        assertEquals(121.0, sigmaApprox(listOf(0, 4)))
    }

    @Test
    fun hApproxTest() {
        assertEquals(2.3333333333333335, hApprox(12.0, listOf(2, 1)))
    }

    @Test
    fun zarembaSumSigmaTest() {
        // This tests equality of floating point numbers, so it may break if the
        // computation is done in a different order, e.g. if the recomposition
        // of primes into divisors is unstable.
        //
        // Compared to a different computation (zarembaCartesian, just summing
        // divisor/ln(divisor) there's a difference in the last 3 decimal
        // places.
        assertEquals(1.0114042647073518, zaremba(6.toBigInteger(), listOf(1, 1)))
        assertEquals(1.5650534091363244, zaremba(12.toBigInteger(), listOf(2, 1)))
        assertEquals(1.957402511444135, zaremba(24.toBigInteger(), listOf(3, 1)))
        assertEquals(2.211339327644702, zaremba(48.toBigInteger(), listOf(4, 1)))
        assertEquals(20.507394183515835, zaremba(
            "55203167814376982400".toBigInteger(),
            listOf(7,5,2,2,1,1,1,1,1,1,1,1,1)
        ))
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
                z = 14.960783769593887, isZRecord = true,
                v = 1.6976114329564411, isVRecord = false,
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
