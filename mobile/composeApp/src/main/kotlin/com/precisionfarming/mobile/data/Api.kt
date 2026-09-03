package com.precisionfarming.mobile.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Session {
    var accessToken: String? = null
        private set
    var userId: String? = null
        private set

    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    fun set(accessToken: String?, userId: String?) {
        this.accessToken = accessToken
        this.userId = userId
        _signedIn.value = !accessToken.isNullOrBlank() && !userId.isNullOrBlank()
    }

    fun clear() {
        accessToken = null
        userId = null
        _signedIn.value = false
    }
}

fun HttpRequestBuilder.farmQuery(farmId: String?) {
    if (!farmId.isNullOrBlank()) parameter("farmId", farmId)
}

val api = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    HttpResponseValidator {
        validateResponse { response ->
            if (response.status == HttpStatusCode.Unauthorized) {
                TokenStore.clear()
                Session.clear()
            }
        }
    }
    defaultRequest {
        url("http://10.0.2.2:8080")
        Session.accessToken?.let { header("Authorization", "Bearer $it") }
    }
}

@Serializable
data class LoginRequest(val email: String, val password: String)

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
data class FarmDto(val id: String, val name: String, val location: String, val areaHa: Double? = null)

@Serializable
data class FieldDto(
    val id: String,
    val farmId: String? = null,
    val name: String? = null,
    val areaHa: Double? = null,
    val crop: String? = null,
    val variety: String? = null,
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
data class MachineDto(val id: String, val name: String, val status: String, val type: String)

@Serializable
data class OperationDto(val id: String, val type: String, val status: String)

@Serializable
data class PauseRequest(val reason: String)

@Serializable
data class AlertDto(val id: String, val title: String, val severity: String, val status: String)

@Serializable
data class InsightDto(val id: String, val type: String, val score: Double, val model: String, val demo: Boolean = true)

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
    val farmId: String,
    val fieldId: String,
    val product: String,
    val rate: Double,
    val unit: String,
    val status: String,
    val createdAt: String? = null,
    val approvedAt: String? = null,
)

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

suspend fun login(email: String, password: String): TokenResponse {
    Session.clear()
    TokenStore.clear()
    val res: TokenResponse = api.post("/api/v1/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginRequest(email, password))
    }.body()
    Session.set(res.accessToken, res.userId)
    TokenStore.save(res.accessToken, res.userId)
    return res
}

suspend fun me() = api.get("/api/v1/auth/me").body<MeDto>()

suspend fun farms() = api.get("/api/v1/farms").body<List<FarmDto>>()

suspend fun fields(farmId: String? = null) =
    api.get("/api/v1/fields") { farmQuery(farmId) }.body<List<FieldDto>>()

suspend fun seasons(farmId: String? = null) =
    api.get("/api/v1/seasons") { farmQuery(farmId) }.body<List<SeasonDto>>()

suspend fun machines(farmId: String? = null) =
    api.get("/api/v1/machines") { farmQuery(farmId) }.body<List<MachineDto>>()

suspend fun operations(farmId: String? = null) =
    api.get("/api/v1/operations") { farmQuery(farmId) }.body<List<OperationDto>>()

suspend fun alerts(farmId: String? = null) =
    api.get("/api/v1/alerts") { farmQuery(farmId) }.body<List<AlertDto>>()

suspend fun insights(farmId: String? = null) =
    api.get("/api/v1/ai/insights") { farmQuery(farmId) }.body<List<InsightDto>>()

suspend fun startOp(id: String) = api.post("/api/v1/operations/$id/start")

suspend fun pauseOp(id: String, reason: String) = api.post("/api/v1/operations/$id/pause") {
    contentType(ContentType.Application.Json)
    setBody(PauseRequest(reason))
}

suspend fun completeOp(id: String) = api.post("/api/v1/operations/$id/complete")

suspend fun ackAlert(id: String) = api.post("/api/v1/alerts/$id/ack")

suspend fun scouting(farmId: String? = null) =
    api.get("/api/v1/scouting") { farmQuery(farmId) }.body<List<ScoutingDto>>()

suspend fun soilSamples(farmId: String? = null) =
    api.get("/api/v1/soil/samples") { farmQuery(farmId) }.body<List<SoilSampleDto>>()

suspend fun recommendations(farmId: String? = null) =
    api.get("/api/v1/recommendations") { farmQuery(farmId) }.body<List<RecommendationDto>>()

suspend fun prescriptions(farmId: String? = null) =
    api.get("/api/v1/prescriptions") { farmQuery(farmId) }.body<List<PrescriptionDto>>()

suspend fun approvePrescription(id: String) = api.post("/api/v1/prescriptions/$id/approve")

suspend fun weatherWindows(farmId: String? = null) =
    api.get("/api/v1/weather/windows") { farmQuery(farmId) }.body<List<WeatherWindowDto>>()

