package com.vitalis.feature.tracking

import com.vitalis.core.model.TrackingState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

private val Id = Locale.forLanguageTag("id")

/**
 * What to say out loud after a state change, or null for silence. Pure, so the wording is testable;
 * [TrackingService] owns the speaker. Reaching the target wins over the kilometre cue it coincides with.
 */
object VoiceCues {
    fun cue(before: TrackingState, after: TrackingState): String? {
        val target = after.targetMeters
        if (target != null && before.distanceMeters < target && after.distanceMeters >= target) {
            return "Target ${km(target / 1000)} kilometer tercapai. Waktu ${spoken(after.movingSeconds)}."
        }
        if (after.laps.size > before.laps.size) {
            val pace = after.avgPaceSecPerKm?.let { " Pace rata-rata ${spoken(it.roundToLong())} per kilometer." }.orEmpty()
            return "Kilometer ${after.laps.last().index}. Waktu ${spoken(after.movingSeconds)}.$pace"
        }
        return null
    }

    /** "1 jam 5 menit", "6 menit 10 detik": how a coach says it, not "06:10". */
    fun spoken(seconds: Long): String {
        val h = seconds / 3600
        val m = seconds % 3600 / 60
        val s = seconds % 60
        return listOfNotNull(
            "$h jam".takeIf { h > 0 },
            "$m menit".takeIf { m > 0 },
            "$s detik".takeIf { s > 0 || (h == 0L && m == 0L) },
        ).joinToString(" ")
    }

    /** "5", "21,1". */
    fun km(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Id, "%.1f", value)
}

/** Tile caption on the sport picker: "Hari ini", "Kemarin", "3 hari lalu", then a date. */
fun lastUsedLabel(date: LocalDate, today: LocalDate): String = when (val days = ChronoUnit.DAYS.between(date, today)) {
    0L -> "Hari ini"
    1L -> "Kemarin"
    in 2L..6L -> "$days hari lalu"
    else -> date.format(DateTimeFormatter.ofPattern("d MMM", Id))
}
