package com.vitalis.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vitalis.core.model.BodyMeasurement
import java.time.LocalDate

/**
 * One row per day. The unique index on [date] enforces that — a second weigh-in
 * updates the day rather than creating a duplicate that would double-count in
 * trend charts.
 */
@Entity(tableName = "body_measurement", indices = [Index(value = ["date"], unique = true)])
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val weightKg: Float?,
    val bodyFatPercent: Float?,
    val waistCm: Float?,
    val hipCm: Float?,
    val chestCm: Float?,
    val armCm: Float?,
    val thighCm: Float?,
    val restingHeartRate: Int?,
    val hrvMs: Float?,
    val systolic: Int?,
    val diastolic: Int?,
    val sleepMinutes: Int?,
    val spo2Percent: Float?,
    /** Internal-storage URI only — progress photos never touch the public MediaStore (spec §13.4). */
    val photoUri: String?,
    val note: String?,
    val isSynced: Boolean = false,
)

fun BodyMeasurementEntity.toDomain() = BodyMeasurement(
    id = id,
    date = date,
    weightKg = weightKg,
    bodyFatPercent = bodyFatPercent,
    waistCm = waistCm,
    hipCm = hipCm,
    chestCm = chestCm,
    armCm = armCm,
    thighCm = thighCm,
    restingHeartRate = restingHeartRate,
    hrvMs = hrvMs,
    systolic = systolic,
    diastolic = diastolic,
    sleepMinutes = sleepMinutes,
    spo2Percent = spo2Percent,
    photoUri = photoUri,
    note = note,
)

fun BodyMeasurement.toEntity() = BodyMeasurementEntity(
    id = id,
    date = date,
    weightKg = weightKg,
    bodyFatPercent = bodyFatPercent,
    waistCm = waistCm,
    hipCm = hipCm,
    chestCm = chestCm,
    armCm = armCm,
    thighCm = thighCm,
    restingHeartRate = restingHeartRate,
    hrvMs = hrvMs,
    systolic = systolic,
    diastolic = diastolic,
    sleepMinutes = sleepMinutes,
    spo2Percent = spo2Percent,
    photoUri = photoUri,
    note = note,
)
