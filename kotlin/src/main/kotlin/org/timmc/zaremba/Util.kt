package org.timmc.zaremba

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object Util {
    val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
}

fun <T> List<T>.swapAt(at: Int, mapper: (T) -> T): List<T> {
    return mapIndexed { i, v -> if (i == at) mapper(v) else v }
}

fun Iterable<Double>.product(): Double = fold(1.0, Double::times)
fun Iterable<Long>.product(): Long = fold(1L, Math::multiplyExact)
