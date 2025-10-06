package com.rentals.eliterentals

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class ApiResponse(
    val message: String
)

