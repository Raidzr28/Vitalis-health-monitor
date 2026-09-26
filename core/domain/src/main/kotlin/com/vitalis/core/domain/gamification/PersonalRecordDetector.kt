package com.vitalis.core.domain.gamification

import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.PersonalRecord
import com.vitalis.core.model.RecordType
import com.vitalis.core.model.SportType
import java.util.UUID

/**
 * Detects personal records after a session is saved (spec §9.6).
 *
 * Records are scoped per sport — a cycling 10 km and a running 10 km are not
 * comparable — and time-based records only count when the session actually
 * covered the distance, so a 3 km run can never set a "fastest 5K".
 */
object PersonalRecordDetector {

    /** Distance records, in metres, that a session may qualify for. */
    private val DISTANCE_RECORDS = listOf(
        1_000.0 to RecordType.FASTEST_1K,
        5_000.0 to RecordType.FASTEST_5K,
        10_000.0 to RecordType.FASTEST_10K,
        21_097.5 to RecordType.FASTEST_HALF_MARATHON,
        42_195.0 to RecordType.FASTEST_MARATHON,
    )

    /**
     * @param session the just-completed activity
     * @param existing the user's current records for this sport
     * @return records that were beaten, already carrying the new value
     */
    fun detect(session: ActivitySession, existing: List<PersonalRecord>): List<PersonalRecord> {
        val results = mutableListOf<PersonalRecord>()
        val currentBySport = existing.filter { it.sportType == session.sportType }.associateBy { it.recordType }

        fun consider(type: RecordType, value: Double, lowerIsBetter: Boolean) {
            val previous = currentBySport[type]?.value
            val isBetter = previous == null || if (lowerIsBetter) value < previous else value > previous
            if (!isBetter) return
            results += PersonalRecord(
                id = UUID.randomUUID().toString(),
                sportType = session.sportType,
                recordType = type,
                value = value,
                sessionId = session.id,
                achievedAt = session.endTime,
                previousValue = previous,
            )
        }

        val distance = session.distanceMeters ?: 0.0

        if (distance > 0) {
            consider(RecordType.LONGEST_DISTANCE, distance, lowerIsBetter = false)

            // Estimated split time for each standard distance the session covered.
            // Exact splits need the raw point stream; this whole-session projection
            // is the honest approximation available at save time.
            DISTANCE_RECORDS.forEach { (meters, type) ->
                if (distance >= meters && session.movingSeconds > 0) {
                    val projectedSeconds = session.movingSeconds * (meters / distance)
                    consider(type, projectedSeconds, lowerIsBetter = true)
                }
            }

            session.avgPaceSecPerKm?.let { consider(RecordType.FASTEST_AVG_PACE, it, lowerIsBetter = true) }
        }

        if (session.movingSeconds > 0) {
            consider(RecordType.LONGEST_DURATION, session.movingSeconds.toDouble(), lowerIsBetter = false)
        }

        if (session.sportType.tracksElevation) {
            session.elevationGainM?.takeIf { it > 0 }
                ?.let { consider(RecordType.MOST_ELEVATION_GAIN, it, lowerIsBetter = false) }
            session.maxAltitudeM?.let { consider(RecordType.HIGHEST_ALTITUDE, it, lowerIsBetter = false) }
        }

        return results
    }

    /** Record types that make sense to display for a given sport. */
    fun applicableRecords(sportType: SportType): List<RecordType> = buildList {
        add(RecordType.LONGEST_DISTANCE)
        add(RecordType.LONGEST_DURATION)
        if (sportType.isFootSport) {
            add(RecordType.FASTEST_1K)
            add(RecordType.FASTEST_5K)
            add(RecordType.FASTEST_10K)
            add(RecordType.FASTEST_HALF_MARATHON)
            add(RecordType.FASTEST_MARATHON)
        }
        add(RecordType.FASTEST_AVG_PACE)
        if (sportType.tracksElevation) {
            add(RecordType.MOST_ELEVATION_GAIN)
            add(RecordType.HIGHEST_ALTITUDE)
        }
    }
}
