package com.it342.projectmanagementsystem.models

import java.util.concurrent.TimeUnit

data class TimestampObject(
    val seconds: Long,
    val nanos: Int
) {
    companion object {
        fun fromMillis(millis: Long): TimestampObject {
            val seconds = millis / 1000
            val nanos = ((millis % 1000) * 1_000_000).toInt()
            return TimestampObject(seconds, nanos)
        }
    }

    fun toMillis(): Long {
        return TimeUnit.SECONDS.toMillis(seconds) +
                TimeUnit.NANOSECONDS.toMillis(nanos.toLong())
    }
} 