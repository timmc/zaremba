package org.timmc.zaremba

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant

object Util {
    val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
}

val isDebug = System.getenv("DEBUG") == "true"

fun debug(msgF: () -> String) {
    if (isDebug)
        System.err.println("DEBUG [${Instant.now()}] ${msgF()}")
}

fun <T> List<T>.swapAt(at: Int, mapper: (T) -> T): List<T> {
    return mapIndexed { i, v -> if (i == at) mapper(v) else v }
}

fun Iterable<Double>.product(): Double = fold(1.0, Double::times)
fun Iterable<Long>.product(): Long = fold(1, Long::times)
