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
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import kotlin.math.ln
import kotlin.math.max
import kotlin.system.exitProcess


fun Sequence<Long>.product(): Long = fold(1) { acc, n -> Math.multiplyExact(acc, n) }
fun Iterable<Double>.product(): Double = fold(1.0) { acc, n -> acc * n }
fun Iterable<Long>.product(): Long = fold(1) { acc, n -> Math.multiplyExact(acc, n) }

fun <T> List<T>.swapAt(at: Int, mapper: (T) -> T): List<T> {
    return mapIndexed { i, v -> if (i == at) mapper(v) else v }
}

val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

/**
 * The first N primes.
 */
val primes = longArrayOf(
    2, 3, 5, 7, 11,
    13, 17, 19, 23, 29,
    31, 37, 41, 43, 47,
    // Can't add more without overflowing Long in [primorials]
)

/**
 * Cache of the running product of the first N primes, computed at startup.
 * 2, 6, 30...
 */
val primorials = primes.runningReduce { acc, p -> Math.multiplyExact(acc, p) }.toLongArray()

/**
 * Get the [n]th prime, with 1-based indexing.
 */
fun nthPrime(n: Int): Long = primes[n - 1]

/**
 * Get the nth primorial: The product of the first [n] primes
 */
fun nthPrimorial(n: Int): Long {
    return if (n == 0)
        1L
    else
        primorials[n - 1]
}

/**
 * Convert a list of prime factor exponents into primorial exponents.
 */
fun transposePrimesToPrimorials(primeExponents: List<Int>): List<Int> {
    return (primeExponents + listOf(0)).zipWithNext(Int::minus)
}

/**
 * Convert a list of primorial factor exponents into prime exponents.
 */
fun transposePrimorialsToPrimes(primorialExponents: List<Int>): List<Int> {
    return primorialExponents.asReversed().runningReduce(Int::plus).reversed()
}

// TODO: Improve [factor]:
//  - Pre-check some larger powers of 2 and 3
/**
 * Produce the prime factorization of a waterfall number.
 *
 * Can be jumpstarted by supplying an existing waterfall number [knownDivisor]
 * that this one is divisible by, along with its exponents [divisorExponents].
 *
 * @return a list of exponents, or null if not a waterfall number
 */
fun factorWaterfall(n: Long, knownDivisor: Long, divisorExponents: List<Int>): List<Int>? {
    if (n % knownDivisor != 0L)
        throw java.lang.AssertionError("Known divisor $knownDivisor did not cleanly divide $n")

    // Running remainder, and factors so far
    var remainder = n.div(knownDivisor)
    val factors = divisorExponents.toMutableList()

    var previousPrimeRepeat = Int.MAX_VALUE // used for waterfall checks
    var factored = false

    for (i in primes.indices) {
        val prime = primes[i]
        // Keep dividing n by prime until we can't
        val positionAlreadySeeded = i < factors.size
        var repeats = if (positionAlreadySeeded) factors[i] else 0
        while (remainder % prime == 0L) {
            remainder = remainder.div(prime)
            repeats++
        }

        // Check if this fails the "non-ascending" constraint
        if (repeats > previousPrimeRepeat)
            return null

        if (repeats == 0) {
            // This prime wasn't a factor at all. Check what that means:
            if (remainder == 1L) {
                factored = true
                break
            } else {
                // Violates the "contiguous" constraint.
                return null
            }
        } else {
            if (positionAlreadySeeded)
                factors[i] = repeats
            else
                factors.add(repeats)
        }

        previousPrimeRepeat = repeats
    }

    if (!factored)
        throw AssertionError("Ran out of primes when factoring")

    return factors
}

fun factorWaterfall(n: Long) = factorWaterfall(n, 1, emptyList())

/**
 * Cartesian product of zero or more iterables.
 *
 * This is adapted from https://stackoverflow.com/questions/53749357/idiomatic-way-to-create-n-ary-cartesian-product-combinations-of-several-sets-of
 * with comments, variable renamings, and minor formatting changes. Also changed
 * return value from a Set to a Sequence, for both order-stability and working
 * with large products. Credit is mainly to Erik Huizinga and Tenfour04.
 */
fun <T> Collection<Iterable<T>>.getCartesianProduct(): Sequence<List<T>> {
    return if (isEmpty()) {
        emptySequence()
    } else {
        // Turn the first iterable into a sequence of single-item lists. Each of
        // these will be the seed that items from remaining iterables will be
        // added onto. This is the beginning of the product.
        val seeds = first().map(::listOf).asSequence()
        // For each remaining iterable, expand the product by combining each
        // element of the existing product with everything in the iterable.
        drop(1).fold(seeds) { product, nextIterable ->
            product.flatMap { productMember -> nextIterable.map(productMember::plus) }
        }
    }
}

