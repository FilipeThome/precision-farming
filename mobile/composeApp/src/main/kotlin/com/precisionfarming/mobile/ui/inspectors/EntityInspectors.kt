package com.precisionfarming.mobile.ui.inspectors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.HarvestPlanDto
import com.precisionfarming.mobile.data.InsightDto
import com.precisionfarming.mobile.data.InventoryItemDto
import com.precisionfarming.mobile.data.InventoryMovementDto
import com.precisionfarming.mobile.data.OperationDto
import com.precisionfarming.mobile.data.PrescriptionDto
import com.precisionfarming.mobile.data.StorageLotDto
import com.precisionfarming.mobile.data.YieldMath
import com.precisionfarming.mobile.data.YieldRecordDto
import com.precisionfarming.mobile.data.alerts
import com.precisionfarming.mobile.data.fields
import com.precisionfarming.mobile.data.financePnl
import com.precisionfarming.mobile.data.formatNumber
import com.precisionfarming.mobile.data.formatWhen
import com.precisionfarming.mobile.data.harvestYield
import com.precisionfarming.mobile.data.inventoryMovements
import com.precisionfarming.mobile.data.operations
import com.precisionfarming.mobile.data.prescriptions
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.S
import com.precisionfarming.mobile.ui.components.InspectorKpiItem
import com.precisionfarming.mobile.ui.components.InspectorKpis
import kotlin.math.round