suspend fun forecast(farmId: String? = null) =
    api.get("/api/v1/weather/forecast") { farmQuery(farmId) }.body<List<WeatherForecastDto>>()

suspend fun irrigationAssets(farmId: String? = null) =
    api.get("/api/v1/irrigation/assets") { farmQuery(farmId) }.body<List<IrrigationAssetDto>>()

suspend fun irrigationRecommendations(farmId: String? = null) =
    api.get("/api/v1/irrigation/recommendations") { farmQuery(farmId) }
        .body<List<IrrigationRecommendationDto>>()

suspend fun maintenanceWorkOrders(farmId: String? = null) =
    api.get("/api/v1/maintenance/work-orders") { farmQuery(farmId) }
        .body<List<MaintenanceWorkOrderDto>>()

suspend fun completeWorkOrder(id: String) =
    api.post("/api/v1/maintenance/work-orders/$id/complete")

suspend fun mapLayers(farmId: String? = null) =
    api.get("/api/v1/map/layers") { farmQuery(farmId) }.body<List<MapLayerDto>>()

suspend fun inventory(farmId: String? = null) =
    api.get("/api/v1/inventory") { farmQuery(farmId) }.body<List<InventoryItemDto>>()

suspend fun harvestPlans(farmId: String? = null) =
    api.get("/api/v1/harvest/plans") { farmQuery(farmId) }.body<List<HarvestPlanDto>>()

suspend fun harvestYield(farmId: String? = null) =
    api.get("/api/v1/harvest/yield") { farmQuery(farmId) }.body<List<YieldRecordDto>>()

suspend fun logisticsLoads(farmId: String? = null) =
    api.get("/api/v1/logistics/loads") { farmQuery(farmId) }.body<List<LogisticsLoadDto>>()

suspend fun dispatchLoad(loadId: String) = api.post("/api/v1/logistics/dispatch") {
    contentType(ContentType.Application.Json)
    setBody(DispatchRequest(loadId))
}

suspend fun storageUnits(farmId: String? = null) =
    api.get("/api/v1/storage/units") { farmQuery(farmId) }.body<List<StorageUnitDto>>()

suspend fun storageLots(farmId: String? = null) =
    api.get("/api/v1/storage/lots") { farmQuery(farmId) }.body<List<StorageLotDto>>()

suspend fun financeCosts(farmId: String? = null) =
    api.get("/api/v1/finance/costs") { farmQuery(farmId) }.body<List<FinanceCostDto>>()

suspend fun financePnl(farmId: String? = null) =
    api.get("/api/v1/finance/pnl") { farmQuery(farmId) }.body<List<FinancePnlDto>>()

suspend fun financeBudget(farmId: String? = null) =
    api.get("/api/v1/finance/budget") { farmQuery(farmId) }.body<List<FinanceBudgetDto>>()

suspend fun financeCashflow(farmId: String? = null) =
    api.get("/api/v1/finance/cashflow") { farmQuery(farmId) }.body<List<FinanceCashflowDto>>()

suspend fun marketQuotes(farmId: String? = null) =
    api.get("/api/v1/market/quotes") { farmQuery(farmId) }.body<List<MarketQuoteDto>>()

suspend fun marketContracts(farmId: String? = null) =
    api.get("/api/v1/market/contracts") { farmQuery(farmId) }.body<List<MarketContractDto>>()

suspend fun marketExposure(farmId: String? = null) =
    api.get("/api/v1/market/exposure") { farmQuery(farmId) }.body<List<MarketExposureDto>>()

suspend fun traceability(farmId: String? = null) =
    api.get("/api/v1/traceability") { farmQuery(farmId) }.body<List<TraceabilityLotDto>>()

suspend fun esg(farmId: String? = null) =
    api.get("/api/v1/esg") { farmQuery(farmId) }.body<List<EsgMetricDto>>()

suspend fun integrations() = api.get("/api/v1/integrations").body<List<IntegrationDto>>()

suspend fun reportOperationsCsv(): ByteArray = api.get("/api/v1/reports/operations.csv").bodyAsBytes()

suspend fun reportInventoryCsv(): ByteArray = api.get("/api/v1/reports/inventory.csv").bodyAsBytes()

/** Soft-callable sync pull stub. Prefer [Session.userId]:demo when bound. */
suspend fun syncPull(deviceId: String, cursor: String? = null): SyncPullResponse =
    api.post("/api/v1/sync/pull") {
        contentType(ContentType.Application.Json)
        setBody(SyncPullRequest(deviceId = deviceId, cursor = cursor))
    }.body()

fun syncDeviceId(): String {
    val userId = Session.userId
    if (userId.isNullOrBlank()) error("Missing userId for sync device binding — sign in again")
    return "$userId:demo"
}
