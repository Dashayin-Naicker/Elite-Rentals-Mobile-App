package com.rentals.eliterentals

import androidx.room.*


@Dao
interface OfflineDao {

    // ------------------ Requests ------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: OfflineRequest): Long

    @Query("SELECT * FROM offline_requests WHERE syncStatus = 'pending' OR syncStatus = 'conflict'")
    suspend fun getPendingRequests(): List<OfflineRequest>

    @Query("SELECT * FROM offline_requests WHERE id = :id")
    suspend fun getRequestById(id: Int): OfflineRequest?

    @Update
    suspend fun updateRequest(request: OfflineRequest)

    @Delete
    suspend fun deleteRequest(request: OfflineRequest)

    @Query("DELETE FROM offline_requests WHERE syncStatus = 'synced'")
    suspend fun clearSyncedRequests()


    // ------------------ Payments ------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: OfflinePayment): Long

    @Query("SELECT * FROM offline_payments WHERE syncStatus = 'pending' OR syncStatus = 'conflict'")
    suspend fun getPendingPayments(): List<OfflinePayment>

    @Query("SELECT * FROM offline_payments WHERE id = :id")
    suspend fun getPaymentById(id: Int): OfflinePayment?

    @Update
    suspend fun updatePayment(payment: OfflinePayment)

    @Delete
    suspend fun deletePayment(payment: OfflinePayment)

    @Query("DELETE FROM offline_payments WHERE syncStatus = 'synced'")
    suspend fun clearSyncedPayments()
}
