package com.rentals.eliterentals

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "offline_payments")
data class OfflinePayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tenantId: Int,
    val amount: String,
    val dateIso: String,
    val fileName: String?,
    val fileUri: String?,
    val timestamp: Long = System.currentTimeMillis(),
    var syncStatus: String = "pending" // pending, synced, conflict, failed
)
