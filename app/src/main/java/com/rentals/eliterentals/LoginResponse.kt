package com.rentals.eliterentals

data class LoginResponse(
    val token: String,
    val user: UserInfo
)

data class UserInfo(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val managerId: Int?
)

