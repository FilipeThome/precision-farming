package com.precisionfarming.weather

import com.precisionfarming.weather.domain.parseMapaZarcWindow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.MonthDay

class MapaZarcParserTest {
    @Test
    fun parsesDecendiosIntoWindow() {
        val json = """
            {
              "success": true,
              "result": {
                "records": [
                  {"cultura": "Soja precoce", "mes": 10, "decendio": 1},
                  {"cultura": "Soja irrigada", "mes": 12, "decendio": 2},
                  {"cultura": "Milho", "mes": 9, "decendio": 1}
                ]
              }
            }
        """.trimIndent()
        val window = parseMapaZarcWindow(json, "soja")
        assertEquals(MonthDay.of(10, 1), window!!.start)
        assertEquals(MonthDay.of(12, 20), window.end)
    }

    @Test
    fun hardCapsDecendio3ToLastDayOfMonth() {
        val json = """
            {
              "success": true,
              "result": {
                "records": [
                  {"cultura": "SOY cultivar", "mes": 2, "decendio": 3}
                ]
              }
            }
        """.trimIndent()
        val window = parseMapaZarcWindow(json, "soy")
        assertEquals(MonthDay.of(2, 21), window!!.start)
        assertEquals(MonthDay.of(2, 28), window.end)
    }

    @Test
    fun soyMatchesPortugueseSoja() {
        val json = """
            {"success":true,"result":{"records":[
              {"cultura":"Soja","mes":10,"decendio":1}
            ]}}
        """.trimIndent()
        val window = parseMapaZarcWindow(json, "SOY")
        assertEquals(MonthDay.of(10, 1), window!!.start)
        assertEquals(MonthDay.of(10, 10), window.end)
    }

    @Test
    fun decemberAndJanuaryStayAShortArc() {
        val json = """
            {"success":true,"result":{"records":[
              {"cultura":"Soja","mes":12,"decendio":3},
              {"cultura":"Soja","mes":1,"decendio":1}
            ]}}
        """.trimIndent()
        val window = parseMapaZarcWindow(json, "SOY")
        assertEquals(MonthDay.of(12, 21), window!!.start)
        assertEquals(MonthDay.of(1, 10), window.end)
    }

    @Test
    fun municipalityFilterDropsOtherCities() {
        val json = """
            {"success":true,"result":{"records":[
              {"cultura":"Soja","mes":10,"decendio":1,"municipio":"Campo Grande","UF":"MS"},
              {"cultura":"Soja","mes":11,"decendio":2,"municipio":"São Gabriel do Oeste","UF":"MS"}
            ]}}
        """.trimIndent()
        val window = parseMapaZarcWindow(json, "SOY", "São Gabriel do Oeste - MS")
        assertEquals(MonthDay.of(11, 11), window!!.start)
        assertEquals(MonthDay.of(11, 20), window.end)
    }

    @Test
    fun municipalityFilterMismatchReturnsNull() {
        val json = """
            {"success":true,"result":{"records":[
              {"cultura":"Soja","mes":10,"decendio":1,"municipio":"Campo Grande","UF":"MS"}
            ]}}
        """.trimIndent()
        assertNull(parseMapaZarcWindow(json, "SOY", "São Gabriel do Oeste - MS"))
    }

    @Test
    fun unparseableOrEmptyReturnsNull() {
        assertNull(parseMapaZarcWindow("", "soy"))
        assertNull(parseMapaZarcWindow("""{"success":false}""", "soy"))
        assertNull(parseMapaZarcWindow("""{"success":true,"result":{"records":[]}}""", "soy"))
        assertNull(parseMapaZarcWindow("""not-json""", "soy"))
    }
}
