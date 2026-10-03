package com.vitalis.core.database.entity

import androidx.room.Entity
import java.time.LocalDate

/**
 * Steps per clock hour (0..23). The day total also lives in `daily_stats.steps` for the screens
 * that only need the number; this table feeds the hourly chart and the Health Score average.
 */
@Entity(tableName = "step_hour", primaryKeys = ["date", "hour"])
data class StepHourEntity(
    val date: LocalDate,
    val hour: Int,
    val steps: Int,
)

data class DaySteps(val date: LocalDate, val steps: Int)
