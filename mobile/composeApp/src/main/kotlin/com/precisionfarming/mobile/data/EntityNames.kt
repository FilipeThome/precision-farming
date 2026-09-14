package com.precisionfarming.mobile.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime id → display-name registry fed by API responses (farms, fields, machines).
 * Nothing is hardcoded: names exist only after the backend returned them.
 */
object EntityNames {
    private val names = ConcurrentHashMap<String, String>()

    fun registerFarms(items: List<FarmDto>) = items.forEach { put(it.id, it.name) }

    fun registerFields(items: List<FieldDto>) = items.forEach { put(it.id, it.name) }

    fun registerMachines(items: List<MachineDto>) = items.forEach { put(it.id, it.name) }

    fun nameOf(id: String?): String? = id?.let { names[it] }

    fun clear() = names.clear()

    private fun put(id: String, name: String?) {
        val value = name?.trim().orEmpty()
        if (value.isNotEmpty()) names[id] = value
    }
}
