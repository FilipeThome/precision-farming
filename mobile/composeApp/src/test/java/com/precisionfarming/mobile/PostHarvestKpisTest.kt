package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.ChainStepState
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.FinancePnlDto
import com.precisionfarming.mobile.data.FlowStageId
import com.precisionfarming.mobile.data.HarvestPlanDto
import com.precisionfarming.mobile.data.LogisticsLoadDto
import com.precisionfarming.mobile.data.StorageLotDto
import com.precisionfarming.mobile.data.StorageUnitDto
import com.precisionfarming.mobile.data.YieldRecordDto
import com.precisionfarming.mobile.data.expectedTons
import com.precisionfarming.mobile.data.flowStages
import com.precisionfarming.mobile.data.harvestedTons
import com.precisionfarming.mobile.data.loadsSummary
import com.precisionfarming.mobile.data.marginRows
import com.precisionfarming.mobile.data.qualityBreakdown
import com.precisionfarming.mobile.data.storageOccupancyTotal
import com.precisionfarming.mobile.data.unitOccupancyPct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostHarvestKpisTest {
    private val field = FieldDto(id = "f1", farmId = "farm", name = "A", areaHa = 10.0, crop = "SOY")

    @Test
    fun expectedTonsOnlyWithArea() {
        val out = expectedTons(
            listOf(
                HarvestPlanDto(id = "p1", fieldId = "f1", expectedTHa = 3.5),
                HarvestPlanDto(id = "p2", fieldId = "missing", expectedTHa = 4.0),
                HarvestPlanDto(id = "p3", fieldId = "f1"),
            ),
            listOf(field),
        )
        assertEquals(35.0, out.tons, 0.0)
        assertEquals(1, out.withArea)
        assertEquals(3, out.total)
    }

    @Test
    fun harvestedTonsFromRecordsWithArea() {
        val out = harvestedTons(
            listOf(
                YieldRecordDto(id = "y1", yieldTHa = 3.0, areaHa = 5.0),
                YieldRecordDto(id = "y2", yieldTHa = 2.0),
            ),
        )
        assertEquals(15.0, out.tons, 0.0)
        assertEquals(1, out.withArea)
        assertEquals(2, out.total)
    }

    @Test
    fun loadsSummaryByStatus() {
        val out = loadsSummary(
            listOf(
                LogisticsLoadDto(id = "l1", status = "QUEUED", tons = 10.0),
                LogisticsLoadDto(id = "l2", status = "DISPATCHED", tons = 20.0),
                LogisticsLoadDto(id = "l3", status = "DELIVERED", tons = 30.0),
            ),
        )
        assertEquals(1, out.queued)
        assertEquals(1, out.inTransit)
        assertEquals(1, out.delivered)
        assertEquals(20.0, out.tonsInTransit, 0.0)
    }

    @Test
    fun storageOccupancy() {
        assertEquals(38, storageOccupancyTotal(
            listOf(
                StorageUnitDto(id = "s1", usedT = 50.0, capacityT = 100.0),
                StorageUnitDto(id = "s2", usedT = 25.0, capacityT = 100.0),
            ),
        ).pct)
        assertNull(storageOccupancyTotal(emptyList()).pct)
        assertEquals(100, unitOccupancyPct(StorageUnitDto(id = "s", usedT = 120.0, capacityT = 100.0)))
        assertNull(unitOccupancyPct(StorageUnitDto(id = "s", usedT = 1.0)))
    }

    @Test
    fun qualityBreakdownSortedByTons() {
        val rows = qualityBreakdown(
            listOf(
                StorageLotDto(id = "a", unitId = "u", farmId = "f", crop = "SOY", tons = 10.0, quality = "A"),
                StorageLotDto(id = "b", unitId = "u", farmId = "f", crop = "SOY", tons = 30.0, quality = "B"),
                StorageLotDto(id = "c", unitId = "u", farmId = "f", crop = "SOY", tons = 5.0, quality = "A"),
            ),
        )
        assertEquals("B", rows[0].quality)
        assertEquals(30.0, rows[0].tons, 0.0)
        assertEquals("A", rows[1].quality)
        assertEquals(2, rows[1].count)
        assertEquals(15.0, rows[1].tons, 0.0)
    }

    @Test
    fun flowStagesDerivation() {
        val stages = flowStages(
            listOf(HarvestPlanDto(id = "p1", status = "IN_PROGRESS")),
            listOf(LogisticsLoadDto(id = "l1", status = "IN_TRANSIT", tons = 1.0)),
            listOf(StorageLotDto(id = "a", unitId = "u", farmId = "f", crop = "SOY", tons = 10.0, quality = "A")),
        )
        assertEquals(
            listOf("harvest:NOW", "transport:NOW", "storage:DONE", "quality:DONE"),
            stages.map { "${it.id.name}:${it.state.name}" },
        )
        assertTrue(flowStages(emptyList(), emptyList(), emptyList()).all { it.state == ChainStepState.PENDING })
        assertEquals(FlowStageId.harvest, stages[0].id)
    }

    @Test
    fun marginRowsFilterSortLimit() {
        val rows = marginRows(
            listOf(
                FinancePnlDto(id = "1", fieldId = "f1", margin = 10.0, currency = "BRL"),
                FinancePnlDto(id = "2", fieldId = "f1", margin = 5.0, currency = "BRL"),
                FinancePnlDto(id = "3", fieldId = "f2", margin = 20.0, currency = "BRL"),
                FinancePnlDto(id = "4", fieldId = null, margin = 99.0),
                FinancePnlDto(id = "5", fieldId = "f3", margin = null),
            ),
            limit = 8,
        )
        assertEquals(listOf("f2", "f1", "f1"), rows.map { it.fieldId })
        assertEquals(20.0, rows[0].margin, 0.0)
        assertEquals(10.0, rows[1].margin, 0.0)
        assertEquals(5.0, rows[2].margin, 0.0)
    }
}
