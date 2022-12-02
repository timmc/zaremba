package org.timmc.zaremba

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ZarembaTest {
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
}
