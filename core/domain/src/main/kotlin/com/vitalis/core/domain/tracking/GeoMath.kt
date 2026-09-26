package com.vitalis.core.domain.tracking

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle geometry (spec §5.9). */
object GeoMath {

    /** Mean Earth radius in metres. */
    const val EARTH_RADIUS_M = 6_371_000.0

    /**
     * Haversine distance between two coordinates.
     *
     * Chosen over the flat-earth approximation because hill and trail routes fold
     * back on themselves constantly; small per-segment errors accumulate into a
     * visibly wrong total over a few thousand points.
     */
    fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val sinHalfPhi = sin(deltaPhi / 2)
        val sinHalfLambda = sin(deltaLambda / 2)

        val a = sinHalfPhi * sinHalfPhi +
            cos(phi1) * cos(phi2) * sinHalfLambda * sinHalfLambda
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /** Initial bearing from point 1 to point 2, in degrees clockwise from north. */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Grade as a percentage: `rise / run × 100`. Guards against a zero run. */
    fun gradePercent(elevationChangeM: Double, horizontalDistanceM: Double): Double {
        if (horizontalDistanceM < 1.0) return 0.0
        return (elevationChangeM / horizontalDistanceM) * 100.0
    }
}

/**
 * Google's Encoded Polyline Algorithm.
 *
 * A two-hour run holds ~7 200 raw fixes. Storing that per activity and decoding it
 * to draw a 200 dp list thumbnail would make the history screen unusable, so each
 * session also keeps a compressed route string for previews (spec §7.2).
 */
object PolylineCodec {

    fun encode(points: List<Pair<Double, Double>>): String {
        val builder = StringBuilder()
        var previousLat = 0L
        var previousLng = 0L

        points.forEach { (lat, lng) ->
            val scaledLat = Math.round(lat * 1e5)
            val scaledLng = Math.round(lng * 1e5)
            encodeValue(scaledLat - previousLat, builder)
            encodeValue(scaledLng - previousLng, builder)
            previousLat = scaledLat
            previousLng = scaledLng
        }
        return builder.toString()
    }

    fun decode(encoded: String): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var value = 0
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                value = value or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20 && index < encoded.length)
            lat += if (value and 1 != 0) (value shr 1).inv() else value shr 1

            shift = 0
            value = 0
            do {
                byte = encoded[index++].code - 63
                value = value or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20 && index < encoded.length)
            lng += if (value and 1 != 0) (value shr 1).inv() else value shr 1

            result += (lat / 1e5) to (lng / 1e5)
        }
        return result
    }

    private fun encodeValue(value: Long, builder: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else (value shl 1)
        while (v >= 0x20) {
            builder.append(((0x20 or (v and 0x1f).toInt()) + 63).toChar())
            v = v shr 5
        }
        builder.append((v.toInt() + 63).toChar())
    }
}

/**
 * Ramer–Douglas–Peucker simplification, used to downsample routes older than
 * 90 days so the database does not grow without bound (spec §7.2).
 */
object RouteSimplifier {

    /**
     * @param toleranceMeters points closer than this to the simplified line are dropped
     */
    fun simplify(
        points: List<Pair<Double, Double>>,
        toleranceMeters: Double = 5.0,
    ): List<Pair<Double, Double>> {
        if (points.size < 3) return points
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        simplifySegment(points, 0, points.lastIndex, toleranceMeters, keep)
        return points.filterIndexed { index, _ -> keep[index] }
    }

    private fun simplifySegment(
        points: List<Pair<Double, Double>>,
        start: Int,
        end: Int,
        tolerance: Double,
        keep: BooleanArray,
    ) {
        if (end <= start + 1) return

        var maxDistance = 0.0
        var maxIndex = start

        for (i in (start + 1) until end) {
            val distance = perpendicularDistance(points[i], points[start], points[end])
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }

        if (maxDistance > tolerance) {
            keep[maxIndex] = true
            simplifySegment(points, start, maxIndex, tolerance, keep)
            simplifySegment(points, maxIndex, end, tolerance, keep)
        }
    }

    /**
     * Distance from [point] to the segment [lineStart]–[lineEnd], in metres.
     * Works in a local flat projection, which is accurate enough over the few
     * hundred metres any single segment spans.
     */
    private fun perpendicularDistance(
        point: Pair<Double, Double>,
        lineStart: Pair<Double, Double>,
        lineEnd: Pair<Double, Double>,
    ): Double {
        val latScale = 111_320.0
        val lonScale = 111_320.0 * cos(Math.toRadians(lineStart.first))

        val x = (point.second - lineStart.second) * lonScale
        val y = (point.first - lineStart.first) * latScale
        val dx = (lineEnd.second - lineStart.second) * lonScale
        val dy = (lineEnd.first - lineStart.first) * latScale

        val segmentLengthSq = dx * dx + dy * dy
        if (segmentLengthSq == 0.0) return sqrt(x * x + y * y)

        val t = ((x * dx + y * dy) / segmentLengthSq).coerceIn(0.0, 1.0)
        val projX = t * dx
        val projY = t * dy
        return sqrt((x - projX) * (x - projX) + (y - projY) * (y - projY))
    }
}