@Composable
fun FarmInspector(farm: FarmDto, modifier: Modifier = Modifier) {
    var fieldCount by remember { mutableStateOf(0) }
    var opCount by remember { mutableStateOf(0) }
    var alertCount by remember { mutableStateOf(0) }
    var margin by remember { mutableStateOf(0.0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(farm.id) {
        loading = true
        error = null
        val result = runCatching {
            val farmFields = fields(farm.id)
            val ops = operations(farm.id)
            val farmAlerts = alerts(farm.id)
            val pnl = financePnl(farm.id)
            Triple(farmFields.size to ops.size, farmAlerts.size, pnl.sumOf { it.margin ?: 0.0 })
        }
        result.onSuccess { (counts, alertsSize, pnlMargin) ->
            fieldCount = counts.first
            opCount = counts.second
            alertCount = alertsSize
            margin = pnlMargin
        }.onFailure { error = it.message ?: S.t("common.error") }
        loading = false
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(listOfNotNull(farm.location, farm.timezone).joinToString(" · "))
        when {
            loading -> Text(S.t("common.loading"))
            error != null -> Text("${S.t("common.error")}: $error")
            else -> InspectorKpis(
                listOf(
                    InspectorKpiItem(S.t("farms.kpi.area"), farm.areaHa?.let { "${formatNumber(it)} ha" } ?: "—"),
                    InspectorKpiItem(S.t("farms.kpi.fields"), fieldCount.toString()),
                    InspectorKpiItem(S.t("farms.kpi.ops"), opCount.toString()),
                    InspectorKpiItem(S.t("farms.kpi.alerts"), alertCount.toString()),
                    InspectorKpiItem(S.t("farms.kpi.pnl"), formatNumber(margin)),
                ),
            )
        }
    }
}

@Composable
fun FieldInspector(field: FieldDto, modifier: Modifier = Modifier) {
    var ops by remember { mutableStateOf<List<OperationDto>>(emptyList()) }
    var yields by remember { mutableStateOf<List<YieldRecordDto>>(emptyList()) }
    var rx by remember { mutableStateOf<List<PrescriptionDto>>(emptyList()) }
    var yieldReady by remember { mutableStateOf(false) }
    var rxReady by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(field.id, field.farmId) {
        loading = true
        error = null
        yieldReady = false
        rxReady = false
        val farmId = field.farmId
        runCatching { operations(farmId).filter { it.fieldId == field.id } }
            .onSuccess { ops = it }
            .onFailure { error = it.message ?: S.t("common.error") }
        runCatching { harvestYield(farmId) }
            .onSuccess { yields = it; yieldReady = true }
        runCatching { prescriptions(farmId).filter { it.fieldId == field.id } }
            .onSuccess { rx = it; rxReady = true }
        loading = false
    }
    val actual = YieldMath.weightedYieldTHa(YieldMath.yieldsForField(yields, field.id))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(listOfNotNull(DomainLabels.label(field.crop), field.variety).joinToString(" · "))
        when {
            loading -> Text(S.t("common.loading"))
            error != null -> Text("${S.t("common.error")}: $error")
            else -> {
                InspectorKpis(
                    listOf(
                        InspectorKpiItem(S.t("fields.kpi.crop"), DomainLabels.label(field.crop)),
                        InspectorKpiItem(S.t("fields.kpi.area"), field.areaHa?.let { "${formatNumber(it)} ha" } ?: "—"),
                        InspectorKpiItem(S.t("fields.kpi.ops"), ops.size.toString()),
                        InspectorKpiItem(
                            S.t("fields.kpi.yield"),
                            if (yieldReady) "${formatNumber(actual)} t/ha" else "—",
                        ),
                        InspectorKpiItem(
                            S.t("fields.kpi.prescriptions"),
                            if (rxReady) rx.size.toString() else "—",
                        ),
                    ),
                )
                ops.takeLast(5).reversed().forEach { op ->
                    Text("${DomainLabels.label(op.type)} · ${DomainLabels.label(op.status)}")
                }
            }
        }
    }
}

@Composable
fun InventoryInspector(item: InventoryItemDto, modifier: Modifier = Modifier) {
    var movements by remember { mutableStateOf<List<InventoryMovementDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(item.id) {
        loading = true
        val result = runCatching { inventoryMovements(item.id) }
        movements = result.getOrDefault(emptyList())
        error = result.exceptionOrNull()?.message
        loading = false
    }
    val qty = item.quantity ?: 0.0
    val reserved = item.reserved ?: 0.0
    val available = qty - reserved
    val unit = item.unit.orEmpty()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(DomainLabels.label(item.category))
        InspectorKpis(
            listOf(
                InspectorKpiItem(S.t("inventory.kpi.stock"), "${formatNumber(qty)} $unit"),
                InspectorKpiItem(S.t("inventory.kpi.reserved"), "${formatNumber(reserved)} $unit"),
                InspectorKpiItem(S.t("inventory.kpi.available"), "${formatNumber(available)} $unit"),
            ),
        )
        if (loading) Text(S.t("common.loading"))
        error?.let { Text("${S.t("common.error")}: $it") }
        val consumed = movements.filter { it.type.equals("CONSUME", ignoreCase = true) }
        if (consumed.isNotEmpty()) {
            Text(S.t("inventory.charts.consumption"))
            consumed.takeLast(8).forEach { row ->
                Text("${formatWhen(row.occurredAt)} · ${formatNumber(row.quantity)} $unit")
            }
        }
    }
}

@Composable
fun HarvestPlanInspector(plan: HarvestPlanDto, yields: List<YieldRecordDto>, modifier: Modifier = Modifier) {
    val actual = YieldMath.weightedYieldTHa(YieldMath.yieldsForPlan(yields, plan))
    val expected = plan.expectedTHa ?: 0.0
    val ratio = if (expected == 0.0) 0 else round(actual / expected * 100).toInt()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(DomainLabels.label(plan.status))
        Text(formatWhen(plan.plannedStart))
        InspectorKpis(
            listOf(
                InspectorKpiItem(S.t("harvest.kpi.expected"), "${formatNumber(expected)} t/ha"),
                InspectorKpiItem(S.t("harvest.kpi.actual"), "${formatNumber(actual)} t/ha"),
                InspectorKpiItem(S.t("harvest.kpi.yieldVsPlan"), "$ratio%"),
            ),
        )
    }
}

@Composable
fun StorageLotInspector(lot: StorageLotDto, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InspectorKpis(
            listOf(
                InspectorKpiItem(S.t("fields.kpi.crop"), DomainLabels.label(lot.crop)),
                InspectorKpiItem(S.t("harvest.kpi.actual"), lot.tons?.let { "${formatNumber(it)} t" } ?: "—"),
                InspectorKpiItem(S.t("inventory.kpi.stock"), DomainLabels.label(lot.quality)),
                InspectorKpiItem(S.t("harvest.received"), formatWhen(lot.receivedAt)),
            ),
        )
    }
}

@Composable
fun InsightInspector(insight: InsightDto, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${insight.model} ${insight.modelVersion.orEmpty()}".trim())
        Text(formatWhen(insight.generatedAt))
        InspectorKpis(
            listOf(
                InspectorKpiItem(
                    S.t("ai.kpi.confidence"),
                    insight.confidence?.let { formatNumber(it * 100, 0) + "%" } ?: "—",
                ),
                InspectorKpiItem(
                    S.t("charts.risk"),
                    formatNumber(insight.score * 100, 0) + "%",
                ),
            ),
        )
        if (insight.explanation.isNotEmpty()) {
            Text(S.t("ai.factors"))
            insight.explanation.forEach { Text(DomainLabels.label(it)) }
        }
    }
}
