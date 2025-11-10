package com.rentals.eliterentals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConflictItem(
    val id: Int,
    val type: String // "maintenance" or "payment"
) : Parcelable
