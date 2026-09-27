package com.vitalis.feature.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.label
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.domain.tracking.LocationPipeline
import com.vitalis.core.model.PersonalRecord
import com.vitalis.core.model.RecordType
import com.vitalis.core.model.SportType
import com.vitalis.core.model.TrackPoint
import com.vitalis.core.model.TrackingState
import com.vitalis.core.model.XpAction
import com.vitalis.data.ActivityRepository
import com.vitalis.data.ActivityRewards
import com.vitalis.data.GamificationRepository
import com.vitalis.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class TrackingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: TrackingManager,
    private val users: UserRepository,
    private val activities: ActivityRepository,
    private val gamification: GamificationRepository,
) : ViewModel() {
    private val _sport = MutableStateFlow(SportType.RUNNING)
    val sport: StateFlow<SportType> = _sport.asStateFlow()

    private val _gpsAccuracyM = MutableStateFlow<Float?>(null)
    val gpsAccuracyM: StateFlow<Float?> = _gpsAccuracyM.asStateFlow()

    val tracking: StateFlow<TrackingState> = manager.state

    val live: StateFlow<LiveUiState> = manager.state.map { it.toLiveUi() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), manager.state.value.toLiveUi())

    val summary: StateFlow<SummaryUiState?> = manager.finished.map { f -> f?.toSummaryUi(gamification.activityRewards(f.session)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var saving = false

    fun selectSport(sport: SportType) { _sport.value = sport }

    /** One fix for the pre-start signal badge; recording itself runs in [TrackingService]. */
    @SuppressLint("MissingPermission") // only called once location permission is granted
    fun refreshGps() {
        viewModelScope.launch {
            runCatching {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            }.getOrNull()?.let { _gpsAccuracyM.value = it.accuracy }
        }
    }

    /** [onStarted] runs once the session exists — navigate only then, or this ViewModel's scope may die first. */
    fun start(onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            // Onboarding guarantees both; the fallbacks only keep a half-migrated install from crashing.
            val weightKg = users.latestWeightKg.first()?.toDouble() ?: 70.0
            val bmr = users.observeDay(LocalDate.now()).first()?.targets?.bmrKcal ?: 1_500
            manager.start(_sport.value, weightKg, bmr)
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
            onStarted()
        }
    }

    fun togglePause() = manager.togglePause()

    fun stop() {
        manager.stop()
        context.stopService(Intent(context, TrackingService::class.java))
    }

    fun save(onDone: () -> Unit) {
        val finished = manager.finished.value ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            activities.saveRecorded(finished.session, finished.route, finished.laps)
            onDone()
            manager.clearFinished()
        }
    }

    fun discard() = manager.clearFinished()
}

internal val TrackingPermissions = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

/**
 * Opens [onOpenLive] whenever a recording is running, so the Track tab always lands on it.
 * Without precise location, Start goes to [onNeedPermission] (the priming screen) instead.
 */
@Composable
fun SportSelectRoute(onOpenLive: () -> Unit, onNeedPermission: (SportType) -> Unit, vm: TrackingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val sport by vm.sport.collectAsStateWithLifecycle()
    val accuracy by vm.gpsAccuracyM.collectAsStateWithLifecycle()
    val tracking by vm.tracking.collectAsStateWithLifecycle()
    val recording = tracking.isRecording || tracking.isPaused
    LaunchedEffect(recording) { if (recording) onOpenLive() }

    // Location is already granted here; this only asks for notifications the first time (Android 13+).
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            vm.refreshGps()
            vm.start()
        } else {
            onNeedPermission(sport) // revoked while the screen was open
        }
    }
    LaunchedEffect(Unit) { if (context.hasFineLocation()) vm.refreshGps() }

    SportSelectScreen(
        selected = sport,
        lastUsed = emptyMap(),
        gps = LocationPipeline.signalQuality(accuracy),
        gpsAccuracyM = accuracy?.roundToInt(),
        onSelect = vm::selectSport,
        onStart = { if (context.hasFineLocation()) permissions.launch(TrackingPermissions) else onNeedPermission(sport) },
    )
}

/** Back leaves the screen but not the recording — the service keeps going. */
@Composable
fun LiveTrackingRoute(onStopped: () -> Unit, onBack: () -> Unit, vm: TrackingViewModel = hiltViewModel()) {
    val state by vm.live.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    LiveTrackingScreen(
        state = state,
        onTogglePause = vm::togglePause,
        onLap = {}, // ponytail: auto 1 km laps only; manual laps need a LapTracker API
        onStop = { vm.stop(); onStopped() },
        onLock = {},
    )
}

@Composable
fun ActivitySummaryRoute(onBack: () -> Unit, onDone: () -> Unit, vm: TrackingViewModel = hiltViewModel()) {
    val state by vm.summary.collectAsStateWithLifecycle()
    val s = state ?: return Box(Modifier.fillMaxSize().background(VitalisColors.Ground))
    ActivitySummaryScreen(s, onBack, onShare = {}, onSave = { vm.save(onDone) }, onDiscard = { onDone(); vm.discard() })
}

internal fun TrackingState.toLiveUi() = LiveUiState(
    sport = sportType,
    distanceM = distanceMeters,
    movingSeconds = movingSeconds,
    currentPaceSecPerKm = currentPaceSecPerKm,
    kcalGross = kcalGross,
    heartRate = heartRate,
    hrZone = null,
    isPaused = isPaused,
    route = normalizeRoute(route),
)

