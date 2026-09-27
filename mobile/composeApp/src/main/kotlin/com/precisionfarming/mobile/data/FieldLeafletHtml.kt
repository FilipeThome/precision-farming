package com.precisionfarming.mobile.data

/**
 * Builds a self-contained Leaflet + Esri World Imagery HTML page for field markers.
 *
 * Interim WebView until the Google Maps SDK (ADR-002).
 */
fun fieldLeafletHtml(points: List<Pair<String, Pair<Double, Double>>>): String {
    val markersJs = points.joinToString(",\n") { (name, latLon) ->
        val (lat, lon) = latLon
        """{name:'${escapeJs(escapeHtml(name))}',lat:$lat,lon:$lon}"""
    }
    val center = points.firstOrNull()?.second ?: (-16.5 to -54.5)
    val zoom = if (points.isEmpty()) 5 else 12
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  html,body,#map{margin:0;padding:0;height:100%;width:100%;}
</style>
</head>
<body>
<div id="map"></div>
<script>
var map = L.map('map').setView([${center.first}, ${center.second}], $zoom);
L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
  attribution: 'Tiles \u00a9 Esri \u2014 Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community',
  maxZoom: 19
}).addTo(map);
var points = [
$markersJs
];
var group = L.featureGroup();
points.forEach(function(p) {
  L.marker([p.lat, p.lon]).bindPopup(p.name).addTo(group);
});
group.addTo(map);
if (points.length > 1) {
  map.fitBounds(group.getBounds().pad(0.2), { maxZoom: 15 });
}
</script>
</body>
</html>
""".trimIndent()
}

private fun escapeHtml(value: String): String = buildString(value.length) {
    for (ch in value) {
        when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(ch)
        }
    }
}

private fun escapeJs(value: String): String =
    buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\u2028' -> append("\\u2028")
                '\u2029' -> append("\\u2029")
                '<' -> append("\\u003c")
                '>' -> append("\\u003e")
                '&' -> append("\\u0026")
                else -> append(ch)
            }
        }
    }
