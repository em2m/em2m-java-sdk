package io.em2m.geo.math

import org.locationtech.jts.geom.Coordinate
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GreatCircle {

    const val EarthRadius = 6372797.560856

    fun distance(lon1: Double, lat1: Double, lon2: Double, lat2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return EarthRadius * c
    }

    fun distance(start: Coordinate, end: Coordinate): Double {
        return distance(start.x, start.y, end.x, end.y)
    }
}
