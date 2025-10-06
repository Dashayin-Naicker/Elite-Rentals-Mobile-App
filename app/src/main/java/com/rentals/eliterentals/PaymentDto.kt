package com.rentals.eliterentals

data class PaymentDto(
    val paymentId: Int,
    val tenantId: Int,
    val amount: Double,
    val method: String?,
    val date: String?,
    val status: String,
    val proofType: String?,
)

data class PaymentStatusDto(
    val status: String
)

