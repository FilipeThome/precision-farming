package com.precisionfarming.mobile.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val role: String,
    val userId: String,
    val name: String,
    val email: String,
)

@Serializable
data class MeDto(val id: String, val name: String, val email: String, val role: String)

@Serializable
data class FarmDto(
    val id: String,
    val name: String,
    val location: String,
    val areaHa: Double? = null,
    val timezone: String? = null,
)

@Serializable
data class FieldDto(
    val id: String,
    val farmId: String? = null,
    val name: String? = null,
    val areaHa: Double? = null,
    val crop: String? = null,
    val variety: String? = null,
    val geometry: String? = null,
)

@Serializable
data class SeasonDto(
    val id: String,
    val farmId: String? = null,
    val name: String? = null,
    val crop: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val year: Int? = null,
)

@Serializable
data class MachineDto(
    val id: String,
    val name: String,
    val status: String,
    val type: String,
    val farmId: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val photoFileId: String? = null,
    val photoUrl: String? = null,
)

@Serializable
data class OperationDto(
    val id: String,
    val type: String,
    val status: String,
    val fieldId: String? = null,
    val farmId: String? = null,
    val machineId: String? = null,
    val plannedStart: String? = null,
    val plannedEnd: String? = null,
    val actualStart: String? = null,
    val actualEnd: String? = null,
    val pauseReason: String? = null,
    val itemId: String? = null,
    val itemQuantity: Double? = null,
    val areaHa: Double? = null,
    val prescriptionId: String? = null,
    val actualLiters: Double? = null,
)

@Serializable
data class PauseRequest(val reason: String)

@Serializable
data class CompleteOpRequest(val actualLiters: Double)

/** JSON body for POST /operations/{id}/complete when liters were captured. */
fun completeOpBody(actualLiters: Double?): CompleteOpRequest? =
    actualLiters?.let { CompleteOpRequest(it) }

@Serializable
data class AlertDto(
    val id: String,
    val title: String,
    val message: String? = null,
    val severity: String,
    val status: String,
    val farmId: String? = null,
    val type: String? = null,
    val entityType: String? = null,
    val entityId: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class InsightDto(
    val id: String,
    val type: String,
    val score: Double,
    val model: String,
    val demo: Boolean? = null,
    val confidence: Double? = null,
    val explanation: List<String> = emptyList(),
    val entityId: String? = null,
    val horizonHours: Int? = null,
    val modelVersion: String? = null,
    val generatedAt: String? = null,
)

@Serializable
data class MachineMetricsDayDto(
    val day: String,
    val hours: Double = 0.0,
    val speed: Double = 0.0,
    val fuel: Double = 0.0,
)

@Serializable
data class MachineMetricsDto(
    val engineHours: Double = 0.0,
    val lastObservedAt: String? = null,
    val days: List<MachineMetricsDayDto> = emptyList(),
)

@Serializable
data class MachineWorkDayDto(val day: String, val areaHa: Double = 0.0)

@Serializable
data class MachineWorkInputDto(val itemId: String, val quantity: Double = 0.0)

@Serializable
data class MachineWorkSummaryDto(
    val areaHa: Double = 0.0,
    val days: List<MachineWorkDayDto> = emptyList(),
    val inputs: List<MachineWorkInputDto> = emptyList(),
)

@Serializable
data class InventoryMovementDto(
    val id: String,
    val itemId: String? = null,
    val type: String? = null,
    val quantity: Double? = null,
    val occurredAt: String? = null,
    val reference: String? = null,
)

@Serializable
data class ScoutingDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val observedAt: String? = null,
    val pest: String? = null,
    val severity: String? = null,
    val notes: String? = null,
    val status: String? = null,
)

@Serializable
data class SoilSampleDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val sampledAt: String? = null,
    val ph: Double? = null,
    val organicMatterPct: Double? = null,
    val pPpm: Double? = null,
    val kPpm: Double? = null,
    val labRef: String? = null,
    val status: String? = null,
)