/**
 * Given a map of prime divisors to their repeat counts, return all divisors.
 *
 * Does not currently work for an empty input map (i.e. for 1), but may in the
 * future.
 */
fun primesToDivisors(primeFactors: List<Int>): Sequence<Long> {
    // Make a list of powers lists. E.g. for {2:4, 3:2, 5:1} this would produce
    // [[1,2,4,8,16], [1,3,9], [1,5]]
    val powers = primes.zip(primeFactors).map { (prime, repeat) ->
        generateSequence(1L) { Math.multiplyExact(it, prime) }.take(repeat + 1).toList()
    }
    // Get the Cartesian product, e.g. [[1,1,1], [1,1,5], [1,3,1], [1,3,5]...]
    // and take the product of each sublist. This produces all divisors.
    return powers.getCartesianProduct().map { xs -> xs.reduce(Long::times) }
}

/**
 * Given a prime factorization, compute z(n) and tau(n), returned as a pair.
 */
fun zarembaAndTau(primeFactors: List<Int>): Pair<Double, Int> {
    val divisors = primesToDivisors(primeFactors)

    var tau = 0
    val z = divisors.sumOf {
        tau += 1
        ln(it.toDouble()) / it
    }

    return z to tau
}

class SingleCommand : CliktCommand(
    name = "single",
    help = "Compute z(n) and v(n) for a single n",
) {
    private val n: Long by argument("n").long()

    override fun run() {
        val factorization = factorWaterfall(n)
        if (factorization == null) {
            println("Not a waterfall number")
            exitProcess(1)
        }
        val (z, tau) = zarembaAndTau(factorization)
        val v = z / ln(tau.toDouble())
        println("z(n) = $z\ttau(n) = $tau\tv(n) = $v")
    }
}

/**
 * Given record-setter z(n), find new step-size pK for looking for new
 * record-setters.
 *
 * @returns Number of primes in step size
 */
fun zStepPk(recordZ: Double): Int {
    var stepPk = 0
    var mertensProduct = 1.0 // 2/1 * 3/2 * ... * p/(p-1)
    for (prime in primes) {
        stepPk += 1
        mertensProduct *= prime/(prime - 1.0)
        val boundsTest = mertensProduct * ln(prime.toDouble()) // TODO erdos(p)
        // If this prime pushes us past the z bound, it's the last in the prime
        // product that we'll use for step-sizes.
        if (boundsTest > recordZ) {
            return stepPk
        }
    }
    throw AssertionError("Ran out of primes")
}

/**
 * Given a set of prime exponents, give the number of divisors if they were
 * recomposed.
 */
fun primesToTau(exponents: List<Long>): Long {
    return exponents.map { it + 1 }.product()
}

/**
 * A possible candidate for minTau. Both [product] and [usable] can be derived
 * from [exponents] but are provided for performance, as they are already
 * computed in the pruning logic.
 */
data class MinTauCandidate(
    /**
     * Exponents of first k primes.
     */
    val exponents: List<Long>,
    /**
     * Products of first k primes raised to [exponents].
     */
    val product: Long,
    /**
     * Is this a *usable* candidate, i.e. one where the [product] is at least
     * as large as n? (We can return unusable ones as well just for the sake of
     * unit testing.)
     */
    val usable: Boolean,
)

/**
 * Compute MinTau candidates for [n] from a list of waterfall exponents.
 *
 * For a given zero-based index [i] within [exponents], find further
 * candidates by incrementing the exponent at that index and by moving to the
 * next higher index, recursively, always maintaining the waterfall invariant.
 *
 * During exploration, the running [product] is maintained with is just the
 * waterfall number represented by the exponents.
 *
 * Each new candidate is yielded, unless [fast] is set, in which case only
 * candidates representing a waterfall number ≥ n are yielded.
 *
 * Exploration is pruned whenever a candidate's product is ≥ n.
 *
 * @param n is the current record-setting input
 * @param exponents are waterfall number exponents that might be a candidate
 * @param product is the waterfall number represented by [exponents]
 * @param i is the current index into [exponents]
 * @param fast controls whether only valid candidates are emitted
 */
