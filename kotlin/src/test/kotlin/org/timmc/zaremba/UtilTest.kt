package org.timmc.zaremba

import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs

class UtilTest {
    @Test
    fun swapAtTest() {
        val base = listOf("a", "b", "c")
        assertEquals(listOf("A", "b", "c"), base.swapAt(0, String::uppercase))
        assertEquals(listOf("a", "b", "C"), base.swapAt(2, String::uppercase))
        assertIs<IndexOutOfBoundsException>(assertFails {
            base.swapAt(3, String::uppercase)
        })
    }

    @Test
    fun productTest() {
        assertEquals(9.0, listOf(1.0, 2.0, 4.5).product())

        assertEquals(1000L, listOf<Long>(5, 25, 8).product())
        assertIs<ArithmeticException>(assertFails { listOf(Long.MAX_VALUE, 2).product() })

        assertEquals(BigInteger.TEN, listOf(BigInteger.TWO, 5.toBigInteger()).product())
    }
}
