package com.rentals.eliterentals

data class Maintenance(
    val maintenanceId: Int,
    val tenantId: Int,
    val propertyId: Int,
    val description: String,
    val category: String?,
    val urgency: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String?
)
