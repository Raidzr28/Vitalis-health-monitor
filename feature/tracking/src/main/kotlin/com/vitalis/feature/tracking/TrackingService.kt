package com.vitalis.feature.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.label
import com.vitalis.core.model.SportProfile
import com.vitalis.core.model.TrackingState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps GPS recording alive with the screen off (spec §8.2). Owns no session state: it pipes
 * FusedLocation fixes and a 1 s clock into [TrackingManager] and mirrors the result in the
 * persistent notification. Started by [TrackingViewModel.start], stopped via `stopService`.
 */
@AndroidEntryPoint
class TrackingService : LifecycleService() {
    @Inject lateinit var manager: TrackingManager

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val notifications by lazy { getSystemService(NotificationManager::class.java) }
    private var running = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { l ->
                manager.onFix(
                    l.latitude, l.longitude,
                    if (l.hasAltitude()) l.altitude else null,
                    l.accuracy,
                    if (l.hasSpeed()) l.speed else null,
                    if (l.hasBearing()) l.bearing else null,
                    l.time,
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // ponytail: no restart after process death — the session lives in memory. Crash recovery
        // from TrackingSnapshot (spec §8.6) is the upgrade path.
        if (running) return START_NOT_STICKY
        if (!manager.isActive || !hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        running = true

        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Perekaman aktivitas", NotificationManager.IMPORTANCE_LOW),
        )
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(manager.state.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        requestUpdates()

        lifecycleScope.launch {
            while (isActive) {
                delay(1_000)
                manager.tick()
                notifications.notify(NOTIFICATION_ID, notification(manager.state.value))
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission") // checked in onStartCommand
    private fun requestUpdates() {
        val profile = SportProfile.forSport(manager.state.value.sportType)
        // Spec §8.1: no distance filter here (the pipeline filters), small batching for battery.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, profile.samplingIntervalMs)
            .setMinUpdateIntervalMillis(profile.samplingIntervalMs / 2)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateDelayMillis(profile.samplingIntervalMs * 2)
            .build()
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        super.onDestroy()
    }

    private fun notification(s: TrackingState): Notification {
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(s.sportType.label() + if (s.isPaused) " · dijeda" else "")
            .setContentText("${Formatters.duration(s.movingSeconds)} · ${Formatters.distance(s.distanceMeters)} · ${Formatters.pace(s.avgPaceSecPerKm)}${Formatters.paceUnit()}")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun hasLocationPermission() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private companion object {
        const val CHANNEL_ID = "tracking"
        const val NOTIFICATION_ID = 42
    }
}
