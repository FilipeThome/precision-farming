package com.precisionfarming.mobile.data.offline

import com.precisionfarming.mobile.data.SyncCommandDto
import com.precisionfarming.mobile.data.completeOp
import com.precisionfarming.mobile.data.operation
import com.precisionfarming.mobile.data.pauseOp
import com.precisionfarming.mobile.data.startOp
import com.precisionfarming.mobile.data.syncDeviceId
import com.precisionfarming.mobile.data.syncPush
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Outcome of one replay attempt; the queue maps it to [SyncState]. */
sealed class ExecResult {
    /** 2xx — command applied. */
    data object Ok : ExecResult()

    /** 4xx — the server rejected the command; never retried. */
    data class Rejected(val message: String) : ExecResult()

    /** IO / 5xx / transient — keep pending and retry with backoff. */
    data class Retry(val message: String) : ExecResult()
}

fun interface CommandExecutor {
    suspend fun execute(command: QueuedCommand): ExecResult
}

/**
 * Replays through the real operation endpoints (the backend `POST /sync/push` only stores commands).
 * After success it best-effort pushes the same `clientOperationId` to `/sync/push` as an audit trail.
 */
class ApiCommandExecutor(
    private val audit: Boolean = true,
) : CommandExecutor {
    override suspend fun execute(command: QueuedCommand): ExecResult {
        val result = try {
            when (command.type) {
                OpCommandType.START -> startOp(command.operationId, command.clientOperationId)
                OpCommandType.PAUSE -> pauseOp(command.operationId, command.reason.orEmpty(), command.clientOperationId)
                OpCommandType.COMPLETE -> completeOp(
                    command.operationId,
                    command.clientOperationId,
                    command.actualLiters,
                )
            }
            ExecResult.Ok
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            classifyClientError(command, e)
        } catch (e: ServerResponseException) {
            ExecResult.Retry(e.response.status.toString())
        } catch (e: Exception) {
            ExecResult.Retry(e.message ?: e::class.simpleName.orEmpty())
        }
        if (result is ExecResult.Ok && audit) {
            runCatching {
                syncPush(
                    deviceId = syncDeviceId(),
                    commands = listOf(
                        SyncCommandDto(
                            clientOperationId = command.clientOperationId,
                            type = "OPERATION_${command.type.name}",
                            createdAt = command.createdAt,
                            payload = auditPayload(command),
                        ),
                    ),
                )
            }
        }
        return result
    }

    private suspend fun classifyClientError(command: QueuedCommand, e: ClientRequestException): ExecResult {
        val status = e.response.status
        val detail = apiErrorCode(e.response) ?: status.toString()
        // Auth/rate-limit/timeout are not domain rejections — keep the command for a later replay.
        val transient = status == HttpStatusCode.Unauthorized ||
            status == HttpStatusCode.RequestTimeout ||
            status == HttpStatusCode.TooManyRequests
        if (transient) return ExecResult.Retry(detail)
        if (status == HttpStatusCode.Conflict) {
            val current = runCatching { operation(command.operationId) }.getOrElse {
                return ExecResult.Retry(it.message ?: detail)
            }
            return when {
                conflictAlreadyApplied(command.type, current.status) -> ExecResult.Ok
                inFlightSagaStatus(current.status) -> ExecResult.Retry(detail)
                else -> ExecResult.Rejected(detail)
            }
        }
        return ExecResult.Rejected(detail)
    }
}

@Serializable
private data class ApiErrorCode(val code: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

/** `code` from a JSON error body (`ApiError`). Null when the body is missing or not JSON. */
internal suspend fun apiErrorCode(response: HttpResponse): String? {
    val type = response.contentType() ?: return null
    if (!type.match(ContentType.Application.Json)) return null
    val text = runCatching { response.bodyAsText() }.getOrNull()?.trim().orEmpty()
    if (!text.startsWith("{")) return null
    return runCatching { errorJson.decodeFromString(ApiErrorCode.serializer(), text).code }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}

/** Audit trail map for `/sync/push` after a successful replay. */
fun auditPayload(command: QueuedCommand): Map<String, String> = buildMap {
    put("operationId", command.operationId)
    command.reason?.let { put("reason", it) }
    command.actualLiters?.let { put("actualLiters", it.toString()) }
}

/**
 * 409 after a timeout: success only when the server already reached the command's terminal state.
 * STARTING / COMPLETING are in-flight sagas ([inFlightSagaStatus]), not an applied command.
 * PAUSED is not a successful START.
 */
fun conflictAlreadyApplied(type: OpCommandType, serverStatus: String): Boolean {
    val status = serverStatus.uppercase()
    return when (type) {
        OpCommandType.START -> status == "IN_PROGRESS" || status == "COMPLETED"
        OpCommandType.PAUSE -> status == "PAUSED" || status == "COMPLETED"
        OpCommandType.COMPLETE -> status == "COMPLETED"
    }
}

/** Saga still moving. The queued command must stay pending and retry. */
fun inFlightSagaStatus(serverStatus: String): Boolean {
    val status = serverStatus.uppercase()
    return status == "STARTING" || status == "COMPLETING"
}
