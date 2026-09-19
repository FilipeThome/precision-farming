package com.precisionfarming.mobile.data

private val MANAGER_ROLES = setOf("ADMIN", "FARM_MANAGER")

/** True when the role may approve prescriptions and dispatch loads. */
fun canManageFarmOps(role: String?): Boolean =
    role != null && role.uppercase() in MANAGER_ROLES
