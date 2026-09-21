package com.precisionfarming.mobile.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun squarePolygonGeoJson(lat: Double, lon: Double, areaHa: Double): String {
    val areaM2 = maxOf(areaHa, 0.0001) * 10_000
    val halfM = kotlin.math.sqrt(areaM2) / 2
    val dLat = halfM / 111_320
    val cos = kotlin.math.cos(lat * Math.PI / 180)
    val dLon = halfM / (111_320 * maxOf(kotlin.math.abs(cos), 0.01))
    val ring = listOf(
        listOf(lon - dLon, lat - dLat),
        listOf(lon + dLon, lat - dLat),
        listOf(lon + dLon, lat + dLat),
        listOf(lon - dLon, lat + dLat),
        listOf(lon - dLon, lat - dLat),
    )
    val coords = ring.joinToString(",") { "[${it[0]},${it[1]}]" }
    return """{"type":"Polygon","coordinates":[[$coords]]}"""
}

fun centroidOfGeometry(geometry: String?): Pair<Double, Double>? {
    if (geometry.isNullOrBlank()) return null
    return runCatching {
        val parsed = Json.parseToJsonElement(geometry).jsonObject
        val type = parsed["type"]?.jsonPrimitive?.content
        val coords = parsed["coordinates"]?.jsonArray ?: return null
        val ring = when (type) {
            "Polygon" -> coords.first().jsonArray
            "MultiPolygon" -> coords.first().jsonArray.first().jsonArray
            else -> return null
        }
        val n = (ring.size - 1).coerceAtLeast(1)
        var lon = 0.0
        var lat = 0.0
        for (i in 0 until n) {
            val point = ring[i].jsonArray
            lon += point[0].jsonPrimitive.double
            lat += point[1].jsonPrimitive.double
        }
        lat / n to lon / n
    }.getOrNull()
}

fun geometryForFieldSave(
    existingGeometry: String?,
    originalLat: String,
    originalLon: String,
    originalAreaHa: String,
    lat: String,
    lon: String,
    areaHa: String,
): String {
    if (
        !existingGeometry.isNullOrBlank() &&
        lat == originalLat &&
        lon == originalLon &&
        areaHa == originalAreaHa
    ) {
        return existingGeometry
    }
    return squarePolygonGeoJson(
        lat.toDoubleOrNull() ?: -22.9,
        lon.toDoubleOrNull() ?: -49.9,
        areaHa.toDoubleOrNull() ?: 1.0,
    )
}
