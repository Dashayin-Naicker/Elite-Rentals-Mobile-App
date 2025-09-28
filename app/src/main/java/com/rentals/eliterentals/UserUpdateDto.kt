package com.rentals.eliterentals

data class UserUpdateDto(
    val tenantApproval: String? = null,
    val isActive: Boolean? = null
)
