package org.timmc.zaremba

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ZarembaTest {
    @Test
    fun sigmaApproxTest() {
        assertEquals(60.0, Zaremba.sigmaApprox(listOf(3, 1)))
        assertEquals(121.0, Zaremba.sigmaApprox(listOf(0, 4)))
    }

    @Test
    fun hApproxTest() {
        assertEquals(2.3333333333333335, Zaremba.hApprox(12.0, listOf(2, 1)))
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
        assertEquals(1.0114042647073518, Zaremba.z(6.toBigInteger(), listOf(1, 1)))
        assertEquals(1.5650534091363244, Zaremba.z(12.toBigInteger(), listOf(2, 1)))
        assertEquals(1.957402511444135, Zaremba.z(24.toBigInteger(), listOf(3, 1)))
        assertEquals(2.211339327644702, Zaremba.z(48.toBigInteger(), listOf(4, 1)))
        assertEquals(20.507394183515835, Zaremba.z(
            "55203167814376982400".toBigInteger(),
            listOf(7,5,2,2,1,1,1,1,1,1,1,1,1)
        ))
    }

    @Test
    fun primesToTauTest() {
        assertEquals(1, Zaremba.primesToTau(emptyList()).toInt())
        assertEquals(20, Zaremba.primesToTau(listOf(1, 1, 4)).toInt())
    }

    @Test
    fun findRecordsZTest() {
        assertEquals(
            listOf(
                Zaremba.RecordSetter(
                    n = "4", tau = 3.toBigInteger(),
                    z = 0.6931471805599453, isZRecord = true,
                    v = 0.6309297535714574, isVRecord = true,
                    primes = listOf(2), primorials = listOf(2),
                ),
                Zaremba.RecordSetter(
                    n = "6", tau = 4.toBigInteger(),
                    z = 1.0114042647073518, isZRecord = true,
                    v = 0.7295739585136225, isVRecord = true,
                    primes = listOf(1, 1), primorials = listOf(0, 1),
                ),
            ),
            Zaremba.findRecords(10.toBigInteger()).toList()
        )

        // Not independently verified, just grabbed from output of a previous
        // run -- this will only catch changes in behavior.
        assertEquals(
            Zaremba.RecordSetter(
                n = "963761198400", tau = 6720.toBigInteger(),
                z = 14.960783769593887, isZRecord = true,
                v = 1.6976114329564411, isVRecord = false,
                primes = listOf(6, 4, 2, 1, 1, 1, 1, 1, 1),
                primorials = listOf(2, 2, 1, 0, 0, 0, 0, 0, 1)
            ),
            Zaremba.findRecords(1000000000000.toBigInteger()).last()
        )
    }

    @Test
    fun kPrimesFindTest() {
        // Check that, using an older record for v, we would find a new one
        val oldRecord = 1.666933568966563
        val newRecord = 1.7059578102443238
        val earlier = Zaremba.searchVRecordKPrimes(9, oldRecord)
        assertEquals(newRecord, earlier.second.toList().maxOf { it.v })

        // But using the new record as a bound, we don't find any more
        val later = Zaremba.searchVRecordKPrimes(9, newRecord)
        assertEquals(0, later.second.count { it.v > newRecord })
    }
}
