package com.vitalis.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.ActivitySource
import com.vitalis.core.model.CalorieModel
import com.vitalis.core.model.Lap
import com.vitalis.core.model.SportType
import com.vitalis.core.model.TrackPoint
import java.time.Instant

@Entity(
    tableName = "activity_session",
    indices = [Index("startTime"), Index("sportType"), Index("isSynced")],
)
data class ActivitySessionEntity(
    @PrimaryKey val id: String,
    val sportType: SportType,
    val source: ActivitySource,
    val startTime: Instant,
    val endTime: Instant,
    val elapsedSeconds: Long,
    val movingSeconds: Long,
    val distanceMeters: Double?,
    val avgSpeedMps: Double?,
    val maxSpeedMps: Double?,
    val elevationGainM: Double?,
    val elevationLossM: Double?,
    val maxAltitudeM: Double?,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val avgCadence: Int?,
    val kcalGross: Int,
    val kcalNet: Int,
    val calorieModel: CalorieModel,
    val metValue: Double?,
    val trainingLoad: Double?,
    val title: String?,
    val note: String?,
    val encodedPolyline: String?,
    val isSynced: Boolean = false,
)

fun ActivitySessionEntity.toDomain() = ActivitySession(
    id = id,
    sportType = sportType,
    source = source,
    startTime = startTime,
    endTime = endTime,
    elapsedSeconds = elapsedSeconds,
    movingSeconds = movingSeconds,
    distanceMeters = distanceMeters,
    avgSpeedMps = avgSpeedMps,
    maxSpeedMps = maxSpeedMps,
    elevationGainM = elevationGainM,
    elevationLossM = elevationLossM,
    maxAltitudeM = maxAltitudeM,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    avgCadence = avgCadence,
    kcalGross = kcalGross,
    kcalNet = kcalNet,
    calorieModel = calorieModel,
    metValue = metValue,
    trainingLoad = trainingLoad,
    title = title,
    note = note,
    encodedPolyline = encodedPolyline,
    isSynced = isSynced,
)

fun ActivitySession.toEntity() = ActivitySessionEntity(
    id = id,
    sportType = sportType,
    source = source,
    startTime = startTime,
    endTime = endTime,
    elapsedSeconds = elapsedSeconds,
    movingSeconds = movingSeconds,
    distanceMeters = distanceMeters,
    avgSpeedMps = avgSpeedMps,
    maxSpeedMps = maxSpeedMps,
    elevationGainM = elevationGainM,
    elevationLossM = elevationLossM,
    maxAltitudeM = maxAltitudeM,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    avgCadence = avgCadence,
    kcalGross = kcalGross,
    kcalNet = kcalNet,
    calorieModel = calorieModel,
    metValue = metValue,
    trainingLoad = trainingLoad,
    title = title,
    note = note,
    encodedPolyline = encodedPolyline,
    isSynced = isSynced,
)

/**
 * Raw GPS trace. At 1 Hz a two-hour ride is ~7 200 rows, so inserts are batched
 * and the cascade delete guarantees no orphaned points survive a deleted session.
 */
@Entity(
    tableName = "location_point",
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
    val accuracyM: Float,
    val speedMps: Float?,
    val bearing: Float?,
    val timestamp: Long,
    val isFiltered: Boolean = false,
)

fun LocationPointEntity.toDomain() = TrackPoint(
    latitude = latitude,
    longitude = longitude,
    altitudeM = altitudeM,
    accuracyM = accuracyM,
    speedMps = speedMps,
    bearing = bearing,
    timestamp = timestamp,
    isFiltered = isFiltered,
)

fun TrackPoint.toEntity(sessionId: String) = LocationPointEntity(
    sessionId = sessionId,
    latitude = latitude,
    longitude = longitude,
    altitudeM = altitudeM,
    accuracyM = accuracyM,
    speedMps = speedMps,
    bearing = bearing,
    timestamp = timestamp,
    isFiltered = isFiltered,
)

@Entity(
    tableName = "lap",
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class LapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val lapIndex: Int,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val elevationGainM: Double,
    val avgHeartRate: Int?,
    val isManual: Boolean,
)

fun LapEntity.toDomain() = Lap(
    index = lapIndex,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds,
    elevationGainM = elevationGainM,
    avgHeartRate = avgHeartRate,
    isManual = isManual,
)

fun Lap.toEntity(sessionId: String) = LapEntity(
    sessionId = sessionId,
    lapIndex = index,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds,
    elevationGainM = elevationGainM,
    avgHeartRate = avgHeartRate,
    isManual = isManual,
)
