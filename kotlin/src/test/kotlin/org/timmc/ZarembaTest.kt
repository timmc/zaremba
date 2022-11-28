package org.timmc

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZarembaTest {
    @Test
    fun primesRunningProductTest() {
        assertEquals(
            listOf<Long>(2, 6, 30),
            primesRunningProduct.take(3)
        )
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

        // Seed that leaves a "hole" with zero-repeats. This tests the logic
        // around picking up the seed's exponents in the repeats==0 logic.
        // 391287046550400: [7 4 2 2 1 1 1 1 1 1]
        // 58198140:        [2 2 1 1 1 1 1 1]
        // Remainder:       [5 2 1 1 0 0 0 0 1 1]
        assertEquals(
            listOf(7, 4, 2, 2, 1, 1, 1, 1, 1, 1),
            factorWaterfall(391287046550400, 58198140, listOf(2, 2, 1, 1, 1, 1, 1, 1))
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
        // Not currently supported!
        //assertEquals(listOf(1L), primesToDivisors(mapOf()))

        assertEquals(setOf<Long>(1, 7), primesToDivisors(listOf(0, 0, 0, 1)).toSet())
        assertEquals(
            setOf<Long>(1, 2, 5, 10),
            primesToDivisors(listOf(1, 0, 1)).toSet()
        )
        assertEquals(
            setOf<Long>(1, 3, 5, 9, 15, 25, 45, 75, 225),
            primesToDivisors(listOf(0, 2, 2)).toSet()
        )
        assertEquals(
            setOf<Long>(1, 3, 5, 7, 9, 15, 21, 35, 45, 63, 105, 315),
            primesToDivisors(listOf(0, 2, 1, 1)).toSet()
        )
    }

    @Test
    fun zarembaAndTauTest() {
        // This tests equality of floating point numbers, so it may break if the
        // computation is done in a different order, e.g. if the recomposition
        // of primes into divisors is unstable.
        assertEquals(1.0114042647073518 to 4, zarembaAndTau(listOf(1, 1)))
    }

    @Test
    fun stepSizeAfterRecordZTest() {
        val z360 = zarembaAndTau(factorWaterfall(360)!!).first
        assertEquals(3, zStepPk(z360))
        assertEquals(30L to listOf(1, 1, 1), pkToStep(zStepPk(z360)))
    }

    @Test
    fun primesToTauTest() {
        assertEquals(1, primesToTau(emptyList()))
        assertEquals(20, primesToTau(listOf(1, 1, 4)))
    }

    private fun isWaterfall(l: List<Long>): Boolean {
        return l.zipWithNext().all { (a, b) -> a >= b }
    }

    /**
     * Given exponents of consecutive primes starting at 2, produce their
     * recomposition.
     */
    private fun unfactor(primeExponents: List<Long>): Long {
        return primeExponents.zip(primes.asList())
            .flatMap { (exp, prime) -> List(exp.toInt()) { prime } }
            .product()
    }

    @Test
    fun minTauCandidatesTest() {
        // TODO Fix candidate sequence to not emit duplicates, then change sets to lists
        assertEquals(
            setOf(
                MinTauCandidate(listOf(1), 2, true),
            ),
            minTauCandidates(2L, 1, fast=false).toSet()
        )
        assertEquals(
            setOf(
                MinTauCandidate(listOf(1, 1, 1), 30, true),
            ),
            minTauCandidates(2L, 3, fast=false).toSet()
        )
        assertEquals(
            setOf(
                MinTauCandidate(listOf(1), 2, false),
                MinTauCandidate(listOf(2), 4, false),
                MinTauCandidate(listOf(3), 8, false),
                MinTauCandidate(listOf(4), 16, false),
                MinTauCandidate(listOf(5), 32, false),
                MinTauCandidate(listOf(6), 64, false),
                MinTauCandidate(listOf(7), 128, true),
            ),
            minTauCandidates(72L, 1, fast=false).toSet()
        )
        assertEquals(
            setOf(
                MinTauCandidate(listOf(1, 1), 6, false),
                MinTauCandidate(listOf(2, 1), 12, false),
                MinTauCandidate(listOf(2, 2), 36, false),
                MinTauCandidate(listOf(3, 1), 24, false),
                MinTauCandidate(listOf(3, 2), 72, true),
                MinTauCandidate(listOf(4, 1), 48, false),
                MinTauCandidate(listOf(4, 2), 144, true),
                MinTauCandidate(listOf(5, 1), 96, true),
            ),
            minTauCandidates(72L, 2, fast=false).toSet()
        )
        assertEquals(
            setOf(
                MinTauCandidate(listOf(1, 1, 1), 30, false),

                MinTauCandidate(listOf(2, 1, 1), 60, false),
                MinTauCandidate(listOf(2, 2, 1), 180, false),
                MinTauCandidate(listOf(2, 2, 2), 900, false),

                MinTauCandidate(listOf(3, 1, 1), 120, false),
                MinTauCandidate(listOf(3, 2, 1), 360, false),
                MinTauCandidate(listOf(3, 2, 2), 1800, false),
                MinTauCandidate(listOf(3, 3, 1), 1080, false),
                MinTauCandidate(listOf(3, 3, 2), 5400, false),
                MinTauCandidate(listOf(3, 3, 3), 27000, true), // prune

                MinTauCandidate(listOf(4, 1, 1), 240, false),
                MinTauCandidate(listOf(4, 2, 1), 720, false),
                MinTauCandidate(listOf(4, 2, 2), 3600, false),
                MinTauCandidate(listOf(4, 3, 1), 2160, false),
                MinTauCandidate(listOf(4, 3, 2), 10800, true), // prune
                MinTauCandidate(listOf(4, 4, 1), 6480, false),
                MinTauCandidate(listOf(4, 4, 2), 32400, true), // prune

                MinTauCandidate(listOf(5, 1, 1), 480, false),
                MinTauCandidate(listOf(5, 2, 1), 1440, false),
                MinTauCandidate(listOf(5, 2, 2), 7200, false),
                MinTauCandidate(listOf(5, 3, 1), 4320, false),
                MinTauCandidate(listOf(5, 3, 2), 21600, true), // prune
                MinTauCandidate(listOf(5, 4, 1), 12960, true), // prune

                MinTauCandidate(listOf(6, 1, 1), 960, false),
                MinTauCandidate(listOf(6, 2, 1), 2880, false),
                MinTauCandidate(listOf(6, 2, 2), 14400, true), // prune
                MinTauCandidate(listOf(6, 3, 1), 8640, false),
                MinTauCandidate(listOf(6, 3, 2), 43200, true), // prune
                MinTauCandidate(listOf(6, 4, 1), 25920, true), // prune

                MinTauCandidate(listOf(7, 1, 1), 1920, false),
                MinTauCandidate(listOf(7, 2, 1), 5760, false),
                MinTauCandidate(listOf(7, 2, 2), 28800, true), // prune
                MinTauCandidate(listOf(7, 3, 1), 17280, true), // prune

                MinTauCandidate(listOf(8, 1, 1), 3840, false),
                MinTauCandidate(listOf(8, 2, 1), 11520, true), // prune

                MinTauCandidate(listOf(9, 1, 1), 7680, false),
                MinTauCandidate(listOf(9, 2, 1), 23040, true), // prune

                MinTauCandidate(listOf(10, 1, 1), 15360, true) // prune
            ),
            minTauCandidates(10080L, 3, fast=false).toSet()
        )

        // Check that computed values match for some large n.
        val largeK = 7
        val largeN = 1e17.toLong()
        val largeSlow = minTauCandidates(largeN, largeK, fast=false).toList()
        val largeFast = minTauCandidates(largeN, largeK, fast=true).toList()
        largeSlow.forEach { assertTrue(
            isWaterfall(it.exponents),
            "Waterfall condition failed for $it"
        )}
        largeSlow.forEach { assertEquals(
            it.product, unfactor(it.exponents), ".product mismatch for $it"
        )}
        largeSlow.forEach { assertEquals(
            it.usable, it.product >= largeN, ".usable mismatch for $it"
        )}
        // The fast version is the subset with real candidates
        assertTrue(largeSlow.toSet().containsAll(largeFast))
        largeFast.forEach { assertTrue(
            it.usable, "Fast version produced unusable candidate: $it"
        )}
        // These values are pinned to observed test outputs and have not been
        // independently validated. They're just here for behavior-pinning and
        // to illustrate the expected size of the candidate list, in both fast
        // and non-fast mode.
        // TODO: Remove distinct() when duplication bug is fixed
        assertEquals(4517, largeSlow.distinct().size)
        assertEquals(1351, minTauCandidates(largeN, largeK, fast=true).distinct().count())
    }

    @Test
    fun minTauTest() {
        // primesK=0 not supported
        //assertEquals(1, minTau(2, 0))

        assertEquals(3, minTau(4, 1))
        assertEquals(12, minTau(72, 2))

        // Observed as test output, not independently verified
        assertEquals(1152, minTau(n=288807105787200, primesK=6))
    }

    @Test
    fun vStepPkTest() {
        assertEquals(3, vStepPk(n=1102701600, recordV=1.6546758215862982, vStepPkLast=2))
    }

    @Test
    fun pkHighestDoubleFactorTest() {
        /**
         * For a given k, returns p_k and p_l
         */
        fun check(k: Int): Pair<Int, Int?> {
            val l = pkHighestDoubleFactor(k)
            val pK = nthPrime(k)
            return pK.toInt() to if (l == 0)
                null
            else
                nthPrime(l).toInt()
        }

        assertEquals(2 to null, check(1))
        assertEquals(3 to null, check(2))
        assertEquals(5 to null, check(3))
        assertEquals(7 to 2, check(4))

        assertEquals(11 to 2, check(5))
        assertEquals(13 to 3, check(6))

        assertEquals(29 to 3, check(10))
        assertEquals(31 to 5, check(11))

        // Can no longer go this high with hardcoded primes list
//        assertEquals(53 to 5, check(16))
//        assertEquals(59 to 7, check(17))
    }

    @Test
    fun pkToStep() {
        assertEquals((2L * 2*3*5*7) to listOf(2, 1, 1, 1), pkToStep(4))
    }

    @Test
    fun findRecordsZTest() {
        assertEquals(
            listOf(
                RecordSetterZ(n=4, z=0.6931471805599453, tau=3, step=2),
                RecordSetterZ(n=6, z=1.0114042647073518, tau=4, step=2),
                RecordSetterZ(n=12, z=1.5650534091363246, tau=6, step=6),
                RecordSetterZ(n=24, z=1.9574025114441351, tau=8, step=6),
                RecordSetterZ(n=36, z=2.0693078747916944, tau=9, step=6),
                RecordSetterZ(n=48, z=2.2113393276447026, tau=10, step=6),
                RecordSetterZ(n=60, z=2.6291351167661694, tau=12, step=6),
                RecordSetterZ(n=120, z=3.1536019699500124, tau=16, step=6),
                RecordSetterZ(n=180, z=3.296829727702829, tau=18, step=30),
            ),
            findRecordsZ().asSequence().take(9).toList()
        )
    }

    @Test
    fun findRecordsVTest() {
        assertEquals(
            listOf(
                RecordSetterV(n=4, v=0.6309297535714574, z=0.6931471805599453, tau=3, step=2),
                RecordSetterV(n=6, v=0.7295739585136225, z=1.0114042647073518, tau=4, step=2),
                RecordSetterV(n=12, v=0.8734729387592397, z=1.5650534091363246, tau=6, step=6),
                RecordSetterV(n=24, v=0.9413116320946855, z=1.9574025114441351, tau=8, step=6),
                RecordSetterV(n=36, v=0.9417825998016082, z=2.0693078747916944, tau=9, step=6),
                RecordSetterV(n=48, v=0.9603724676117413, z=2.2113393276447026, tau=10, step=6),
                RecordSetterV(n=60, v=1.0580418049066245, z=2.6291351167661694, tau=12, step=6),
                RecordSetterV(n=120, v=1.1374214807461371, z=3.1536019699500124, tau=16, step=6),
                RecordSetterV(n=180, v=1.140624806721235, z=3.296829727702829, tau=18, step=6),
            ),
            findRecordsV().asSequence().take(9).toList()
        )
    }
}
