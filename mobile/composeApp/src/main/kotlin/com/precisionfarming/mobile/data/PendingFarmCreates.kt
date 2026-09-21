package com.precisionfarming.mobile.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

interface PendingFarmStore {
    fun get(key: String): FarmDto?
    fun put(key: String, farm: FarmDto)
    fun remove(key: String)
    fun clear()
}

fun pendingFarmKey(userId: String, body: FarmUpsert): String =
    "$userId:${body.name}|${body.location}|${body.areaHa}|${body.timezone}"

suspend fun createFarm(
    userId: String?,
    body: FarmUpsert,
    store: PendingFarmStore,
    postFarm: suspend (FarmUpsert) -> FarmDto,
    consumeRefresh: () -> String?,
    refreshHttp: suspend (String) -> Boolean,
): FarmDto {
    val key = userId?.let { pendingFarmKey(it, body) }
    val farm = key?.let { store.get(it) } ?: postFarm(body)
    if (key != null) store.put(key, farm)
    val refresh = consumeRefresh()
    val ok = refresh != null && refreshHttp(refresh)
    if (!ok) throw IllegalStateException("SESSION_REFRESH_FAILED")
    if (key != null) store.remove(key)
    return farm
}

/** Encrypted pending farm-create cache (Android). Falls back to in-memory on crypto failure. */
object PendingFarmCreates : PendingFarmStore {
    private const val PREFS = "pf_pending_farm_creates"
    private const val STATE = "pending"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mapSerializer = MapSerializer(String.serializer(), FarmDto.serializer())
    private val lock = Any()
    private val memory = mutableMapOf<String, FarmDto>()

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        try {
            val master = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val created = EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS,
                master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            prefs = created
            hydrate(created)
        } catch (_: Exception) {
            prefs = null
        }
    }

    override fun get(key: String): FarmDto? = synchronized(lock) { memory[key] }

    override fun put(key: String, farm: FarmDto) {
        synchronized(lock) {
            memory[key] = farm
            persistLocked()
        }
    }

    override fun remove(key: String) {
        synchronized(lock) {
            memory.remove(key)
            persistLocked()
        }
    }

    override fun clear() {
        synchronized(lock) {
            memory.clear()
            persistLocked()
        }
    }

    private fun hydrate(prefs: SharedPreferences) {
        val raw = runCatching { prefs.getString(STATE, null) }.getOrNull() ?: return
        val loaded = runCatching { json.decodeFromString(mapSerializer, raw) }.getOrDefault(emptyMap())
        synchronized(lock) {
            memory.clear()
            memory.putAll(loaded)
        }
    }

    private fun persistLocked() {
        try {
            prefs?.edit()?.putString(STATE, json.encodeToString(mapSerializer, memory.toMap()))?.apply()
        } catch (_: Exception) {
            // keep memory only
        }
    }
}
