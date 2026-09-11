package com.precisionfarming.mobile.data.offline

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json

/** Encrypted SharedPreferences + kotlinx-serialization. Corrupt payloads fall back to an empty queue. */
class PrefsQueueStore(private val prefs: SharedPreferences) : QueueStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun load(): QueueState {
        val raw = runCatching { prefs.getString(KEY, null) }.getOrNull() ?: return QueueState()
        return runCatching { json.decodeFromString(QueueState.serializer(), raw) }.getOrDefault(QueueState())
    }

    override fun save(state: QueueState) {
        runCatching {
            prefs.edit().putString(KEY, json.encodeToString(QueueState.serializer(), state)).apply()
        }
    }

    companion object {
        const val PREFS = "pf_secure_offline_queue"
        const val LEGACY_PREFS = "pf_offline_queue"
        private const val KEY = "queue_state"

        fun encrypted(context: Context): SharedPreferences? {
            val app = context.applicationContext
            return runCatching {
                val master = MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    app,
                    PREFS,
                    master,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }.getOrNull()
        }

        /** Drop the pre-encryption plaintext file so backups cannot restore it. */
        fun wipeLegacy(context: Context) {
            runCatching { context.applicationContext.deleteSharedPreferences(LEGACY_PREFS) }
        }
    }
}
