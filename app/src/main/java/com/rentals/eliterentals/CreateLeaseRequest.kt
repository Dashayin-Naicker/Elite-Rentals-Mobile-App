package com.rentals.eliterentals

data class CreateLeaseRequest(
    val propertyId: Int,
    val tenantId: Int,
    val startDate: String,
    val endDate: String,
    val deposit: Double
)
