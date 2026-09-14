package com.precisionfarming.mobile.data.offline

import kotlinx.coroutines.flow.StateFlow

/** Platform-agnostic connectivity signal. `true` only when a validated network is available. */
interface ConnectivityMonitor {
    val online: StateFlow<Boolean>
}
