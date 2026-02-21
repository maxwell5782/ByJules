package com.example.mocklocation

import org.osmdroid.util.GeoPoint

object LocationUtils {
    fun interpolate(start: GeoPoint, end: GeoPoint, fraction: Double): GeoPoint {
        val lat = start.latitude + (end.latitude - start.latitude) * fraction
        val lon = start.longitude + (end.longitude - start.longitude) * fraction
        return GeoPoint(lat, lon)
    }
}
