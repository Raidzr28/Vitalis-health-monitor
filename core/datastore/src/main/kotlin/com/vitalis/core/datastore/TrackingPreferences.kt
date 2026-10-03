package com.vitalis.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.vitalis.core.model.SportType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the sport picker's settings card controls. [targetKm] null = no target. */
data class TrackingSettings(
    val targetKm: Double? = null,
    val voiceCues: Boolean = true,
    val autoPause: Boolean = true,
)

private val Context.trackingStore by preferencesDataStore(name = "tracking_settings")

/**
 * Recording preferences. The distance target is per sport (5 km on foot is not 5 km on a bike);
 * voice cues and auto-pause are one choice for every sport.
 */
@Singleton
class TrackingPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val voice = booleanPreferencesKey("voice_cues")
    private val autoPause = booleanPreferencesKey("auto_pause")
    private fun target(sport: SportType) = doublePreferencesKey("target_km_${sport.name}")

    fun settings(sport: SportType): Flow<TrackingSettings> = context.trackingStore.data.map { p ->
        TrackingSettings(targetKm = p[target(sport)], voiceCues = p[voice] ?: true, autoPause = p[autoPause] ?: true)
    }

    suspend fun setTarget(sport: SportType, km: Double?) {
        context.trackingStore.edit { if (km == null) it.remove(target(sport)) else it[target(sport)] = km }
    }

    suspend fun setVoiceCues(on: Boolean) {
        context.trackingStore.edit { it[voice] = on }
    }

    suspend fun setAutoPause(on: Boolean) {
        context.trackingStore.edit { it[autoPause] = on }
    }
}
