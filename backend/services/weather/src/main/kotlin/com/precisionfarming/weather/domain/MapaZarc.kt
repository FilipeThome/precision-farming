package com.precisionfarming.weather.domain

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.MonthDay
import java.time.YearMonth

fun interface MapaZarcLookup {
    fun window(municipality: String, crop: String): MonthDayWindow?
}

/**
 * Parses CKAN datastore JSON into a ZARC [MonthDayWindow].
 * Accepts records with cultura + mes (1–12) + decendio (1,2,3).
 * Decendio 1 → day 1–10, 2 → 11–20, 3 → 21–last day of month.
 * Window is the shortest arc covering matching decêndios. SOY matches "Soja".
 * Optional [municipality] keeps only records whose municipio (and UF when present) match.
 */
fun parseMapaZarcWindow(json: String, crop: String, municipality: String? = null): MonthDayWindow? {
    if (json.isBlank() || crop.isBlank()) return null
    return try {
        val root = ObjectMapper().readTree(json)
        if (root.has("success") && !root.path("success").asBoolean(true)) return null
        val records = when {
            root.path("result").path("records").isArray -> root.path("result").path("records")
            root.path("records").isArray -> root.path("records")
            else -> return null
        }
        if (!records.isArray || records.isEmpty) return null
        val needles = cropNeedles(crop)
        val ranges = mutableListOf<Pair<MonthDay, MonthDay>>()
        for (rec in records) {
            if (!municipality.isNullOrBlank()) {
                val mun = rec.path("municipio").asText("").ifBlank { rec.path("Municipio").asText("") }
                val uf = rec.path("UF").asText("").ifBlank { rec.path("uf").asText("") }
                if (!municipalityMatches(mun, uf, municipality)) continue
            }
            val cultura = rec.path("cultura").asText("").ifBlank { rec.path("Cultura").asText("") }
            val culturaKey = cultura.lowercase()
            if (needles.none { culturaKey.contains(it) }) continue
            val mes = rec.path("mes").takeIf { !it.isMissingNode && !it.isNull }?.asInt()
                ?: rec.path("Mes").takeIf { !it.isMissingNode && !it.isNull }?.asInt()
                ?: continue
            val decendio = rec.path("decendio").takeIf { !it.isMissingNode && !it.isNull }?.asInt()
                ?: rec.path("Decendio").takeIf { !it.isMissingNode && !it.isNull }?.asInt()
                ?: continue
            if (mes !in 1..12 || decendio !in 1..3) continue
            val startDay = when (decendio) {
                1 -> 1
                2 -> 11
                else -> 21
            }
            val lastDay = YearMonth.of(2001, mes).lengthOfMonth() // non-leap reference year
            val endDay = when (decendio) {
                1 -> 10
                2 -> 20
                else -> lastDay
            }.coerceAtMost(lastDay)
            ranges += MonthDay.of(mes, startDay) to MonthDay.of(mes, endDay)
        }
        tightestWindow(ranges)
    } catch (_: Exception) {
        null
    }
}

/** Fold accents and case for municipality comparison. */
internal fun foldKey(value: String): String =
    java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()

/**
 * Seed form is often "São Gabriel do Oeste - MS"; CKAN uses municipio + UF columns.
 */
internal fun municipalityMatches(recordMunicipio: String, recordUf: String, configured: String): Boolean {
    val cfg = foldKey(configured)
    if (cfg.isEmpty()) return false
    val city = cfg.substringBefore(" - ").trim()
    val cfgUf = cfg.substringAfter(" - ", "").trim()
    val mun = foldKey(recordMunicipio)
    if (mun.isEmpty() || city.isEmpty()) return false
    if (!(mun == city || mun.contains(city) || city.contains(mun))) return false
    val uf = foldKey(recordUf)
    if (cfgUf.isNotEmpty() && uf.isNotEmpty() && cfgUf != uf) return false
    return true
}

/** SOY matches the MAPA label "Soja"; same for the other demo crops. */
internal fun cropNeedles(crop: String): List<String> {
    val key = crop.trim().lowercase()
    if (key.isEmpty()) return emptyList()
    val aliases = when (key) {
        "soy", "soja" -> listOf("soy", "soja")
        "corn", "maize", "milho" -> listOf("corn", "maize", "milho")
        "wheat", "trigo" -> listOf("wheat", "trigo")
        "cotton", "algodao", "algodão" -> listOf("cotton", "algod")
        else -> emptyList()
    }
    return (listOf(key) + aliases).distinct()
}

/**
 * Smallest year-agnostic arc that covers every decêndio.
 * January plus December stays Dec–Jan, not Jan 1–Dec 31.
 */
internal fun tightestWindow(ranges: List<Pair<MonthDay, MonthDay>>): MonthDayWindow? {
    if (ranges.isEmpty()) return null
    val covered = BooleanArray(366)
    for ((start, end) in ranges) {
        val startDay = start.atYear(2001).dayOfYear
        val endDay = end.atYear(2001).dayOfYear
        if (startDay <= endDay) {
            for (day in startDay..endDay) covered[day] = true
        } else {
            for (day in startDay..365) covered[day] = true
            for (day in 1..endDay) covered[day] = true
        }
    }
    val days = (1..365).filter { covered[it] }
    if (days.isEmpty()) return null
    var bestGap = -1
    var gapEnd = days.first()
    val ring = days + (days.first() + 365)
    for (i in 0 until ring.lastIndex) {
        val gap = ring[i + 1] - ring[i]
        if (gap > bestGap) {
            bestGap = gap
            gapEnd = ring[i + 1]
        }
    }
    val startDoy = if (gapEnd > 365) gapEnd - 365 else gapEnd
    val endDoy = days[((days.indexOf(startDoy) - 1) + days.size) % days.size]
    return MonthDayWindow(
        MonthDay.from(LocalDate.ofYearDay(2001, startDoy)),
        MonthDay.from(LocalDate.ofYearDay(2001, endDoy)),
    )
}
