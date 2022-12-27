package org.timmc.zaremba

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class WaterfallSequenceTest {
    @Test
    fun waterfallAssertionTest() {
        Waterfall.assertWaterfall(emptyList())
        Waterfall.assertWaterfall(listOf(4))
        Waterfall.assertWaterfall(listOf(4, 4, 4))
        Waterfall.assertWaterfall(listOf(4, 1, 1))

        assertFails {
            Waterfall.assertWaterfall(listOf(1, 1, 4))
            Waterfall.assertWaterfall(listOf(2, 5, 2))
        }
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
            Waterfall.findUpTo(2400.toBigInteger()).map { it.value }.toList()
        )
    }

    @Test
    fun kPrimesTest() {
        // Too small a tau
        assertEquals(
            emptyList(),
            Waterfall.forKPrimesAndMaxTau(3, 2.toBigInteger()).toList()
        )

        val for13 = Waterfall.forKPrimesAndMaxTau(13, 706631.toBigInteger()).toList()
        assertEquals(2070, for13.size)
        for13.forEach {
            assertEquals(13, it.primorialExponents.size)
            assertTrue(it.primorialExponents.last() > 0)
        }

        // Can return elements with exactly this tau, but not more than (≤)
        assertEquals(
            setOf(30, 60, 120, 180, 240), // tau(240) = 20
            Waterfall.forKPrimesAndMaxTau(3, 20.toBigInteger())
                .map { it.value.toInt() }.toSet()
        )
    }

    /** Adapter that accepts a Long. */
    private fun factorWaterfall(n: Long): PrimeExp? {
        return Waterfall.factor(n.toBigInteger())
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
            Waterfall.factor("446286951930693872026828800000".toBigInteger())
        )
    }
}
