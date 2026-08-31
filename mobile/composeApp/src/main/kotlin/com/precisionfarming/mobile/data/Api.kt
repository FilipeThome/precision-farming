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

suspend fun login(email: String, password: String): TokenResponse {
    val res: TokenResponse = api.post("/api/v1/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginRequest(email, password))
    }.body()
    Session.accessToken = res.accessToken
    return res
}

suspend fun farms() = api.get("/api/v1/farms").body<List<FarmDto>>()
suspend fun machines() = api.get("/api/v1/machines").body<List<MachineDto>>()
suspend fun operations() = api.get("/api/v1/operations").body<List<OperationDto>>()
suspend fun alerts() = api.get("/api/v1/alerts").body<List<AlertDto>>()
suspend fun insights() = api.get("/api/v1/ai/insights").body<List<InsightDto>>()
suspend fun startOp(id: String) = api.post("/api/v1/operations/$id/start")
suspend fun completeOp(id: String) = api.post("/api/v1/operations/$id/complete")
