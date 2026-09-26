package com.vitalis.data

import com.vitalis.core.database.VitalisDatabase
import com.vitalis.core.database.entity.toDomain
import com.vitalis.core.model.ActivitySession
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ActivityRepository @Inject constructor(db: VitalisDatabase) {
    private val dao = db.activityDao()

    fun observeSessionsOn(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<ActivitySession>> =
        dao.observeStartedBetween(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
            .map { rows -> rows.map { it.toDomain() } }
}
