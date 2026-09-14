package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.InspectNav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectNavRunRouteTest {
    @Test
    fun opsRunRouteMatchesPattern() {
        assertEquals("ops/run/{id}", InspectNav.OPS_RUN)
        assertEquals("ops/run/abc-123", InspectNav.opsRun("abc-123"))
        assertEquals("ops/run/a%2Fb", InspectNav.opsRun("a/b"))
    }

    @Test
    fun ordersTabHighlightsForRunRoutes() {
        assertTrue(InspectNav.tabSelected("ops?selected={selected}", InspectNav.OPS))
        assertTrue(InspectNav.tabSelected("ops/run/{id}", InspectNav.OPS))
        assertTrue(InspectNav.tabSelected("ops/run/f1", InspectNav.OPS))
        assertTrue(InspectNav.tabSelected(InspectNav.opsRun("f1"), InspectNav.OPS))
        assertTrue(InspectNav.tabSelected(InspectNav.opsRun("abc-123"), InspectNav.OPS))
        assertFalse(InspectNav.tabSelected("ops/run/{id}", InspectNav.HOME))
        assertFalse(InspectNav.tabSelected("ops/run/f1", InspectNav.HOME))
        assertFalse(InspectNav.tabSelected("ops/run/{id}", InspectNav.MORE))
        assertFalse(InspectNav.tabSelected(null, InspectNav.OPS))
        assertFalse(InspectNav.tabSelected("", InspectNav.OPS))
    }

    @Test
    fun moreTabCoversStackedRoutes() {
        assertTrue(InspectNav.tabSelected(InspectNav.SYNC, InspectNav.MORE))
        assertTrue(InspectNav.tabSelected("mais/farms?selected={selected}", InspectNav.MORE))
        assertFalse(InspectNav.tabSelected("home", InspectNav.MORE))
        assertTrue(InspectNav.tabSelected("home", InspectNav.HOME))
        assertEquals("mais/sync", InspectNav.SYNC)
    }

    @Test
    fun hrefAndPatternStayStable() {
        assertEquals("ops?selected=", InspectNav.href(InspectNav.OPS))
        assertEquals("ops?selected=x", InspectNav.href(InspectNav.OPS, selected = "x"))
        assertEquals("alertas?severity=CRITICAL&selected=", InspectNav.href(InspectNav.ALERTS, severity = "CRITICAL"))
        assertEquals("ops", InspectNav.baseOf("ops?selected=x"))
    }
}
