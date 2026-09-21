package com.precisionfarming.mobile

import com.precisionfarming.mobile.i18n.AppLocale
import com.precisionfarming.mobile.i18n.En
import com.precisionfarming.mobile.i18n.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
        assertTrue(Pt.map.containsKey("inspector.close"))
        assertTrue(Pt.map.containsKey("inspector.notFound"))
        assertTrue(Pt.map.containsKey("inspector.source.live"))
        assertTrue(Pt.map.containsKey("form.none"))
        assertTrue(Pt.map.containsKey("form.photoInvalid"))
        assertTrue(Pt.map.containsKey("harvest.kpi.yieldVsPlan"))
        assertTrue(Pt.map.containsKey("harvest.kpi.expected"))
        assertTrue(Pt.map.containsKey("harvest.kpi.actual"))
        assertTrue(Pt.map.containsKey("scouting.title"))
        assertFalse(Pt.map.containsKey("more.scouting"))
        assertFalse(Pt.map.containsKey("more.prescriptions"))
        // Terra redesign: no prefilled pause reason, Today copy, queue/strip keys in both maps.
        assertFalse(Pt.map.containsKey("ops.pauseReasonDefault"))
        assertEquals("Hoje", Pt.map["nav.home"])
        assertEquals("Today", En.map["nav.home"])
        listOf("today.start", "run.pendingSync", "pause.required", "sync.state.FAILED", "strip.pending", "freshness.updated")
            .forEach { key ->
                assertTrue(key, Pt.map.containsKey(key))
                assertTrue(key, En.map.containsKey(key))
            }
        assertEquals("pt-BR", AppLocale.PT_BR.tag)
        assertEquals("en-US", AppLocale.EN_US.tag)
        val empty = Pt.map.filter { it.value.isBlank() }.keys + En.map.filter { it.value.isBlank() }.keys
        assertEquals(emptySet<String>(), empty)
    }

    @Test
    fun everyLiteralStCallExistsInBothMaps() {
        val root = mainSourceRoot()
        val call = Regex("""S\.t\("([^"]+)"""")
        val keys = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> call.findAll(file.readText()).map { it.groupValues[1] } }
            .filterNot { it.contains('$') }
            .toSortedSet()
        assertNotEquals("expected S.t(\"key\") calls under $root", 0, keys.size)
        val missingPt = keys.filterNot { Pt.map.containsKey(it) }
        val missingEn = keys.filterNot { En.map.containsKey(it) }
        assertEquals(emptyList<String>(), missingPt)
        assertEquals(emptyList<String>(), missingEn)
        listOf(
            "alerts.filter.CRITICAL",
            "alerts.filter.WARNING",
            "alerts.filter.INFO",
            "sync.cmd.START",
            "sync.cmd.PAUSE",
            "sync.cmd.COMPLETE",
            "sync.state.PENDING",
            "sync.state.SYNCING",
            "sync.state.SYNCED",
            "sync.state.FAILED",
            "nav.home",
            "nav.map",
            "nav.ops",
            "nav.alerts",
            "nav.more",
        ).forEach { key ->
            assertTrue(key, Pt.map.containsKey(key))
            assertTrue(key, En.map.containsKey(key))
        }
    }

    private fun mainSourceRoot(): File {
        val marker = "com/precisionfarming/mobile"
        val candidates = listOf(
            File("src/main/kotlin"),
            File("composeApp/src/main/kotlin"),
            File("mobile/composeApp/src/main/kotlin"),
            File("../src/main/kotlin"),
        )
        return candidates.firstOrNull { File(it, marker).isDirectory }
            ?: error("Could not find main source root. cwd=${File(".").canonicalPath}")
    }
}
