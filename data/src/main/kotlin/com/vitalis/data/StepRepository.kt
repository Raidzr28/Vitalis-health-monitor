package com.vitalis.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vitalis.core.database.VitalisDatabase
import com.vitalis.core.database.entity.DailyStatsEntity
import com.vitalis.core.database.entity.StepHourEntity
import com.vitalis.core.datastore.StepCounterPreferences
import com.vitalis.core.datastore.StepReading
import com.vitalis.core.domain.energy.StepMath
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Whether steps can be counted on this phone right now. */
enum class StepTracking { UNSUPPORTED, NEEDS_PERMISSION, ACTIVE }

/**
 * Step counting from the hardware `TYPE_STEP_COUNTER` (spec §4.3.1): the chip counts even while
 * the app is closed, so the app only samples it and books the difference per hour. Sampled when
 * Today opens and every 15 minutes by [StepSyncWorker].
 */
@Singleton
class StepRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: VitalisDatabase,
    private val lastReading: StepCounterPreferences,
    private val rewards: GamificationRepository,
) {
    private val sensors by lazy { context.getSystemService(SensorManager::class.java) }
    private val counter: Sensor? get() = sensors?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val mutex = Mutex() // the worker and the Today screen can sync at the same moment

    fun tracking(): StepTracking = when {
        counter == null -> StepTracking.UNSUPPORTED
        Build.VERSION.SDK_INT >= 29 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED ->
            StepTracking.NEEDS_PERMISSION
        else -> StepTracking.ACTIVE
    }

    /** Steps per hour (index 0..23) for [date]. */
    fun observeHours(date: LocalDate): Flow<List<Int>> = db.stepDao().observeDay(date).map { rows ->
        val byHour = rows.associate { it.hour to it.steps }
        List(24) { byHour[it] ?: 0 }
    }

    /** Idempotent; safe to call often. Enqueues nothing unless counting is possible. */
    fun scheduleBackgroundSync() {
        if (tracking() != StepTracking.ACTIVE) return
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "step-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES).build(),
        )
    }

    /**
     * Reads the counter and books the steps since the last reading. The very first reading only
     * sets the baseline: the chip cannot say which of its steps happened before the app was allowed to ask.
     */
    suspend fun sync(zone: ZoneId = ZoneId.systemDefault()) = mutex.withLock {
        if (tracking() != StepTracking.ACTIVE) return@withLock
        val now = System.currentTimeMillis()
        val value = readCounter() ?: return@withLock
        val previous = lastReading.last()
        if (previous != null) {
            val steps = StepMath.delta(previous.counter, value)
            val buckets = StepMath.distribute(
                steps,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(previous.atEpochMs), zone),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone),
            )
            if (buckets.isNotEmpty()) book(buckets)
        }
        lastReading.save(StepReading(value, now))
    }

    private suspend fun book(buckets: Map<LocalDateTime, Int>) = db.withTransaction {
        val steps = db.stepDao()
        buckets.forEach { (hour, n) ->
            val date = hour.toLocalDate()
            steps.upsert(StepHourEntity(date, hour.hour, (steps.steps(date, hour.hour) ?: 0) + n))
        }
        buckets.keys.map { it.toLocalDate() }.toSet().forEach { date ->
            val total = steps.dayTotal(date)
            val day = db.userDao().dailyStats(date) ?: DailyStatsEntity(date = date)
            db.userDao().upsertDailyStats(day.copy(steps = total))
            // Target comes from the day's snapshot; a day not opened yet gets it (and the XP) on a later sync.
            if (day.stepsTarget in 1..total) rewards.onStepTargetHit(date)
        }
    }

    /** The counter's current value, or null if the chip stays silent (some only report on the next step). */
    private suspend fun readCounter(): Long? {
        val sensor = counter ?: return null
        val manager = sensors ?: return null
        return withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values[0].toLong())
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { manager.unregisterListener(listener) }
            }
        }
    }
}

/** Samples the step counter every 15 minutes so steps land in the right hour even with the app closed. */
class StepSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun steps(): StepRepository
    }

    override suspend fun doWork(): Result {
        EntryPointAccessors.fromApplication(applicationContext, Deps::class.java).steps().sync()
        return Result.success()
    }
}
