package com.rentals.eliterentals
data class LeaseDto(
    val leaseId: Int,
    val propertyId: Int,
    val property: PropertyDto?, // <-- must match API
    val tenantId: Int,
    val tenant: TenantDto?,     // <-- must match API
    val startDate: String,
    val endDate: String,
    val deposit: Double,
    val status: String,
    val documentData: String? = null,
    val documentType: String? = null
)


data class TenantDto(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String
)


