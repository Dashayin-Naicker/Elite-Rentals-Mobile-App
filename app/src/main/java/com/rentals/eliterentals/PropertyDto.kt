package com.rentals.eliterentals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Manager(
    val userId: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
) : Parcelable

@Parcelize
data class PropertyDto(
    val propertyId: Int = 0,
    val managerId: Int? = null,
    val manager: Manager? = null,
    val title: String? = null,
    val description: String? = null,
    val address: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
    val rentAmount: Double? = null,
    val numOfBedrooms: Int? = null,
    val numOfBathrooms: Int? = null,
    val parkingType: String? = null,
    val numOfParkingSpots: Int? = null,
    val petFriendly: Boolean? = null,
    val status: String? = null,
    val imageUrls: List<String> = emptyList()  // <-- NEW
) : Parcelable



