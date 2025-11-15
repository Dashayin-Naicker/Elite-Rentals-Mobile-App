package com.rentals.eliterentals

data class Payment(
    val date: String?,  // ISO string
    val amount: Double?,
    val status: String?,
    val tenantId: Int?
)