@Serializable
data class RecommendationDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val kind: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class WeatherWindowDto(
    val id: String,
    val farmId: String? = null,
    val windowType: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val rating: String? = null,
    val notes: String? = null,
)

@Serializable
data class WeatherForecastDto(
    val id: String,
    val farmId: String? = null,
    val forecastAt: String? = null,
    val temperatureMin: Double? = null,
    val temperatureMax: Double? = null,
    val rainMm: Double? = null,
    val rainProbability: Double? = null,
    val windKmh: Double? = null,
    val humidityPct: Double? = null,
    val sprayingWindow: String? = null,
    val vintage: String? = null,
)

@Serializable
data class IrrigationAssetDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val name: String? = null,
    val type: String? = null,
    val status: String? = null,
    val capacityMmH: Double? = null,
)

@Serializable
data class IrrigationRecommendationDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val assetId: String? = null,
    val recommendedMm: Double? = null,
    val reason: String? = null,
    val status: String? = null,
    val windowStart: String? = null,
    val windowEnd: String? = null,
)

@Serializable
data class MaintenanceWorkOrderDto(
    val id: String,
    val farmId: String? = null,
    val machineId: String? = null,
    val title: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val completedAt: String? = null,
)

@Serializable
data class PrescriptionDto(
    val id: String,
    val farmId: String = "",
    val fieldId: String,
    val product: String = "",
    /** Legacy dose field; prefer [plannedDose] when present. */
    val rate: Double = 0.0,
    val plannedDose: Double? = null,
    val unit: String = "",
    val status: String = "",
    /** SPOT or BROADCAST — never fabricate when absent. */
    val mode: String? = null,
    val treatedFraction: Double? = null,
    val moaGroup: String? = null,
    val createdAt: String? = null,
    val approvedAt: String? = null,
) {
    val dose: Double get() = plannedDose ?: rate
}

@Serializable
data class MapLayerDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val name: String? = null,
    val kind: String? = null,
    val source: String? = null,
    val tileUrl: String? = null,
    val acquiredAt: String? = null,
    val status: String? = null,
)

@Serializable
data class InventoryItemDto(
    val id: String,
    val farmId: String? = null,
    val name: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val quantity: Double? = null,
    val reserved: Double? = null,
)

@Serializable
data class HarvestPlanDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val crop: String? = null,
    val status: String? = null,
    val plannedStart: String? = null,
    val plannedEnd: String? = null,
    val expectedTHa: Double? = null,
)

@Serializable
data class YieldRecordDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val planId: String? = null,
    val recordedAt: String? = null,
    val yieldTHa: Double? = null,
    val moisturePct: Double? = null,
    val areaHa: Double? = null,
)

@Serializable
data class LogisticsLoadDto(
    val id: String,
    val farmId: String? = null,
    val planId: String? = null,
    val truckPlate: String? = null,
    val destination: String? = null,
    val tons: Double? = null,
    val status: String? = null,
    val dispatchedAt: String? = null,
)

@Serializable
data class DispatchRequest(val loadId: String)

@Serializable
data class StorageUnitDto(
    val id: String,
    val farmId: String? = null,
    val name: String? = null,
    val type: String? = null,
    val capacityT: Double? = null,
    val usedT: Double? = null,
)

@Serializable
data class StorageLotDto(
    val id: String,
    val unitId: String? = null,
    val farmId: String? = null,
    val crop: String? = null,
    val tons: Double? = null,
    val quality: String? = null,
    val receivedAt: String? = null,
)

@Serializable
data class FinanceCostDto(
    val id: String,
    val farmId: String? = null,
    val category: String? = null,
    val description: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val occurredAt: String? = null,
    val fieldId: String? = null,
)

@Serializable
data class FinancePnlDto(
    val id: String? = null,
    val farmId: String? = null,
    val fieldId: String? = null,
    val revenue: Double? = null,
    val cost: Double? = null,
    val margin: Double? = null,
    val currency: String? = null,
    val period: String? = null,
)

@Serializable
data class FinanceBudgetDto(
    val id: String,
    val farmId: String? = null,
    val category: String? = null,
    val seasonLabel: String? = null,
    val planned: Double? = null,
    val actual: Double? = null,
    val currency: String? = null,
)

