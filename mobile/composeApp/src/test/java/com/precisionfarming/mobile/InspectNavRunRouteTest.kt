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
        assertTrue(InspectNav.tabSelected(InspectNav.TOWER, InspectNav.MORE))
        assertTrue(InspectNav.tabSelected(InspectNav.pattern(InspectNav.DECISIONS), InspectNav.MORE))
        assertTrue(InspectNav.tabSelected(InspectNav.HARVEST, InspectNav.MORE))
        assertTrue(InspectNav.tabSelected(InspectNav.pattern(InspectNav.HARVEST_DETAIL), InspectNav.MORE))
        assertFalse(InspectNav.tabSelected("home", InspectNav.MORE))
        assertTrue(InspectNav.tabSelected("home", InspectNav.HOME))
        assertEquals("mais/sync", InspectNav.SYNC)
    }

    @Test
    fun towerDecisionsHarvestRoutes() {
        assertEquals("mais/tower", InspectNav.TOWER)
        assertEquals("mais/decisions", InspectNav.DECISIONS)
        assertEquals("mais/harvest", InspectNav.HARVEST)
        assertEquals("mais/harvest/detail", InspectNav.HARVEST_DETAIL)
        assertEquals("mais/decisions?selected=x", InspectNav.href(InspectNav.DECISIONS, selected = "x"))
        assertEquals("mais/harvest/detail?selected=", InspectNav.href(InspectNav.HARVEST_DETAIL))
        assertEquals("mais/decisions?selected={selected}", InspectNav.pattern(InspectNav.DECISIONS))
    }

    @Test
    fun hrefEncodesCompositeDecisionIds() {
        val href = InspectNav.href(InspectNav.DECISIONS, selected = "PRESCRIPTION:p1")
        assertEquals("mais/decisions?selected=PRESCRIPTION%3Ap1", href)
        assertEquals("PRESCRIPTION:p1", InspectNav.decodeArg("PRESCRIPTION%3Ap1"))
        assertEquals("PRESCRIPTION:p1", InspectNav.decodeArg("PRESCRIPTION:p1"))
        assertEquals(null, InspectNav.decodeArg(""))
        assertEquals(null, InspectNav.decodeArg(null))
    }

    @Test
    fun hrefAndPatternStayStable() {
        assertEquals("ops?selected=", InspectNav.href(InspectNav.OPS))
        assertEquals("ops?selected=x", InspectNav.href(InspectNav.OPS, selected = "x"))
        assertEquals("alertas?severity=CRITICAL&selected=", InspectNav.href(InspectNav.ALERTS, severity = "CRITICAL"))
        assertEquals("ops", InspectNav.baseOf("ops?selected=x"))
    }
}