internal fun FinishedActivity.toSummaryUi(rewards: ActivityRewards, zone: ZoneId = ZoneId.systemDefault()): SummaryUiState {
    val s = session
    val id = Locale.forLanguageTag("id")
    val clock = DateTimeFormatter.ofPattern("HH.mm", id)
    val start = s.startTime.atZone(zone)
    val distance = s.distanceMeters ?: 0.0
    val gain = s.elevationGainM ?: 0.0
    return SummaryUiState(
        title = s.sportType.label(),
        subtitle = "${start.format(DateTimeFormatter.ofPattern("EEEE, d MMM", id))} · ${start.format(clock)}–${s.endTime.atZone(zone).format(clock)}",
        distanceM = distance,
        movingSeconds = s.movingSeconds,
        avgPaceSecPerKm = s.avgPaceSecPerKm ?: 0.0,
        elevationGainM = gain,
        elevationLossM = s.elevationLossM ?: 0.0,
        avgHeartRate = s.avgHeartRate,
        cadenceSpm = s.avgCadence,
        kcalGross = s.kcalGross,
        kcalNet = s.kcalNet,
        splits = laps.map { SplitUi(if (it.distanceMeters >= 1000.0) "${it.index}" else Formatters.distanceValue(it.distanceMeters), it.paceSecPerKm.roundToInt()) },
        elevationProfile = elevationProfile(route),
        route = normalizeRoute(route),
        newRecord = rewards.beaten.minByOrNull { HeadlineOrder.indexOf(it.recordType) }?.let { pr ->
            PrUi("Rekor baru: ${pr.recordType.label()}", "${pr.format(pr.value)} · sebelumnya ${pr.format(pr.previousValue!!)}", XpAction.BREAK_PERSONAL_RECORD.baseXp)
        },
        xpBreakdown = rewards.awards.map { it.label() to it.amount },
    )
}

/** Which beaten record headlines the summary: the longest race distance wins. */
private val HeadlineOrder = listOf(
    RecordType.FASTEST_MARATHON, RecordType.FASTEST_HALF_MARATHON, RecordType.FASTEST_10K, RecordType.FASTEST_5K,
    RecordType.FASTEST_1K, RecordType.LONGEST_DISTANCE, RecordType.MOST_ELEVATION_GAIN, RecordType.HIGHEST_ALTITUDE,
    RecordType.LONGEST_DURATION, RecordType.FASTEST_AVG_PACE,
)

private fun RecordType.label() = when (this) {
    RecordType.FASTEST_1K -> "1K tercepat"
    RecordType.FASTEST_5K -> "5K tercepat"
    RecordType.FASTEST_10K -> "10K tercepat"
    RecordType.FASTEST_HALF_MARATHON -> "half marathon tercepat"
    RecordType.FASTEST_MARATHON -> "marathon tercepat"
    RecordType.LONGEST_DISTANCE -> "jarak terjauh"
    RecordType.LONGEST_DURATION -> "durasi terlama"
    RecordType.MOST_ELEVATION_GAIN -> "elevasi terbanyak"
    RecordType.FASTEST_AVG_PACE -> "pace rata-rata tercepat"
    RecordType.HIGHEST_ALTITUDE -> "titik tertinggi"
}

private fun PersonalRecord.format(v: Double) = when (recordType) {
    RecordType.LONGEST_DISTANCE -> Formatters.distance(v)
    RecordType.MOST_ELEVATION_GAIN, RecordType.HIGHEST_ALTITUDE -> Formatters.elevation(v)
    RecordType.FASTEST_AVG_PACE -> Formatters.pace(v) + Formatters.paceUnit()
    else -> Formatters.duration(v.toLong())
}


/**
 * Projects lat/lng into the 0..1 box [RouteMap] draws in, keeping the route's shape
 * (longitude shrunk by cos(latitude)) with a 10% margin.
 * ponytail: equirectangular fit into a square; maps-compose replaces this with real tiles.
 */
internal fun normalizeRoute(points: List<TrackPoint>): List<Offset> {
    if (points.size < 2) return emptyList()
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLng = points.minOf { it.longitude }
    val maxLng = points.maxOf { it.longitude }
    val midLat = (minLat + maxLat) / 2
    val midLng = (minLng + maxLng) / 2
    val k = cos(Math.toRadians(midLat))
    val span = max((maxLng - minLng) * k, maxLat - minLat)
    if (span <= 0.0) return emptyList()
    val scale = 0.8 / span
    return points.map {
        Offset((0.5 + (it.longitude - midLng) * k * scale).toFloat(), (0.5 - (it.latitude - midLat) * scale).toFloat())
    }
}

/** Up to ~40 altitude samples normalised to 0..1 for the summary chart. */
internal fun elevationProfile(points: List<TrackPoint>): List<Float> {
    val alts = points.mapNotNull { it.altitudeM }
    if (alts.size < 2) return emptyList()
    val sampled = alts.filterIndexed { i, _ -> i % max(1, alts.size / 40) == 0 }
    val lo = sampled.min()
    val range = sampled.max() - lo
    return sampled.map { if (range <= 0) 0.5f else ((it - lo) / range).toFloat() }
}
