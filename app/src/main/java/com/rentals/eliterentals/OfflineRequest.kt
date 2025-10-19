package com.rentals.eliterentals

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "offline_requests")
data class OfflineRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: Int,
    val propertyId: Int,
    val category: String,
    val urgency: String,
    val description: String,
    val imageUri: String?,
    val timestamp: Long = System.currentTimeMillis(),
    var syncStatus: String = "pending" // pending, synced, conflict, failed
)
