package com.vitalis.data

import androidx.room.withTransaction
import com.vitalis.core.database.VitalisDatabase
import com.vitalis.core.database.entity.toDomain
import com.vitalis.core.database.entity.toEntity
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.Lap
import com.vitalis.core.model.TrackPoint
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ActivityRepository @Inject constructor(
    private val db: VitalisDatabase,
    private val gamification: GamificationRepository,
) {
    private val dao = db.activityDao()

    fun observeSessionsOn(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<ActivitySession>> =
        dao.observeStartedBetween(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
            .map { rows -> rows.map { it.toDomain() } }

    /** A GPS recording is saved whole or not at all, XP and records included. */
    suspend fun saveRecorded(session: ActivitySession, route: List<TrackPoint>, laps: List<Lap>) = db.withTransaction {
        dao.insertSession(session.toEntity())
        dao.insertPoints(route.map { it.toEntity(session.id) })
        dao.insertLaps(laps.map { it.toEntity(session.id) })
        gamification.onActivitySaved(session)
    }
}
