package com.example.mocklocation

import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.util.GeoPoint

class InterpolationTest {

    @Test
    fun testInterpolate() {
        val start = GeoPoint(0.0, 0.0)
        val end = GeoPoint(10.0, 10.0)
        val fraction = 0.5

        val result = LocationUtils.interpolate(start, end, fraction)
        assertEquals(5.0, result.latitude, 0.0001)
        assertEquals(5.0, result.longitude, 0.0001)
    }

    @Test
    fun testInterpolateStart() {
        val start = GeoPoint(25.0, 121.0)
        val end = GeoPoint(26.0, 122.0)
        val fraction = 0.0

        val result = LocationUtils.interpolate(start, end, fraction)
        assertEquals(25.0, result.latitude, 0.0001)
        assertEquals(121.0, result.longitude, 0.0001)
    }

    @Test
    fun testInterpolateEnd() {
        val start = GeoPoint(25.0, 121.0)
        val end = GeoPoint(26.0, 122.0)
        val fraction = 1.0

        val result = LocationUtils.interpolate(start, end, fraction)
        assertEquals(26.0, result.latitude, 0.0001)
        assertEquals(122.0, result.longitude, 0.0001)
    }
}
