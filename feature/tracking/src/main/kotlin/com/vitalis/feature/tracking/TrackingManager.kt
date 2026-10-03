package com.vitalis.feature.tracking

import com.vitalis.core.datastore.TrackingSettings
import com.vitalis.core.model.SportType
import com.vitalis.core.model.TrackingState
import com.vitalis.core.model.TrackingStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single owner of the in-progress recording, shared by [TrackingService] (writes fixes and ticks)
 * and the tracking screens (read state, send pause/stop). Main-thread only, like [TrackingSession].
 */
@Singleton
class TrackingManager @Inject constructor() {
    private var session: TrackingSession? = null

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    /** Set on stop, cleared once the user saves or discards on the summary screen. */
    private val _finished = MutableStateFlow<FinishedActivity?>(null)
    val finished: StateFlow<FinishedActivity?> = _finished.asStateFlow()

    val isActive: Boolean get() = session != null

    /** Read by [TrackingService] to decide whether to speak. */
    var voiceCues: Boolean = false
        private set

    fun start(sport: SportType, weightKg: Double, bmrKcal: Int, settings: TrackingSettings = TrackingSettings(), now: Long = System.currentTimeMillis()) {
        if (session != null) return
        voiceCues = settings.voiceCues
        session = TrackingSession(
            sport, weightKg, bmrKcal, now,
            autoPauseEnabled = settings.autoPause,
            targetMeters = settings.targetKm?.times(1000),
        ).also { _state.value = it.state() }
    }

    fun tick(now: Long = System.currentTimeMillis()) = update { tick(now) }

    fun onFix(lat: Double, lng: Double, altitudeM: Double?, accuracyM: Float, speedMps: Float?, bearing: Float?, timeMs: Long) =
        update { onFix(lat, lng, altitudeM, accuracyM, speedMps, bearing, timeMs) }

    fun togglePause(now: Long = System.currentTimeMillis()) = update { togglePause(now) }

    fun stop(now: Long = System.currentTimeMillis()) {
        val s = session ?: return
        _finished.value = s.finish(now)
        session = null
        _state.value = TrackingState(status = TrackingStatus.STOPPED, sportType = s.sport)
    }

    fun clearFinished() {
        _finished.value = null
        _state.value = TrackingState()
    }

    private inline fun update(block: TrackingSession.() -> Unit) {
        val s = session ?: return
        s.block()
        _state.value = s.state()
    }
}
