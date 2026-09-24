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
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

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
        // Auth/rate-limit/timeout are not domain rejections — keep the command for a later replay.
        val transient = status == HttpStatusCode.Unauthorized ||
            status == HttpStatusCode.RequestTimeout ||
            status == HttpStatusCode.TooManyRequests
        if (transient) return ExecResult.Retry(status.toString())
        if (status == HttpStatusCode.Conflict) {
            val current = runCatching { operation(command.operationId) }.getOrElse {
                return ExecResult.Retry(it.message ?: status.toString())
            }
            return if (conflictAlreadyApplied(command.type, current.status)) ExecResult.Ok
            else ExecResult.Rejected(status.toString())
        }
        return ExecResult.Rejected(status.toString())
    }
}

/** Audit trail map for `/sync/push` after a successful replay. */
fun auditPayload(command: QueuedCommand): Map<String, String> = buildMap {
    put("operationId", command.operationId)
    command.reason?.let { put("reason", it) }
    command.actualLiters?.let { put("actualLiters", it.toString()) }
}

/** 409 after a timeout: treat as success when the server already moved into the command's state. */
fun conflictAlreadyApplied(type: OpCommandType, serverStatus: String): Boolean {
    val status = serverStatus.uppercase()
    return when (type) {
        // PAUSED is the resume-failure revert, not a successful START.
        OpCommandType.START -> status in setOf("IN_PROGRESS", "STARTING", "COMPLETED", "COMPLETING")
        OpCommandType.PAUSE -> status in setOf("PAUSED", "COMPLETED", "COMPLETING")
        OpCommandType.COMPLETE -> status in setOf("COMPLETED", "COMPLETING")
    }
}
