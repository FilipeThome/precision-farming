package com.precisionfarming.mobile.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory session. Access fields are volatile so Dispatchers.IO and the UI see the same values.
 * [signedIn] is the only Compose-facing signal; collectors use [kotlinx.coroutines.flow.StateFlow].
 */
object Session {
    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var userId: String? = null
        private set

    @Volatile
    var role: String? = null
        private set

    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    fun set(accessToken: String?, userId: String?, role: String? = this.role) {
        this.accessToken = accessToken
        this.userId = userId
        this.role = role
        _signedIn.value = !accessToken.isNullOrBlank() && !userId.isNullOrBlank()
    }

    fun clear() {
        accessToken = null
        userId = null
        role = null
        _signedIn.value = false
    }
}
