package com.precisionfarming.mobile.data

import com.precisionfarming.mobile.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

fun HttpRequestBuilder.farmQuery(farmId: String?) {
    if (!farmId.isNullOrBlank()) parameter("farmId", farmId)
}

private fun HttpRequestBuilder.idempotency(clientOperationId: String?) {
    if (!clientOperationId.isNullOrBlank()) header("Idempotency-Key", clientOperationId)
}

private val AuthRetried = AttributeKey<Boolean>("pf.AuthRetried")
private val refreshMutex = Mutex()

val api = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    defaultRequest {
        url(BuildConfig.API_BASE_URL)
        Session.accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
}.also { client ->
    client.plugin(HttpSend).intercept { request ->
        val failedAuth = request.headers[HttpHeaders.Authorization]
        val first = try {
            execute(request)
        } catch (e: ClientRequestException) {
            if (!shouldRefresh(request, e.response.status)) throw e
            if (!refreshSession(failedAuth)) throw e
            return@intercept execute(retried(request))
        }
        if (first.response.status == HttpStatusCode.Unauthorized && shouldRefresh(request, first.response.status)) {
            if (!refreshSession(failedAuth)) return@intercept first
            return@intercept execute(retried(request))
        }
        first
    }
}

private fun shouldRefresh(request: HttpRequestBuilder, status: HttpStatusCode): Boolean {
    if (status != HttpStatusCode.Unauthorized) return false
    if (request.attributes.contains(AuthRetried)) return false
    val path = request.url.build().encodedPath
    return !path.endsWith("/auth/login") && !path.endsWith("/auth/refresh")
}

/** Copy the call and mark it so a second 401 cannot refresh again. */
private fun retried(request: HttpRequestBuilder): HttpRequestBuilder =
    HttpRequestBuilder().apply {
        takeFrom(request)
        attributes.put(AuthRetried, true)
        headers.remove(HttpHeaders.Authorization)
        val token = Session.accessToken
        if (!token.isNullOrBlank()) header(HttpHeaders.Authorization, "Bearer $token")
    }

suspend fun login(email: String, password: String): TokenResponse {
    Session.clear()
    TokenStore.clear()
    val res: TokenResponse = api.post("/api/v1/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginRequest(email, password))
    }.body()
    Session.set(res.accessToken, res.userId, res.role)
    TokenStore.save(res.accessToken, res.userId, res.role, res.refreshToken)
    return res
}

/**
 * Exchanges the stored refresh token once. Does not delete it before the call.
 * [failedAuthorization] is the bearer that just got a 401; if another caller already
 * rotated the access token, this returns without a second refresh.
 */
suspend fun refreshSession(failedAuthorization: String? = null): Boolean = refreshMutex.withLock {
    val current = Session.accessToken?.let { "Bearer $it" }
    if (!failedAuthorization.isNullOrBlank() && !current.isNullOrBlank() && current != failedAuthorization) {
        return@withLock true
    }
    val refresh = TokenStore.readRefresh()
    if (refresh.isNullOrBlank()) {
        TokenStore.clear()
        Session.clear()
        return@withLock false
    }
    val ok = refreshWithToken(refresh)
    if (!ok) {
        TokenStore.clear()
        Session.clear()
    }
    ok
}

private suspend fun refreshWithToken(refresh: String): Boolean =
    runCatching {
        val res: TokenResponse = api.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refresh))
        }.body()
        Session.set(res.accessToken, res.userId, res.role)
        TokenStore.save(res.accessToken, res.userId, res.role, res.refreshToken)
        true
    }.getOrDefault(false)

suspend fun me() = api.get("/api/v1/auth/me").body<MeDto>()

suspend fun farms() =
    api.get("/api/v1/farms").body<List<FarmDto>>().also(EntityNames::registerFarms)

suspend fun fields(farmId: String? = null) =
    api.get("/api/v1/fields") { farmQuery(farmId) }.body<List<FieldDto>>().also(EntityNames::registerFields)

