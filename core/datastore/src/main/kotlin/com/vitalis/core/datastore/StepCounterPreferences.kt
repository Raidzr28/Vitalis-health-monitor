package com.vitalis.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** The previous hardware counter reading and when it was taken. */
data class StepReading(val counter: Long, val atEpochMs: Long)

private val Context.stepStore by preferencesDataStore(name = "step_counter")

@Singleton
class StepCounterPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val counter = longPreferencesKey("counter")
    private val at = longPreferencesKey("at_epoch_ms")

    suspend fun last(): StepReading? = context.stepStore.data.first().let { p ->
        val c = p[counter] ?: return null
        StepReading(c, p[at] ?: return null)
    }

    suspend fun save(reading: StepReading) {
        context.stepStore.edit {
            it[counter] = reading.counter
            it[at] = reading.atEpochMs
        }
    }
}
