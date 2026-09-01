package com.precisionfarming.mobile.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Encrypted access-token store (Android). Falls back to in-memory on crypto failure. */
object TokenStore {
    private const val PREFS = "pf_secure_session"
    private const val KEY = "access_token"
    private const val KEY_USER_ID = "user_id"

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var memory: String? = null

    @Volatile
    private var memoryUserId: String? = null

    fun init(context: Context) {
        if (prefs != null) return
        try {
            val master = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS,
                master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            prefs = null
        }
    }

    fun save(accessToken: String, userId: String? = null) {
        memory = accessToken
        memoryUserId = userId
        try {
            prefs?.edit()
                ?.putString(KEY, accessToken)
                ?.also { editor ->
                    if (userId != null) editor.putString(KEY_USER_ID, userId)
                    else editor.remove(KEY_USER_ID)
                }
                ?.apply()
        } catch (_: Exception) {
            // keep memory only
        }
    }

    fun read(): String? {
        memory?.let { return it }
        return try {
            prefs?.getString(KEY, null)?.also { memory = it }
        } catch (_: Exception) {
            null
        }
    }

    fun readUserId(): String? {
        memoryUserId?.let { return it }
        return try {
            prefs?.getString(KEY_USER_ID, null)?.also { memoryUserId = it }
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        memory = null
        memoryUserId = null
        try {
            prefs?.edit()?.remove(KEY)?.remove(KEY_USER_ID)?.apply()
        } catch (_: Exception) {
            // ignore
        }
    }
}
