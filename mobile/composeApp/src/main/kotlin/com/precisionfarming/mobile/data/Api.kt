package com.precisionfarming.mobile.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Session {
    var accessToken: String? = null
}

val api = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
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
data class FarmDto(val id: String, val name: String, val location: String, val areaHa: Double? = null)

@Serializable
data class MachineDto(val id: String, val name: String, val status: String, val type: String)

@Serializable
data class OperationDto(val id: String, val type: String, val status: String)

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

suspend fun login(email: String, password: String): TokenResponse {
    Session.accessToken = null
    TokenStore.clear()
    val res: TokenResponse = api.post("/api/v1/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginRequest(email, password))
    }.body()
    Session.accessToken = res.accessToken
    TokenStore.save(res.accessToken)
    return res
}

suspend fun farms() = api.get("/api/v1/farms").body<List<FarmDto>>()
suspend fun machines() = api.get("/api/v1/machines").body<List<MachineDto>>()
suspend fun operations() = api.get("/api/v1/operations").body<List<OperationDto>>()
suspend fun alerts() = api.get("/api/v1/alerts").body<List<AlertDto>>()
suspend fun insights() = api.get("/api/v1/ai/insights").body<List<InsightDto>>()
suspend fun startOp(id: String) = api.post("/api/v1/operations/$id/start")
suspend fun completeOp(id: String) = api.post("/api/v1/operations/$id/complete")

suspend fun scouting() = api.get("/api/v1/scouting").body<List<ScoutingDto>>()
suspend fun soilSamples() = api.get("/api/v1/soil/samples").body<List<SoilSampleDto>>()
suspend fun recommendations() = api.get("/api/v1/recommendations").body<List<RecommendationDto>>()
suspend fun weatherWindows() = api.get("/api/v1/weather/windows").body<List<WeatherWindowDto>>()
suspend fun irrigationAssets() = api.get("/api/v1/irrigation/assets").body<List<IrrigationAssetDto>>()
suspend fun irrigationRecommendations() =
    api.get("/api/v1/irrigation/recommendations").body<List<IrrigationRecommendationDto>>()
suspend fun maintenanceWorkOrders() =
    api.get("/api/v1/maintenance/work-orders").body<List<MaintenanceWorkOrderDto>>()
suspend fun completeWorkOrder(id: String) =
    api.post("/api/v1/maintenance/work-orders/$id/complete")
