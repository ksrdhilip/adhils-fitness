package com.adhils.fitness.core

expect fun nowMillis(): Long

fun formatDecimal(v: Double): String {
    val rounded = kotlin.math.round(v * 10.0) / 10.0
    val i = rounded.toLong()
    return if (kotlin.math.abs(rounded - i) < 0.05) i.toString() else {
        val dec = (kotlin.math.abs(rounded - i) * 10.0 + 0.5).toInt()
        "$i.$dec"
    }
}
