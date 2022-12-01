/**
 * Definitions:
 *
 * - `Pk` is the kth prime, and is usually used when the first k primes are to
 *   be multiplied together; `zStepPk` refers to the number of primes that must
 *   be multiplied to form the step size for z.
 * - `Waterfall` refers to either a number in OEIS A025487, or the list of
 *   exponents themselves. Where p_i are the consecutive primes starting with
 *   p_1 = 2, and n has prime factorization `p_1^a_1 * p_2^a_2 * ... * p_k^a_k`
 *   then it is a waterfall number if `a_1 >= a_2 >= a_3 >= ... >= a_k`.
 *   Simply put, there are no gaps in the prime factorization and the exponents
 *   are non-ascending. For example, `10080 = (2^5)(3^2)(5^1)(7^1)`.
 */

package org.timmc

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.types.file
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigInteger
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.system.exitProcess

fun <T> List<T>.swapAt(at: Int, mapper: (T) -> T): List<T> {
    return mapIndexed { i, v -> if (i == at) mapper(v) else v }
}

fun Iterable<Double>.product(): Double = fold(1.0, Double::times)
fun Iterable<Long>.product(): Long = fold(1, Long::times)

val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

/**
 * From https://oeis.org/A000040 -- add more as needed.
 */
val primes = arrayOf(
    2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
    73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151,
    157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233,
    239, 241, 251, 257, 263, 269, 271,
).map { it.toBigInteger() }

/**
 * primorials.get(k) == (k+1)th primorial number: 2, 6, 30, ...
 */
val primorials = primes
    .runningFold(BigInteger.ONE, BigInteger::multiply)
    .drop(1)

/**
 * Get the nth primorial: The product of the first [n] primes
 */
fun nthPrimorial(n: Int): BigInteger {
    return if (n == 0)
        BigInteger.ONE
    else
        primorials[n - 1]
}

/**
 * Convert a list of prime factor exponents into primorial exponents.
 */
fun transposePrimesToPrimorials(primeExponents: List<Int>): PrimorialExponents {
    return (primeExponents + listOf(0)).zipWithNext(Int::minus)
}

/**
 * Convert a list of primorial factor exponents into prime exponents.
 */
fun transposePrimorialsToPrimes(primorialExponents: PrimorialExponents): List<Int> {
    return primorialExponents.asReversed().runningReduce(Int::plus).reversed()
}

/**
 * Produce the prime factorization of a waterfall number.
 *
 * @return a map of prime factors to their counts, or null if not a waterfall
 *   number
 */
fun factorWaterfall(n: BigInteger): List<Int>? {
    // Running remainder, and factors so far
    var remainder = n
    val factors = mutableListOf<Int>()
    var previousPrimeRepeat = Int.MAX_VALUE // used for waterfall checks
    var factored = false

    for (prime in primes) {
        // Keep dividing n by prime until we can't
        var repeats = 0
        while (remainder.mod(prime) == BigInteger.ZERO) {
            remainder /= prime
            repeats++
        }

        // Check if this fails the "non-ascending" constraint
        if (repeats > previousPrimeRepeat)
            return null

        if (repeats == 0) {
            // This prime wasn't a factor at all. Check what that means:
            if (remainder == BigInteger.ONE) {
                factored = true
                break
            } else {
                // Violates the "contiguous" constraint.
                return null
            }
        } else {
            factors.add(repeats)
        }

        previousPrimeRepeat = repeats
    }

    if (!factored)
        throw AssertionError("Ran out of primes when factoring")

    return factors.toList()
}

/**
 * Approximate sigma_1(n), the sum of the divisors, without computing them
 * directly.
 */
fun sigmaApprox(primeFactors: List<Int>): Double {
    return primeFactors.mapIndexed { pK, exp ->
        val p = primes[pK].toDouble()
        (p.pow(exp + 1) - 1)/(p - 1)
    }.product()
}

/**
 * Approximate h(n) = sigma(n)/n = sum(1/divisor) -- without computing divisors.
 */
fun hApprox(n: Double, primeFactors: List<Int>): Double {
    return sigmaApprox(primeFactors) / n
}

/**
 * Compute z(n) based on a formula from Erdős and Zaremba 1974, although it
 * removes the `j` term from the product on the assumption that that was a typo
 * (not yet proven as of this writing).
 *
 * This method avoids having to compute all the divisors of n, which can be
 * computationally much more expensive.
 */