suspend fun seasons(farmId: String? = null) =
    api.get("/api/v1/seasons") { farmQuery(farmId) }.body<List<SeasonDto>>()

suspend fun machines(farmId: String? = null) =
    api.get("/api/v1/machines") { farmQuery(farmId) }.body<List<MachineDto>>().also(EntityNames::registerMachines)

suspend fun operations(farmId: String? = null) =
    api.get("/api/v1/operations") { farmQuery(farmId) }.body<List<OperationDto>>()

suspend fun operation(id: String) = api.get("/api/v1/operations/$id").body<OperationDto>()

suspend fun alerts(farmId: String? = null) =
    api.get("/api/v1/alerts") { farmQuery(farmId) }.body<List<AlertDto>>()

suspend fun insights(farmId: String? = null) =
    api.get("/api/v1/ai/insights") { farmQuery(farmId) }.body<List<InsightDto>>()

suspend fun startOp(id: String, clientOperationId: String? = null) = api.post("/api/v1/operations/$id/start") {
    idempotency(clientOperationId)
}

suspend fun pauseOp(id: String, reason: String, clientOperationId: String? = null) = api.post("/api/v1/operations/$id/pause") {
    contentType(ContentType.Application.Json)
    setBody(PauseRequest(reason))
    idempotency(clientOperationId)
}

suspend fun completeOp(
    id: String,
    clientOperationId: String? = null,
    actualLiters: Double? = null,
) = api.post("/api/v1/operations/$id/complete") {
    completeOpBody(actualLiters)?.let { body ->
        contentType(ContentType.Application.Json)
        setBody(body)
    }
    idempotency(clientOperationId)
}

suspend fun ackAlert(id: String) = api.post("/api/v1/alerts/$id/ack")

suspend fun scouting(farmId: String? = null) =
    api.get("/api/v1/scouting") { farmQuery(farmId) }.body<List<ScoutingDto>>()

suspend fun soilSamples(farmId: String? = null) =
    api.get("/api/v1/soil/samples") { farmQuery(farmId) }.body<List<SoilSampleDto>>()

suspend fun recommendations(farmId: String? = null) =
    api.get("/api/v1/recommendations") { farmQuery(farmId) }.body<List<RecommendationDto>>()

suspend fun prescriptions(farmId: String? = null) =
    api.get("/api/v1/prescriptions") { farmQuery(farmId) }.body<List<PrescriptionDto>>()

suspend fun prescription(id: String) =
    api.get("/api/v1/prescriptions/$id").body<PrescriptionDto>()

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

suspend fun inventoryMovements(itemId: String) =
    api.get("/api/v1/inventory/$itemId/movements").body<List<InventoryMovementDto>>()

suspend fun machineMetrics(machineId: String) =
    api.get("/api/v1/machines/$machineId/metrics").body<MachineMetricsDto>()

suspend fun machineWorkSummary(machineId: String, from: String, to: String) =
    api.get("/api/v1/operations/machine-summary") {
        parameter("machineId", machineId)
        parameter("from", from)
        parameter("to", to)
    }.body<MachineWorkSummaryDto>()

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

suspend fun reportOperationsPdf(farmId: String? = null): ByteArray =
    api.get("/api/v1/reports/operations.pdf") { farmQuery(farmId) }.bodyAsBytes()

suspend fun reportInventoryPdf(farmId: String? = null): ByteArray =
    api.get("/api/v1/reports/inventory.pdf") { farmQuery(farmId) }.bodyAsBytes()

suspend fun syncPull(deviceId: String, cursor: String? = null): SyncPullResponse =
    api.post("/api/v1/sync/pull") {
        contentType(ContentType.Application.Json)
        setBody(SyncPullRequest(deviceId = deviceId, cursor = cursor))
    }.body()

/** Audit-only: the backend stores the commands; state changes go through the operation endpoints. */
suspend fun syncPush(deviceId: String, commands: List<SyncCommandDto>) =
    api.post("/api/v1/sync/push") {
        contentType(ContentType.Application.Json)
        setBody(SyncPushRequest(deviceId = deviceId, commands = commands))
    }