@Serializable
data class FinanceCashflowDto(
    val id: String,
    val farmId: String? = null,
    val label: String? = null,
    val direction: String? = null,
    val amount: Double? = null,
    val dueAt: String? = null,
)

@Serializable
data class MarketQuoteDto(
    val id: String,
    val commodity: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val unit: String? = null,
    val quotedAt: String? = null,
    val market: String? = null,
    val exchange: String? = null,
)

@Serializable
data class MarketContractDto(
    val id: String,
    val farmId: String? = null,
    val commodity: String? = null,
    val volumeTons: Double? = null,
    val volumeT: Double? = null,
    val price: Double? = null,
    val currency: String? = null,
    val status: String? = null,
    val counterparty: String? = null,
    val deliveryAt: String? = null,
)

@Serializable
data class MarketExposureDto(
    val id: String,
    val farmId: String? = null,
    val commodity: String? = null,
    val openT: Double? = null,
    val hedgedT: Double? = null,
    val riskScore: Double? = null,
    val netTons: Double? = null,
    val markToMarket: Double? = null,
    val currency: String? = null,
    val asOf: String? = null,
)

@Serializable
data class TraceabilityLotDto(
    val id: String,
    val farmId: String? = null,
    val fieldId: String? = null,
    val lotCode: String? = null,
    val crop: String? = null,
    val eventType: String? = null,
    val summary: String? = null,
    val occurredAt: String? = null,
)

@Serializable
data class EsgMetricDto(
    val id: String,
    val farmId: String? = null,
    val metric: String? = null,
    val value: Double? = null,
    val unit: String? = null,
    val score: Double? = null,
    val periodLabel: String? = null,
)

@Serializable
data class IntegrationDto(
    val name: String,
    val type: String? = null,
    val mode: String? = null,
    val capabilities: List<String> = emptyList(),
)

@Serializable
data class SyncPullRequest(val deviceId: String, val cursor: String? = null)

@Serializable
data class SyncPullResponse(
    val cursor: String? = null,
    val deviceId: String? = null,
)

@Serializable
data class SyncCommandDto(
    val clientOperationId: String,
    val type: String,
    val createdAt: String,
    val payload: Map<String, String> = emptyMap(),
)

@Serializable
data class SyncPushRequest(val deviceId: String, val commands: List<SyncCommandDto>)

@Serializable
data class FarmUpsert(val name: String, val location: String, val areaHa: Double, val timezone: String)

@Serializable
data class FieldUpsert(
    val farmId: String,
    val name: String,
    val areaHa: Double,
    val crop: String,
    val variety: String?,
    val geometry: String,
)

@Serializable
data class SeasonUpsert(
    val farmId: String,
    val name: String,
    val crop: String,
    val startDate: String,
    val endDate: String?,
    val status: String,
)

@Serializable
data class MachineUpsert(
    val farmId: String,
    val name: String,
    val type: String,
    val manufacturer: String,
    val model: String,
    val status: String,
    val photoFileId: String? = null,
)

@Serializable
data class InventoryCreate(
    val farmId: String,
    val name: String,
    val category: String,
    val unit: String,
    val quantity: Double,
)

@Serializable
data class InventoryPatch(val farmId: String, val name: String, val category: String, val unit: String)

@Serializable
data class IrrigationAssetUpsert(
    val farmId: String,
    val fieldId: String?,
    val name: String,
    val type: String,
    val status: String,
    val capacityMmH: Double?,
)

@Serializable
data class HarvestPlanCreate(val farmId: String, val fieldId: String, val crop: String, val expectedTHa: Double)

@Serializable
data class StorageUnitUpsert(
    val farmId: String,
    val name: String,
    val type: String,
    val capacityT: Double,
    val usedT: Double,
)

@Serializable
data class WorkOrderCreate(val farmId: String, val machineId: String, val title: String, val priority: String)

@Serializable
data class FileMetaDto(val id: String, val farmId: String? = null, val kind: String? = null)