fun zaremba(n: BigInteger, primeFactors: List<Int>): Double {
    val nApprox = n.toDouble()
    // Precomputing a list of powers of primes did not achieve a noticeable speedup.
    return primeFactors.flatMapIndexed { pK, exp ->
        val p = primes[pK].toDouble()
        val lnP = ln(p)
        (1..exp).map { j ->
            val pToJ = p.pow(j)
            val nReduced = nApprox / pToJ
            val nFacReduced = primeFactors.swapAt(pK) { it - j }
            val h = hApprox(nReduced, nFacReduced)
            lnP / pToJ * h
        }
    }.sum()
}

class SingleCommand : CliktCommand(
    name = "single",
    help = "Compute z(n) and v(n) for a single n",
) {
    private val n by argument("n").convert { it.toBigInteger() }

    override fun run() {
        val factorization = factorWaterfall(n)
        if (factorization == null) {
            println("Not a waterfall number")
            exitProcess(1)
        }
        val z = zaremba(n, factorization)
        val tau = primesToTau(factorization)
        val v = z / ln(tau.toDouble())
        println("z(n) = $z\ttau(n) = $tau\tv(n) = $v")
    }
}

/**
 * Given a set of prime exponents, give the number of divisors if they were
 * recomposed.
 */
fun primesToTau(exponents: List<Int>): Long {
    return exponents.map { it + 1L }.product()
}

data class RecordSetter(
    val n: String, // JSON doesn't handle BigInteger well
    val tau: Long,
    val z: Double,
    val isZRecord: Boolean,
    val v: Double,
    val isVRecord: Boolean,
    val primes: List<Int>,
    val primorials: List<Int>
)

/**
 * Yield all n that produce record-setting values for z(n) or v(n).
 */
fun findRecords(maxN: BigInteger): Sequence<RecordSetter> {
    return sequence {
        var recordZ = 0.0
        var recordV = 0.0
        for (n in findWaterfall(maxN).drop(1)) {
            val primeExp = transposePrimorialsToPrimes(n.primorialExponents)
            val tau = primesToTau(primeExp)
            val z = zaremba(n.value, primeExp)
            val v = z / ln(tau.toDouble())

            val isRecordZ = recordZ > 0 && z > recordZ
            val isRecordV = recordV > 0 && v > recordV

            if (isRecordZ || isRecordV) {
                yield(RecordSetter(
                    n = n.value.toString(), tau = tau,
                    z = z, isZRecord = isRecordZ,
                    v = v, isVRecord = isRecordV,
                    primes = primeExp, primorials = n.primorialExponents,
                ))
            }

            recordZ = max(z, recordZ)
            recordV = max(v, recordV)
        }
    }
}

class RecordsCmd : CliktCommand(
    name = "records",
    help = "Find and print all n that produce a record-setter for z(n) or v(n)"
) {
    private val maxN by argument(
        "max-n",
        help = "Search for record-setting inputs up to this n."
    ).convert { it.toBigInteger() }

    override fun run() {
        @OptIn(ExperimentalStdlibApi::class)
        val json = moshi.adapter<RecordSetter>()

        findRecords(maxN).forEach { r ->
            println(json.toJson(r))
        }
    }
}

class LatexCommand : CliktCommand(
    name = "latex",
    help = "Reformat records output as LaTeX table"
) {
    private val recordsJsonFile by argument("records-file").file(mustBeReadable = true)

    override fun run() {
        @OptIn(ExperimentalStdlibApi::class)
        val json = moshi.adapter<RecordSetter>()
        val records = recordsJsonFile.readLines().map { json.fromJson(it)!! }

        println("n & z(n) & tau(n) & v(n) & type of record & z step & v step")
        for (r in records) {
            val recordType = when {
                r.isZRecord && !r.isVRecord -> "Z"
                !r.isZRecord && r.isVRecord -> "V"
                r.isZRecord && r.isVRecord -> "both"
                else -> throw AssertionError("Non-record-setter entry found: $r")
            }

            println(
                listOf(r.n, r.z, r.tau, r.v, recordType)
                    .joinToString(" & ")
            )
        }
    }
}