/** Device binding for sync: one logical device per signed-in user on this platform. */
fun syncDeviceId(): String {
    val userId = Session.userId
    if (userId.isNullOrBlank()) error("Missing userId for sync device binding — sign in again")
    return "$userId:android"
}

private suspend inline fun <reified T, reified B : Any> postJson(path: String, body: B): T =
    api.post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }.body()

private suspend inline fun <reified T, reified B : Any> patchJson(path: String, body: B): T =
    api.patch(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }.body()

suspend fun createFarm(body: FarmUpsert): FarmDto =
    createFarm(
        userId = Session.userId,
        body = body,
        store = PendingFarmCreates,
        postFarm = { postJson("/api/v1/farms", it) },
        consumeRefresh = { TokenStore.clearRefresh() },
        refreshHttp = { refreshWithToken(it) },
    )
suspend fun patchFarm(id: String, body: FarmUpsert): FarmDto = patchJson("/api/v1/farms/$id", body)
suspend fun createField(body: FieldUpsert): FieldDto = postJson("/api/v1/fields", body)
suspend fun patchField(id: String, body: FieldUpsert): FieldDto = patchJson("/api/v1/fields/$id", body)
suspend fun createSeason(body: SeasonUpsert): SeasonDto = postJson("/api/v1/seasons", body)
suspend fun patchSeason(id: String, body: SeasonUpsert): SeasonDto = patchJson("/api/v1/seasons/$id", body)
suspend fun createMachine(body: MachineUpsert): MachineDto = postJson("/api/v1/machines", body)
suspend fun patchMachine(id: String, body: MachineUpsert): MachineDto = patchJson("/api/v1/machines/$id", body)
suspend fun createInventoryItem(body: InventoryCreate): InventoryItemDto = postJson("/api/v1/inventory", body)
suspend fun patchInventoryItem(id: String, body: InventoryPatch): InventoryItemDto = patchJson("/api/v1/inventory/$id", body)
suspend fun createIrrigationAsset(body: IrrigationAssetUpsert): IrrigationAssetDto =
    postJson("/api/v1/irrigation/assets", body)
suspend fun patchIrrigationAsset(id: String, body: IrrigationAssetUpsert): IrrigationAssetDto =
    patchJson("/api/v1/irrigation/assets/$id", body)
suspend fun createHarvestPlan(body: HarvestPlanCreate): HarvestPlanDto = postJson("/api/v1/harvest/plans", body)
suspend fun createStorageUnit(body: StorageUnitUpsert): StorageUnitDto = postJson("/api/v1/storage/units", body)
suspend fun patchStorageUnit(id: String, body: StorageUnitUpsert): StorageUnitDto =
    patchJson("/api/v1/storage/units/$id", body)
suspend fun createWorkOrder(body: WorkOrderCreate): MaintenanceWorkOrderDto =
    postJson("/api/v1/maintenance/work-orders", body)

suspend fun fileContent(id: String): ByteArray =
    api.get("/api/v1/files/$id/content").bodyAsBytes()

suspend fun uploadMachinePhoto(farmId: String, entityId: String?, bytes: ByteArray, mimeType: String): FileMetaDto =
    api.post("/api/v1/files") {
        setBody(
            MultiPartFormDataContent(
                formData {
                    append("farmId", farmId)
                    append("kind", "MACHINE_PHOTO")
                    if (!entityId.isNullOrBlank()) append("entityId", entityId)
                    append(
                        "file",
                        bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=machine.jpg")
                        },
                    )
                },
            ),
        )
    }.body()

suspend fun saveMachineWithPhoto(id: String?, body: MachineUpsert, photoBytes: ByteArray?, mimeType: String?): MachineDto {
    var photoFileId = body.photoFileId
    if (photoBytes != null && mimeType != null) {
        val meta = uploadMachinePhoto(body.farmId, id, photoBytes, mimeType)
        photoFileId = meta.id
    }
    val next = body.copy(photoFileId = photoFileId)
    return if (id == null) createMachine(next) else patchMachine(id, next)
}
