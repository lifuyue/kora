package com.lifuyue.kora.core.testing

import java.util.concurrent.atomic.AtomicLong

class SequentialIdGenerator(
    private val prefix: String = "test",
    seed: Long = 0,
) {
    private val nextValue = AtomicLong(seed)

    fun nextId(): String = "$prefix-${nextValue.incrementAndGet()}"
}
