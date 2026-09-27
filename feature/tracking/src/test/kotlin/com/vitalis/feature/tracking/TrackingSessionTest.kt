package com.vitalis.feature.tracking

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.SportType
import com.vitalis.core.model.TrackingStatus
import org.junit.Test

class TrackingSessionTest {
    private val t0 = 1_000_000L
    private val metersPerDegLat = 111_195.0

    private fun session() = TrackingSession(SportType.RUNNING, weightKg = 70.0, bmrKcal = 1_600, startMs = t0)

    /** A 5 m-accuracy fix [meters] north of the start, [sec] seconds in. */
    private fun TrackingSession.fix(sec: Int, meters: Double) =
        onFix(-6.2 + meters / metersPerDegLat, 106.8, null, 5f, null, null, t0 + sec * 1000L)

    @Test
    fun `steady run accrues distance, moving time and 1 km laps`() {
        val s = session()
        for (sec in 1..400) s.fix(sec, sec * 3.0)
        val done = s.finish(t0 + 400_000)

        assertThat(done.session.movingSeconds).isEqualTo(400)
        // Warm-up drops the first 3 fixes (9 m), the Kalman lag a few more.
        assertThat(done.session.distanceMeters!!).isWithin(20.0).of(1188.0)
        assertThat(done.laps.map { it.index }).containsExactly(1, 2).inOrder()
        assertThat(done.laps.first().distanceMeters).isEqualTo(1000.0)
        assertThat(done.session.kcalNet).isGreaterThan(0)
    }

    @Test
    fun `manual pause freezes the clock and does not bridge the gap on resume`() {
        val s = session()
        for (sec in 1..100) s.fix(sec, sec * 3.0)
        s.togglePause(t0 + 100_000)
        for (sec in 101..160) s.fix(sec, 300 + (sec - 100) * 3.0) // 180 m walked while paused
        s.togglePause(t0 + 160_000)
        for (sec in 161..260) s.fix(sec, 480 + (sec - 160) * 3.0)

        val st = s.state()
        assertThat(st.movingSeconds).isEqualTo(200)
        // 288 m + 297 m; bridging the pause would add another ~180 m.
        assertThat(st.distanceMeters).isWithin(20.0).of(585.0)
    }

    @Test
    fun `standing still auto-pauses after the dwell`() {
        val s = session()
        for (sec in 1..60) s.fix(sec, sec * 3.0)
        for (sec in 61..120) s.fix(sec, 180.0)

        val st = s.state()
        assertThat(st.status).isEqualTo(TrackingStatus.PAUSED_AUTO)
        assertThat(st.movingSeconds).isIn(Range.closed(60L, 75L))
    }
}
