package com.vitalis.core.common.format

import com.vitalis.core.model.UnitSystem
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Display formatting shared by every screen.
 *
 * Kept in one place because inconsistent number formatting is one of the fastest
 * ways to make an app feel unfinished — 5.2 km on one screen and 5,20 km on the
 * next reads as a bug even when the value is right.
 */
object Formatters {

    private val locale: Locale get() = Locale.getDefault()

    // ---- Duration ---------------------------------------------------------

    /** `1:23:45` for long sessions, `23:45` for short ones. */
    fun duration(totalSeconds: Long): String {
        val seconds = abs(totalSeconds)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(locale, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(locale, "%d:%02d", minutes, secs)
        }
    }

    /** Compact human duration, e.g. `1j 24m` or `45m`. */
    fun durationCompact(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}j ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${totalSeconds}d"
        }
    }

    // ---- Distance ---------------------------------------------------------

    fun distance(meters: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String = when (unitSystem) {
        UnitSystem.METRIC ->
            if (meters < 1000) "${meters.roundToInt()} m"
            else String.format(locale, "%.2f km", meters / 1000.0)

        UnitSystem.IMPERIAL -> {
            val miles = meters / METERS_PER_MILE
            if (miles < 0.1) "${(meters * FEET_PER_METER).roundToInt()} ft"
            else String.format(locale, "%.2f mi", miles)
        }
    }

    /** Just the number, for hero metrics that render their unit separately. */
    fun distanceValue(meters: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String {
        val value = if (unitSystem == UnitSystem.METRIC) meters / 1000.0 else meters / METERS_PER_MILE
        return String.format(locale, "%.2f", value)
    }

    fun distanceUnit(unitSystem: UnitSystem = UnitSystem.METRIC): String =
        if (unitSystem == UnitSystem.METRIC) "km" else "mi"

    // ---- Pace & speed -----------------------------------------------------

    /** `5:42` minutes per km/mile. Returns an em-dash placeholder when undefined. */
    fun pace(secondsPerKm: Double?, unitSystem: UnitSystem = UnitSystem.METRIC): String {
        if (secondsPerKm == null || secondsPerKm.isNaN() || secondsPerKm <= 0) return "--:--"
        val adjusted = if (unitSystem == UnitSystem.METRIC) secondsPerKm else secondsPerKm * KM_PER_MILE
        if (adjusted > 3600) return "--:--"
        val total = adjusted.roundToInt()
        return String.format(locale, "%d:%02d", total / 60, total % 60)
    }

    fun paceUnit(unitSystem: UnitSystem = UnitSystem.METRIC): String =
        if (unitSystem == UnitSystem.METRIC) "/km" else "/mi"

    fun speed(metersPerSecond: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String {
        val value = if (unitSystem == UnitSystem.METRIC) metersPerSecond * 3.6
        else metersPerSecond * 3.6 / KM_PER_MILE
        return String.format(locale, "%.1f", value)
    }

    fun speedUnit(unitSystem: UnitSystem = UnitSystem.METRIC): String =
        if (unitSystem == UnitSystem.METRIC) "km/j" else "mph"

    // ---- Body & energy ----------------------------------------------------

    fun weight(kg: Float, unitSystem: UnitSystem = UnitSystem.METRIC): String = when (unitSystem) {
        UnitSystem.METRIC -> String.format(locale, "%.1f kg", kg)
        UnitSystem.IMPERIAL -> String.format(locale, "%.1f lb", kg * POUNDS_PER_KG)
    }

    fun height(cm: Float, unitSystem: UnitSystem = UnitSystem.METRIC): String = when (unitSystem) {
        UnitSystem.METRIC -> "${cm.roundToInt()} cm"
        UnitSystem.IMPERIAL -> {
            val totalInches = (cm / CM_PER_INCH).roundToInt()
            "${totalInches / 12}'${totalInches % 12}\""
        }
    }

    fun elevation(meters: Double, unitSystem: UnitSystem = UnitSystem.METRIC): String = when (unitSystem) {
        UnitSystem.METRIC -> "${meters.roundToInt()} m"
        UnitSystem.IMPERIAL -> "${(meters * FEET_PER_METER).roundToInt()} ft"
    }

    /** Thousands-separated calories. */
    fun kcal(value: Int): String = String.format(locale, "%,d", value)

    fun grams(value: Float): String = "${value.roundToInt()} g"

    /** `+120` / `-340`, for deltas where the sign carries the meaning. */
    fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()

    fun percent(fraction: Float): String = "${(fraction * 100).roundToInt()}%"

    // ---- Unit conversion --------------------------------------------------

    const val METERS_PER_MILE = 1609.344
    const val KM_PER_MILE = 1.609344
    const val FEET_PER_METER = 3.28084
    const val POUNDS_PER_KG = 2.20462262
    const val CM_PER_INCH = 2.54

    fun kgToPounds(kg: Float): Float = kg * POUNDS_PER_KG.toFloat()
    fun poundsToKg(pounds: Float): Float = pounds / POUNDS_PER_KG.toFloat()
    fun cmToInches(cm: Float): Float = cm / CM_PER_INCH.toFloat()
    fun inchesToCm(inches: Float): Float = inches * CM_PER_INCH.toFloat()
}