fun minTauCandidates(
    n: Long, exponents: List<Long>, product: Long, i: Int, fast: Boolean
): Sequence<MinTauCandidate> {
    return sequence {
        val usable = product >= n
        if (usable || !fast)
            yield(MinTauCandidate(exponents, product, usable))
        // Any higher product would *also* be at least n, but would have a
        // higher tau and would not contribute to a minimum.
        if (usable)
            return@sequence
        // Explore further to the right, if possible
        // FIXME: Produces the same candidate multiple times, with duplication
        //  factor approximately sqrt(primesK)
        if (i < exponents.size - 1) {
            yieldAll(minTauCandidates(n, exponents, product, i + 1, fast))
        }
        // Then, explore upwards from this p_k, if possible. We can do that iff
        // we're at the first index or if we wouldn't violate the waterfall
        // constraint.
        if (i == 0 || exponents[i] < exponents[i-1]) {
            val nextExponents = exponents.swapAt(i) { it + 1 }
            val nextProduct = Math.multiplyExact(product, primes[i])
            yieldAll(minTauCandidates(n, nextExponents, nextProduct, i, fast))
        }
    }
}

/**
 * Base case for recursive [minTauCandidates].
 */
fun minTauCandidates(n: Long, primesK: Int, fast: Boolean): Sequence<MinTauCandidate> {
    val exponents = List(primesK) {1L}
    val product = nthPrimorial(primesK)
    val i = 0
    return minTauCandidates(n, exponents, product, i, fast)
}

/**
 * Finds the smallest tau value of numbers meeting these criteria:
 *
 * - Waterfall number using the first [primesK] primes
 * - Greater than or equal to n
 */
fun minTau(n: Long, primesK: Int): Long {
    return minTauCandidates(n, primesK, fast = true)
        .filter { it.usable } // do this even though fast=true, for safety
        .minOf { primesToTau(it.exponents) }
}

/**
 * Find new step-size for looking for new record-setters, based on V.
 *
 * This requires the most recent *record* for v.
 *
 * @return The number of consecutive primes (from 2) to multiply to get the
 *   step size for v.
 */
fun vStepPk(n: Long, recordV: Double, vStepPkLast: Int): Int {
    if (vStepPkLast < 1) {
        return if (n < 4) {
            0 // step size 1
        } else {
            1 // step size 2
        }
    }

    val lastStepPrimes = primes.sliceArray(0 until vStepPkLast)

    // Merten's Product up to largest prime in current v step size
    val mert = lastStepPrimes.map { p -> p/(p - 1.0) }.product()
    // And the Erdos sum
    val erdos = lastStepPrimes.sumOf { p -> ln(p.toDouble())/(p - 1) }
    val minTau = minTau(n, vStepPkLast)

    // Multiplication by 1.0 is just to visually prove that there isn't any
    // possible integer overflow, i.e. it's a "safe" multiplication.
    return if (mert * 1.0 * erdos / ln(minTau.toDouble()) <= recordV) {
        val nextStepCount = vStepPkLast + 1
        val nextStep = nthPrimorial(nextStepCount)

        if (n % nextStep == 0L) {
            nextStepCount
        } else {
            // need to wait until a higher multiple
            vStepPkLast
        }
    } else {
        // no luck, use the old one
        vStepPkLast
    }
}

/**
 * If steps are divisible by the first k primes, they are also divisible by the
 * first l primes. Returns l, which might be zero.
 */
fun pkHighestDoubleFactor(k: Int): Int {
    val pK = nthPrime(k).toDouble()
    return primes.takeWhile { pLInt ->
        val pL = pLInt.toDouble()
        val lnPL = ln(pL)
        val c1 = pK > pL * (pL + 1)
        val c2LHS = lnPL*(pL + 3)/pL // factored a little differently in the paper
        val c2RHS = lnPL*(pK + 1)/pK + ln(pK)/pK*(pL + 1)
        c1 && c2LHS>= c2RHS
    }.count()
}

/**
 * Produce the step size we can take if all record-setting n are divisible by
 * the first [k] primes. Also returns the exponents of the step's factorization.
 * (The step size is a waterfall number.)
 */
fun pkToStep(k: Int): Pair<Long, List<Int>> {
    val l = pkHighestDoubleFactor(k)
    // We get to double-dip on the first l primes
    val step = Math.multiplyExact(nthPrimorial(k), nthPrimorial(l))
    val exp = List(k) { i -> if (i < l) 2 else 1 }
    return step to exp
}

data class RecordSetterZ(
    val n: Long,
    val z: Double,
    val tau: Int,
    val step: Long,
)

