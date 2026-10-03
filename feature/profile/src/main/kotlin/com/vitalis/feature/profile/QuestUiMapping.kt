package com.vitalis.feature.profile

import com.vitalis.core.common.format.Formatters
import com.vitalis.core.model.Quest
import com.vitalis.core.model.QuestType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.floor

private fun whole(v: Double) = Formatters.kcal(v.toInt())

internal fun QuestType.title(target: Double): String = when (this) {
    QuestType.DISTANCE_KM -> "Tempuh ${whole(target)} km"
    QuestType.ELEVATION_M -> "Naik total ${whole(target)} m"
    QuestType.ACTIVITY_COUNT -> "Rekam ${whole(target)} aktivitas"
    QuestType.LOG_DAYS -> "Catat makan ${whole(target)} hari"
    QuestType.PROTEIN_DAYS -> "Target protein ${whole(target)} hari"
    QuestType.STEP_DAYS -> "Target langkah ${whole(target)} hari"
}

/** "8,4 / 15 km", "3 / 5 hari"; rounded down so a bar never reads as done early. */
internal fun Quest.progressLabel(): String {
    val shown = current.coerceAtMost(target)
    return when (type) {
        QuestType.DISTANCE_KM -> "${String.format(Locale.getDefault(), "%.1f", floor(shown * 10) / 10)} / ${whole(target)} km"
        QuestType.ELEVATION_M -> "${whole(floor(shown))} / ${whole(target)} m"
        QuestType.ACTIVITY_COUNT -> "${whole(shown)} / ${whole(target)}"
        else -> "${whole(shown)} / ${whole(target)} hari"
    }
}

fun questUi(q: Quest): QuestUi = QuestUi(
    name = q.type.title(q.target),
    progressLabel = if (q.isComplete) "Selesai" else q.progressLabel(),
    fraction = q.fraction,
    xp = q.xpReward,
    done = q.isComplete,
)

/** Quests reset on Monday: "Sisa 3 hari", or "Berakhir hari ini" on Sunday. */
fun questsEndLabel(today: LocalDate, weekStart: LocalDate): String =
    when (val left = ChronoUnit.DAYS.between(today, weekStart.plusDays(7))) {
        1L -> "Berakhir hari ini"
        else -> "Sisa $left hari"
    }
