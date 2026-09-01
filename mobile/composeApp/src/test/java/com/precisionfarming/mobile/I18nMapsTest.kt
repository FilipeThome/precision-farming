package com.precisionfarming.mobile

import com.precisionfarming.mobile.i18n.AppLocale
import com.precisionfarming.mobile.i18n.En
import com.precisionfarming.mobile.i18n.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class I18nMapsTest {
    @Test
    fun ptAndEnShareKeys() {
        assertEquals(Pt.map.keys, En.map.keys)
        assertTrue(Pt.map.containsKey("nav.more"))
        assertEquals("pt-BR", AppLocale.PT_BR.tag)
        assertEquals("en-US", AppLocale.EN_US.tag)
    }
}
