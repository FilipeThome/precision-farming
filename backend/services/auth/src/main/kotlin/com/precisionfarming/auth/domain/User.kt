package com.precisionfarming.auth.domain

enum class UserRole { ADMIN, FARM_MANAGER, OPERATOR, MAINTENANCE }
enum class UserStatus { ACTIVE, DISABLED }

data class User(
    val id: java.util.UUID,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val status: UserStatus,
)