/**
 * Yield all n that produce record-setting values for z(n).
 *
 * Eventually overflows Long and crashes, if you get that far.
 *
 * Assumes that all record-setters will be waterfall numbers, so don't even do
 * those computations if waterfall factorization fails.
 */
fun findRecordsZ(continueFrom: RecordsContinueFrom? = null): Iterator<RecordSetterZ> {
    return iterator {
        var n = 1L
        var stepSize = 1L
        var stepExponents = emptyList<Int>()
        var record = 0.0

        fun setStepPk(newStepPk: Int) {
            with(pkToStep(newStepPk)) {
                stepSize = first
                stepExponents = second
            }
        }

        continueFrom?.let {
            n = it.n
            setStepPk(it.stepPk)
            record = it.record
        }

        while (true) {
            val primeFactors = factorWaterfall(n, stepSize, stepExponents)
            if (primeFactors != null) {
                val (z, tau) = zarembaAndTau(primeFactors)

                val isRecord = record > 0 && z > record
                record = max(z, record)

                if (isRecord) {
                    setStepPk(zStepPk(recordZ=record))
                    yield(RecordSetterZ(
                        n = n, z = z, tau = tau, step = stepSize,
                    ))
                }
            }
            n += stepSize
        }
    }
}

/**
 * Find and print all n that produce record-setters for z(n).
 */
fun doRecordsZ(continueFrom: RecordsContinueFrom?) {
    @OptIn(ExperimentalStdlibApi::class)
    val jsonZ = moshi.adapter<RecordSetterZ>()

    findRecordsZ(continueFrom).forEach { r ->
        println(jsonZ.toJson(r))
    }
}

data class RecordSetterV(
    val n: Long,
    val v: Double,
    val z: Double,
    val tau: Int,
    val step: Long,
)

/**
 * Yield all n that produce record-setting values for v(n).
 *
 * Eventually overflows Long and crashes, if you get that far.
 *
 * Assumes that all record-setters will be waterfall numbers, so don't even do
 * those computations if waterfall factorization fails.
 */
fun findRecordsV(continueFrom: RecordsContinueFrom? = null): Iterator<RecordSetterV> {
    return iterator {
        var n = 2L // v(1) is undefined

        var stepPk = 0 // number of primes to multiply for the v step size
        var stepSize = 1L // *actual* step size to use, based on stepPk
        var stepExponents = emptyList<Int>()

        var record = 0.0

        fun setStepPk(newStepPk: Int) {
            stepPk = newStepPk
            with(pkToStep(newStepPk)) {
                stepSize = first
                stepExponents = second
            }
        }

        continueFrom?.let {
            n = it.n
            setStepPk(it.stepPk)
            record = it.record
        }

        // Trigger recomputation of step size when n >= this
        var recalcStepWhen: Long = 1000
        // Report statistics on how fast things are moving
        var lastRecalc = System.nanoTime()
        var nWhenLastRecalc = n

        while (true) {
            val primeFactors = factorWaterfall(n, stepSize, stepExponents)
            if (primeFactors != null) {
                val (z, tau) = zarembaAndTau(primeFactors)
                val v = z / ln(tau.toDouble())

                val isRecordV = record > 0 && v > record
                record = max(v, record)

                val doRecalc = n > recalcStepWhen
                // V gets rare much faster than Z, so periodically recompute
                // step size even when there isn't a record.
                if (isRecordV || doRecalc) {
                    val iters = (n - nWhenLastRecalc)/stepSize // compute before recalc
                    setStepPk(vStepPk(n = n, recordV = record, vStepPkLast = stepPk))

                    if (doRecalc) {
                        lastRecalc = System.nanoTime().also { nowNanos ->
                            val millis = (nowNanos - lastRecalc) / 1_000_000
                            System.err.println("Recalculated step size: n=$n, step=$stepSize, elapsed=${millis}ms, iters=$iters")
                        }
                    }

                    // Every N iterations -- not too frequently, since
                    // recalculating has its own cost.
                    recalcStepWhen = n + 10_000 * stepSize
                    nWhenLastRecalc = n
                }

                if (isRecordV) {
                    yield(RecordSetterV(
                        n = n, v = v, z = z, tau = tau, step = stepSize,
                    ))
                }
            }
            n += stepSize
        }
    }
}

/**
 * Find and print all n that produce record-setters for v(n).
 */
fun doRecordsV(continueFrom: RecordsContinueFrom?) {
    @OptIn(ExperimentalStdlibApi::class)
    val jsonV = moshi.adapter<RecordSetterV>()

    findRecordsV(continueFrom).forEach { r ->
        println(jsonV.toJson(r))
    }
}

