package com.rentals.eliterentals

data class LeaseDto(
    val leaseId: Int,
    val propertyId: Int,
    val tenantId: Int,
    val startDate: String,
    val endDate: String,
    val deposit: Double,
    val status: String
)
