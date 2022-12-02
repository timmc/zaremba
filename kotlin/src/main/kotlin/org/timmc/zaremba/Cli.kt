package org.timmc.zaremba

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.types.file
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
        val primorialExps = Primes.toPrimorials(factorization)
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
    SingleCommand(),
    FactorCmd(),
    LatexCommand(),
)

fun main(args: Array<String>) {
    cli.main(args)
}
