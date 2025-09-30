package com.rentals.eliterentals

data class User(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val tenantApproval: String,
    val isActive: Boolean
)

