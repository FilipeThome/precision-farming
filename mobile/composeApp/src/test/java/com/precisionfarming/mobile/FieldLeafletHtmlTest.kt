package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.fieldLeafletHtml
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldLeafletHtmlTest {
    @Test
    fun htmlContainsLeafletAndKnownCoordinates() {
        val html = fieldLeafletHtml(
            listOf("Talhão Norte" to (-19.3912 to -54.5728)),
        )
        assertTrue(html.contains("leaflet", ignoreCase = true))
        assertTrue(html.contains("-19.3912"))
        assertTrue(html.contains("-54.5728"))
    }

    @Test
    fun fieldNameIsHtmlEscapedBeforeThePopup() {
        val html = fieldLeafletHtml(listOf("<img src=x onerror=alert(1)>" to (1.0 to 2.0)))
        assertFalse(html.contains("<img"))
        assertTrue(html.contains("\\u0026lt;img"))
    }

    @Test
    fun fieldNameEscapesJsLineSeparators() {
        val html = fieldLeafletHtml(
            listOf("break\u2028out\u2029here" to (1.0 to 2.0)),
        )
        assertFalse(html.contains("\u2028"))
        assertFalse(html.contains("\u2029"))
        assertTrue(html.contains("\\u2028"))
        assertTrue(html.contains("\\u2029"))
    }
}
