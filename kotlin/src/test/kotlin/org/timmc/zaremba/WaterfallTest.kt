package org.timmc.zaremba

import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFails

class WaterfallTest {
    @Test
    fun assertWaterfallTest() {
        Waterfall.assertWaterfall(emptyList())
        Waterfall.assertWaterfall(listOf(4))
        Waterfall.assertWaterfall(listOf(1, 1, 1, 1))
        Waterfall.assertWaterfall(listOf(4, 1, 1, 1))
        assertFails { Waterfall.assertWaterfall(listOf(1, 1, 1, 2)) }
        assertFails { Waterfall.assertWaterfall(listOf(1, 2, 2, 2)) }
    }

    @Test
    fun findUntilTest() {
        // Expected values, in the actual order of recursion
        assertEquals(
            listOf(
                WaterfallRestart(1.toBigInteger(),  listOf(0, 0), null),
                WaterfallNumber(6.toBigInteger(),  listOf(0, 1)),
                WaterfallRestart(6.toBigInteger(), listOf(0, 1), null),
                WaterfallRestart(36.toBigInteger(),  listOf(0), 2),
                WaterfallNumber(2.toBigInteger(),  listOf(1)),
                WaterfallRestart(2.toBigInteger(),  listOf(1, 0), null),
                WaterfallRestart(12.toBigInteger(),  listOf(1), 1),
                WaterfallNumber(4.toBigInteger(),  listOf(2)),
                WaterfallRestart(4.toBigInteger(), listOf(2, 0), null),
                WaterfallRestart(24.toBigInteger(),  listOf(2), 1),
                WaterfallNumber(8.toBigInteger(),  listOf(3)),
                WaterfallRestart(8.toBigInteger(), listOf(3, 0), null),
                WaterfallRestart(48.toBigInteger(),  listOf(3), 1),
                WaterfallRestart(16.toBigInteger(),  listOf(), 4),
            ),
            Waterfall.findUntil(9.toBigInteger(), BigInteger.ONE, emptyList(), null)
                .toList()
        )

        // Bounds testing is <, not <=
        assertEquals(
            listOf(
                WaterfallRestart(1.toBigInteger(),  listOf(0, 0), null),
                WaterfallNumber(6.toBigInteger(),  listOf(0, 1)),
                WaterfallRestart(6.toBigInteger(), listOf(0, 1), null),
                WaterfallRestart(36.toBigInteger(),  listOf(0), 2),
                WaterfallNumber(2.toBigInteger(),  listOf(1)),
                WaterfallRestart(2.toBigInteger(),  listOf(1, 0), null),
                WaterfallRestart(12.toBigInteger(),  listOf(1), 1),
                WaterfallNumber(4.toBigInteger(),  listOf(2)),
                WaterfallRestart(4.toBigInteger(), listOf(2, 0), null),
                WaterfallRestart(24.toBigInteger(),  listOf(2), 1),
                WaterfallRestart(8.toBigInteger(),  listOf(), 3),
            ),
            Waterfall.findUntil(8.toBigInteger(), Waterfall.searchBaseStart).toList()
        )

        // Restarting from inside the loop
        assertEquals(
            listOf(
                WaterfallNumber(8.toBigInteger(),  listOf(3)),
                WaterfallRestart(8.toBigInteger(),  listOf(3, 0), null),
                WaterfallRestart(48.toBigInteger(),  listOf(3), 1),
                WaterfallRestart(16.toBigInteger(),  listOf(), 4),
            ),
            Waterfall.findUntil(
                limit = 10.toBigInteger(), baseProduct = 8.toBigInteger(),
                baseExp = emptyList(), restartExp = 3
            ).toList()
        )
    }

    @Test
    fun findFromRestartsUntilTest() {
        assertEquals(
            listOf(
                // 1 not included, since it's before the search root
                WaterfallRestart(1.toBigInteger(),  listOf(0), null),
                WaterfallNumber(2.toBigInteger(), listOf(1)),
                WaterfallRestart(2.toBigInteger(), listOf(1), null),
                WaterfallNumber(4.toBigInteger(), listOf(2)),
                WaterfallRestart(4.toBigInteger(), listOf(2), null),
                WaterfallRestart(8.toBigInteger(),  listOf(), 3),
            ),
            Waterfall.findFromRestartsUntil(
                5.toBigInteger(), listOf(Waterfall.searchBaseStart)
            ).toList()
        )
    }

    @Test
    fun findAllTest() {
        // From https://oeis.org/A025487
        val first54 = listOf(
            1, 2, 4, 6, 8, 12, 16, 24, 30, 32, 36, 48, 60, 64, 72, 96, 120,
            128, 144, 180, 192, 210, 216, 240, 256, 288, 360, 384, 420, 432,
            480, 512, 576, 720, 768, 840, 864, 900, 960, 1024, 1080, 1152,
            1260, 1296, 1440, 1536, 1680, 1728, 1800, 1920, 2048, 2160,
            2304, 2310
        ).map { it.toBigInteger() }

        // Explicit limits
        assertEquals(
            listOf(
                1, 2, 4, 6, 8, 12, 16, 24, 30, 32
            ).map { it.toBigInteger() },
            Waterfall.findAll(
                sequenceOf(5.toBigInteger(), 35.toBigInteger())
            ).map { it.value }.toList()
        )

        // Auto-stepping
        assertEquals(
            first54,
            Waterfall.findAll(stepSize = 100.toBigInteger())
                .take(54).map { it.value }.toList()
        )

        // Boundary conditions check
        (1..16).forEach { stepSize ->
            assertEquals(
                first54,
                Waterfall.findAll(stepSize.toBigInteger())
                    .take(54).map { it.value }.toList()
            )
        }
    }

    /** Adapter that accepts a Long. */
    private fun factorWaterfall(n: Long): PrimeExp? {
        return Waterfall.factor(n.toBigInteger())
    }

    @Test
    fun factorTest() {
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
