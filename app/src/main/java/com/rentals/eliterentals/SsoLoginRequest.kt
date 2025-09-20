package com.rentals.eliterentals

data class SsoLoginRequest(
    val provider: String,
    val token: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String?
)

