package com.rentals.eliterentals

data class CreateLeaseRequest(
    val deposit: Double,
    val startDate: String,
    val endDate: String,
    val propertyId: Int,
    val tenantId: Int
)
