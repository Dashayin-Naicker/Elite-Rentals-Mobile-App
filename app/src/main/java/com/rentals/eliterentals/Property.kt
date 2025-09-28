package com.rentals.eliterentals

import java.math.BigDecimal

data class Property(
    val title: String,
    val description: String,
    val address: String,
    val city: String,
    val province: String,
    val country: String,
    val rentAmount: BigDecimal,
    val numOfBedrooms: Int,
    val numOfBathrooms: Int,
    val parkingType: String,
    val numOfParkingSpots: Int,
    val petFriendly: Boolean,
    val status: String
)
