package com.precisionfarming.mobile.data.offline

import kotlinx.serialization.Serializable

/** Command kinds the queue can replay against the existing operation endpoints. */
@Serializable
enum class OpCommandType { START, PAUSE, COMPLETE }

/** Intent captured at the UI edge; the queue turns it into a persisted [QueuedCommand]. */
sealed class OpCommand {
    abstract val operationId: String
    abstract val type: OpCommandType
    open val reason: String? get() = null

    data class Start(override val operationId: String) : OpCommand() {
        override val type: OpCommandType get() = OpCommandType.START
    }

    data class Pause(override val operationId: String, override val reason: String) : OpCommand() {
        override val type: OpCommandType get() = OpCommandType.PAUSE
    }

    data class Complete(override val operationId: String) : OpCommand() {
        override val type: OpCommandType get() = OpCommandType.COMPLETE
    }
}
