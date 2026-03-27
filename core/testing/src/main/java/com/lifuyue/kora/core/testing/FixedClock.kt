package com.lifuyue.kora.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FixedClock(
    private var currentInstant: Instant,
    private var currentZone: ZoneId = ZoneId.of("UTC"),
) : Clock() {
    override fun getZone(): ZoneId = currentZone

    override fun instant(): Instant = currentInstant

    override fun withZone(zone: ZoneId): Clock = FixedClock(currentInstant, zone)

    fun advanceBy(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }

    fun set(instant: Instant) {
        currentInstant = instant
    }
}
