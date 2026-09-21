package com.precisionfarming.mobile.data

private val MANAGER_ROLES = setOf("ADMIN", "FARM_MANAGER")
private val FLEET_ROLES = setOf("ADMIN", "FARM_MANAGER", "MAINTENANCE")

/** True when the role may approve prescriptions and dispatch loads. */
fun canManageFarmOps(role: String?): Boolean =
    role != null && role.uppercase() in MANAGER_ROLES

fun canCreateFarm(role: String?): Boolean =
    role?.uppercase() == "ADMIN"

fun canWriteMasterData(role: String?): Boolean =
    role != null && role.uppercase() in MANAGER_ROLES

fun canWriteFleet(role: String?): Boolean =
    role != null && role.uppercase() in FLEET_ROLES
