package com.vitalis.core.domain.energy

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Turning the hardware step counter into steps per hour (spec §4.3.1).
 *
 * `TYPE_STEP_COUNTER` reports one ever-growing number since the last reboot. The app samples it
 * now and then (on open, and every ~15 minutes in the background) and books the difference.
 */
object StepMath {

    /** Steps between two readings. A smaller reading means the phone rebooted and the counter restarted at 0. */
    fun delta(previous: Long, current: Long): Long = if (current >= previous) current - previous else current

    /**
     * Spreads [steps] over the clock hours between two readings, in proportion to time.
     * ponytail: a proxy; with 15-minute sampling the error stays within a bar of the hourly chart.
     * Returns hour-start → steps; the total always equals [steps].
     */
    fun distribute(steps: Long, from: LocalDateTime, to: LocalDateTime): Map<LocalDateTime, Int> {
        if (steps <= 0) return emptyMap()
        val end = to.truncatedTo(ChronoUnit.HOURS)
        if (!to.isAfter(from)) return mapOf(end to steps.toInt())

        val total = Duration.between(from, to).seconds.toDouble()
        val out = linkedMapOf<LocalDateTime, Int>()
        var hour = from.truncatedTo(ChronoUnit.HOURS)
        var given = 0
        while (!hour.isAfter(end)) {
            val segStart = maxOf(hour, from)
            val segEnd = minOf(hour.plusHours(1), to)
            val share = (steps * Duration.between(segStart, segEnd).seconds / total).toInt()
            if (share > 0) out[hour] = share
            given += share
            hour = hour.plusHours(1)
        }
        // Rounding leftovers go to the most recent hour, where the reading was taken.
        if (steps - given > 0) out[end] = (out[end] ?: 0) + (steps - given).toInt()
        return out
    }
}
