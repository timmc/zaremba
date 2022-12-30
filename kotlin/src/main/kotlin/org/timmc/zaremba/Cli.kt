package org.timmc.zaremba

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.squareup.moshi.adapter
import kotlin.math.ln
import kotlin.system.exitProcess

class SingleCommand : CliktCommand(
    name = "single",
    help = "Compute z(n) and v(n) for a single n",
) {
    private val n by argument("n").convert { it.toBigInteger() }

    override fun run() {
        val factorization = Waterfall.factor(n)
        if (factorization == null) {
            println("Not a waterfall number")
            exitProcess(1)
        }
        val z = Zaremba.z(n, factorization)
        val tau = Zaremba.primesToTau(factorization)
        val v = z / ln(tau.toDouble())
        println("z(n) = $z\ttau(n) = $tau\tv(n) = $v")
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
        val json = Util.moshi.adapter<Zaremba.RecordSetter>()

        Zaremba.findRecords(maxN).forEach { r ->
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
        val json = Util.moshi.adapter<Zaremba.RecordSetter>()
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
        val factorization = Waterfall.factor(n)
        if (factorization == null) {
            println("Not a waterfall number, cannot factor")
            exitProcess(1)
        }
        println("Prime exponents: $factorization")
        println("Repeated prime factors: ${Primes.list.zip(factorization).joinToString(" * ") { (p, a) -> "$p^$a" }}")

        // Transpose the factorization into primorials
        val primorialExps = Primes.waterfallToPrimorials(factorization)
        val primFactorsString = primorialExps.mapIndexedNotNull { i, exp ->
            if (exp == 0) null else "${Primorials.nth(i + 1)}^$exp"
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
        Waterfall.findUpTo(maxN).forEach {
            println("${it.value}: ${it.primorialExponents}")
        }
    }
}

class KPrimesCmd : CliktCommand(
    name = "k-primes",
    help = """
        Check for record-setters for v(n) where n is composed only of positive
        powers of all of the first k primes.
    """.trimIndent()
) {
    private val k by option(
        "--k",
        help = "Number of unique, consecutive primes in n's factorization"
    ).int().required()

    private val vRecord by option(
        "--V",
        help = "Largest known record-setter for v(n)"
    ).double().required()

    override fun run() {
        val (intermediate, results) = Zaremba.searchVRecordKPrimes(k, vRecord)

        println("Max z(n) = ${intermediate.zMax}")
        println("Max log(tau(n)) = ${intermediate.logTauMax}, tau(n) = ${intermediate.tauMax}")

        val newRecords = mutableListOf<String>()
        var checked = 0
        results.forEach { res ->
            val result = "primorials=${res.primorials}\tprimes = ${res.primes}\ttau = ${res.tau}\tn = ${res.n}\tz = ${res.z}\tv = ${res.v}"
            println(result)
            if (res.v > vRecord) {
                newRecords.add(result)
            }
            checked++
        }

        println("Checked $checked candidates; ${newRecords.size} new records found.")
        if (newRecords.isNotEmpty()) {
            newRecords.forEach { println("New record! $it") }
        }
    }
}

class MaxVCmd : CliktCommand(
    name = "max-v",
    help = """
        Performs a search for the highest possible v(n) value.

        This is done using k-primes bootstrapping. The bootstrapping is backed
        by proofs, but the halting condition used in the code is not (although
        it searches beyond k=29, so we're OK anyway.)
    """.trimIndent()
) {
    override fun run() {
        for (v in Zaremba.maxVByBootstrapping()) {
            println(v)
        }
    }
}

class Cli : CliktCommand(
    name = "Zaremba",
    help = "Tool to find z(n) and v(n) record-setters.",
    invokeWithoutSubcommand = false,
) {
    override fun run() {
        // does not run
    }
}

val cli = Cli().subcommands(
    WaterfallCmd(),
    RecordsCmd(),
    KPrimesCmd(),
    MaxVCmd(),
    SingleCommand(),
    FactorCmd(),
    LatexCommand(),
)

fun main(args: Array<String>) {
    cli.main(args)
}