enum class RecordsType { Z, V; }

data class RecordsContinueFrom(
    val n: Long,
    val stepPk: Int,
    val record: Double,
)

class RecordsCmd : CliktCommand(
    name = "records",
    help = "Find and print all n that produce a record-setter for z(n) or v(n)"
) {
    private val which by argument(
        "function",
        help = "Which function to compute, i.e. ${RecordsType.values().joinToString("/") { it.name.lowercase() }}"
    ).enum<RecordsType> { it.name.lowercase() }

    private val continueFrom by option(
        "--from",
        help = """
            Continue calculation from the checkpoint saved in this JSON file.

            Format is a map of iteration state values (as strings):

            - n: The input n${"\u0085"}
            - step_pk: Number of unique primes in step size (use the factor
              command if needed)${"\u0085"}
            - record: The most recent record-setting output
        """.trimIndent()
    ).file(mustBeReadable = true)

    override fun run() {
        val continueData = continueFrom?.let { f ->
            @OptIn(ExperimentalStdlibApi::class)
            val json = moshi.adapter<Map<String, String>>()
            val prev = json.fromJson(f.readText())!!

            RecordsContinueFrom(
                n = prev["n"]!!.toLong(),
                stepPk = prev["step_pk"]!!.toInt(),
                record = prev["record"]!!.toDouble(),
            )
        }

        // These are computed separately because one runs much faster than
        // the other.
        when (which) {
            RecordsType.Z -> doRecordsZ(continueData)
            RecordsType.V -> doRecordsV(continueData)
        }
    }
}

class LatexCommand : CliktCommand(
    name = "latex",
    help = "Combine z/v outputs and print for LaTeX table"
) {
    private val zJsonFile by option("--z").file(mustBeReadable = true).required()
    private val vJsonFile by option("--v").file(mustBeReadable = true).required()

    override fun run() {
        @OptIn(ExperimentalStdlibApi::class)
        val json = moshi.adapter<Map<String, Double>>()

        fun read(src: File): Map<Long, Map<String, Double>> {
            return src.readLines().map { json.fromJson(it)!! }
                .associateBy { it["n"]!!.toLong() }
        }

        val zByN = read(zJsonFile)
        val vByN = read(vJsonFile)

        val onlyV = vByN.keys.minus(zByN.keys).sorted()
        if (onlyV.isNotEmpty()) {
            println("===========================================================")
            println("Record-setting n for V that were not in Z: $onlyV")
            if (vByN.keys.max() > zByN.keys.max())
                println("...but that's probably because the Z records are incomplete")
            println("===========================================================")
        }

        val allN = zByN.keys.plus(vByN.keys).sorted()
        println("n & z(n) & tau(n) & v(n) & type of record & z step & v step")
        for (n in allN) {
            val zRec = zByN[n]
            val vRec = vByN[n]
            val recordType = when {
                zRec != null && vRec == null -> "Z"
                zRec == null && vRec != null -> "V"
                zRec != null && vRec != null -> "both"
                else -> throw AssertionError("Unexpected N value")
            }

            fun lookup(key: String): Double? {
                val all = listOfNotNull(zRec, vRec).mapNotNull { it[key] }
                if (all.size > 1 && all.any { it != all[0] }) {
                    throw RuntimeException("For n=$n, field '$key' did not match")
                }
                return all.getOrNull(0)
            }

            println(listOf(
                n,
                lookup("z")!!,
                lookup("tau")!!.toInt(),
                lookup("v"),
                recordType,
                zRec?.get("step")?.toInt(),
                vRec?.get("step")?.toInt(),
            ).map { it ?: "" }.joinToString(" & ")
            )
        }
    }
}

class FactorCmd : CliktCommand(
    name = "factor",
    help = "Factor a waterfall number"
) {
    private val n: Long by argument("n").long()

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
        val primIndicesString = primorialExps.mapIndexedNotNull { i, exp ->
            if (exp == 0) null else "pr(${i + 1})^$exp"
        }.joinToString(" * ")
        val primFactorsString = primorialExps.mapIndexedNotNull { i, exp ->
            if (exp == 0) null else "${nthPrimorial(i + 1)}^$exp"
        }.joinToString(" * ")
        println("Primorial indices: $primIndicesString")
        println("Primorial factors: $primFactorsString")
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
    RecordsCmd(),
    SingleCommand(),
    FactorCmd(),
    LatexCommand(),
)

fun main(args: Array<String>) {
    cli.main(args)
}
