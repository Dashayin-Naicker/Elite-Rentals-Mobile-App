package com.rentals.eliterentals

data class UserDto(
    val userId: Int,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val role: String?,
    val isActive: Boolean,
    val tenantApproval: String?
)
