package com.precisionfarming.mobile

import com.precisionfarming.mobile.i18n.AppLocale
import com.precisionfarming.mobile.i18n.En
import com.precisionfarming.mobile.i18n.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class I18nMapsTest {
    @Test
    fun ptAndEnShareKeys() {
        assertEquals(Pt.map.keys, En.map.keys)
        assertTrue(Pt.map.containsKey("nav.more"))
        assertTrue(Pt.map.containsKey("more.sync"))
        assertTrue(Pt.map.containsKey("prescriptions.title"))
        assertTrue(Pt.map.containsKey("sync.title"))
        assertTrue(Pt.map.containsKey("dashboard.title"))
        assertTrue(Pt.map.containsKey("settings.logout"))
        assertTrue(Pt.map.containsKey("more.group.land"))
        assertTrue(Pt.map.containsKey("scouting.title"))
        assertFalse(Pt.map.containsKey("more.scouting"))
        assertFalse(Pt.map.containsKey("more.prescriptions"))
        assertEquals("pt-BR", AppLocale.PT_BR.tag)
        assertEquals("en-US", AppLocale.EN_US.tag)
    }
}
