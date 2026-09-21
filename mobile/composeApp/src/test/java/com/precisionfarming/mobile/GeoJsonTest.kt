package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.centroidOfGeometry
import com.precisionfarming.mobile.data.geometryForFieldSave
import com.precisionfarming.mobile.data.squarePolygonGeoJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoJsonTest {
    @Test
    fun buildsClosedPolygon() {
        val json = squarePolygonGeoJson(-22.9, -49.9, 100.0)
        assertTrue(json.contains("\"type\":\"Polygon\""))
        assertTrue(json.startsWith("{"))
        val c = centroidOfGeometry(json)
        assertEquals(-22.9, c!!.first, 0.01)
        assertEquals(-49.9, c.second, 0.01)
    }

    @Test
    fun fieldSaveKeepsExistingGeometryWhenLocationUnchanged() {
        val existing = """{"type":"MultiPolygon","coordinates":[[[[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,0.0]]]]}"""
        val next = geometryForFieldSave(
            existingGeometry = existing,
            originalLat = "-19.39",
            originalLon = "-54.57",
            originalAreaHa = "120.5",
            lat = "-19.39",
            lon = "-54.57",
            areaHa = "120.5",
        )
        assertEquals(existing, next)
    }

    @Test
    fun fieldSaveRebuildsSquareWhenLocationChanges() {
        val existing = """{"type":"MultiPolygon","coordinates":[[[[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,0.0]]]]}"""
        val next = geometryForFieldSave(
            existingGeometry = existing,
            originalLat = "-19.39",
            originalLon = "-54.57",
            originalAreaHa = "120.5",
            lat = "-22.9",
            lon = "-49.9",
            areaHa = "120.5",
        )
        assertNotEquals(existing, next)
        assertTrue(next.contains("\"type\":\"Polygon\""))
    }
}