class FactorCmd : CliktCommand(
    name = "factor",
    help = "Factor a waterfall number"
) {
    private val n by argument("n").convert { it.toBigInteger() }

    override fun run() {
        val factorization = factorWaterfall(n)
        if (factorization == null) {
            println("Not a waterfall number, cannot factor")
            exitProcess(1)
        }
        println("Prime exponents: $factorization")
        println("Repeated prime factors: ${primes.zip(factorization).joinToString(" * ") { (p, a) -> "$p^$a" }}")

        // Transpose the factorization into primorials
        val primorialExps = transposePrimesToPrimorials(factorization)
        val primFactorsString = primorialExps.mapIndexedNotNull { i, exp ->
            if (exp == 0) null else "${nthPrimorial(i + 1)}^$exp"
        }.joinToString(" * ")
        println("Primorial exponents: $primorialExps")
        println("Primorial factors: $primFactorsString")

        runCatching {
            val levels = listOf(' ', '▁', '▂', '▃', '▄', '▅', '▆', '▇')
            val sparkline = primorialExps.joinToString("") {
                levels[it].toString()
            }
            println("Primorial sparkline: [$sparkline]")
        }.onFailure { println("Could not make primorial sparkline") }
    }
}

/**
 * Representation of a waterfall number as a list of exponents of the first k
 * primorial numbers. For example, [0, 1, 3] = 2^0 * 6^1 * 30^3
 */
typealias PrimorialExponents = List<Int>

data class WaterfallNumber(
    val value: BigInteger,
    val primorialExponents: PrimorialExponents,
)

/**
 * Recursive core of [find()]. Given a list of exponents of lower primorial
 * numbers, accompanied by a matching product, yield waterfall numbers that
 * are multiples of that base and various powers of the first primorial in
 * the list of remaining ones.
 *
 * Explores in two directions: Higher powers of the current primorial, and in
 * recursive calls to the next primorial (one for each power of this one, plus
 * one.)
 *
 * The output is not sorted.
 */
private fun innerFindWaterfall(
    primorials: List<BigInteger>, maxN: BigInteger,
    baseExp: PrimorialExponents, baseProduct: BigInteger
): Sequence<WaterfallNumber> {
    return sequence {
        // If we ever reach the end of the list *before* we find a primorial
        // that is larger than N, raise an exception -- we need a larger list
        // of prime numbers!
        if (primorials.isEmpty())
            throw AssertionError("Ran out of primorials")

        val factor = primorials[0]
        if (factor > maxN)
            return@sequence

        val nextPrimorials = primorials.drop(1)
        yieldAll(innerFindWaterfall(nextPrimorials, maxN, baseExp.plus(0), baseProduct))

        var nextProduct = baseProduct
        var exponent = 0

        while (true) {
            nextProduct = factor * nextProduct
            if (nextProduct > maxN)
                break
            exponent += 1

            val nextExp = baseExp.plus(exponent)
            yield(WaterfallNumber(
                value = nextProduct,
                primorialExponents = nextExp
            ))
            yieldAll(innerFindWaterfall(nextPrimorials, maxN, nextExp, nextProduct))
        }
    }
}

/**
 * Produce a sorted sequence of all the waterfall numbers up to N, starting with
 * 1, along with their factorization as primorial exponents.
 */
fun findWaterfall(maxN: BigInteger): Sequence<WaterfallNumber> {
    val all = innerFindWaterfall(primorials, maxN, emptyList(), BigInteger.ONE)
        .plus(WaterfallNumber(value = BigInteger.ONE, primorialExponents = emptyList()))
    return all.sortedBy(WaterfallNumber::value)
}

class WaterfallCmd : CliktCommand(
    name = "waterfall",
    help = """
        Print the waterfall numbers up to max-n, in some order.

        Output lines will look like the following:

            5400: [0, 1, 2]
        
        ...where 5400 is the waterfall number, and the list shows the
        decomposition into primorial numbers. Here, 5400 is the product of the
        2nd primorial number (6) to the first power and the 3rd primorial number
        (30) to the second power.
    """.trimIndent()
) {
    private val maxN by argument(
        "max-n",
        help = "Search for waterfall numbers up to this value."
    ).convert { it.toBigInteger() }

    override fun run() {
        findWaterfall(maxN).forEach {
            println("${it.value}: ${it.primorialExponents}")
        }
    }
}

class Zaremba : CliktCommand(
    name = "Zaremba",
    help = "Tool to find z(n) and v(n) record-setters.",
    invokeWithoutSubcommand = false,
) {
    override fun run() {
        // does not run
    }
}

val cli = Zaremba().subcommands(
    WaterfallCmd(),
    RecordsCmd(),
    SingleCommand(),
    FactorCmd(),
    LatexCommand(),
)

fun main(args: Array<String>) {
    cli.main(args)
}
