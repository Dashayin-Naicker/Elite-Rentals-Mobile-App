package com.rentals.eliterentals

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Users
    @POST("api/Users/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/Users/sso")
    fun ssoLogin(@Body request: SsoLoginRequest): Call<LoginResponse>

    @POST("api/Users/signup")
    suspend fun registerUser(@Body request: RegisterRequest): Response<UserDto>

    @GET("api/Users")
    suspend fun getAllUsers(@Header("Authorization") bearer: String): Response<List<UserDto?>>

    @PUT("api/Users/{id}")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int,
        @Body user: User
    ): Response<User>

    @PATCH("api/Users/{id}/status")
    suspend fun toggleUserStatus(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int
    ): Response<Unit> // Use Unit for empty body

    // Properties
    @GET("api/Property")
    suspend fun getAllProperties(@Header("Authorization") bearer: String): Response<List<PropertyDto>>

    @POST("api/Property")
    suspend fun createProperty(
        @Header("Authorization") bearer: String,
        @Body property: PropertyDto
    ): Response<PropertyDto>

    @PUT("api/Property/{id}")
    suspend fun updateProperty(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int,
        @Body property: PropertyDto
    ): Response<PropertyDto>

    @PUT("api/Property/{id}/status")
    suspend fun updatePropertyStatus(
        @Header("Authorization") bearer: String,
        @Path("id") propertyId: Int,
        @Body statusDto: PropertyStatusDto
    ): Response<Unit> // Use Unit instead of expecting JSON

    @DELETE("api/Property/{id}")
    suspend fun deleteProperty(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int
    ): Response<Unit> // DELETE often returns 204 NoContent

    // Leases
    @POST("api/Lease")
    suspend fun createLease(
        @Header("Authorization") bearer: String,
        @Body lease: CreateLeaseRequest
    ): Response<LeaseDto>

    @GET("api/Lease")
    suspend fun getAllLeases(
        @Header("Authorization") bearer: String
    ): Response<List<LeaseDto>>
    // 🔹 Submit payment with proof file
    @Multipart
    @POST("api/Payment")
    suspend fun createPayment(
        @Header("Authorization") bearer: String,
        @Part("TenantId") tenantId: RequestBody,
        @Part("Amount") amount: RequestBody,
        @Part("Date") date: RequestBody,
        @Part proof: MultipartBody.Part? = null
    ): Response<PaymentDto>


    // 🔹 Get all payments (Admin/Manager)
    @GET("api/Payment")
    suspend fun getAllPayments(
        @Header("Authorization") bearer: String
    ): Response<List<PaymentDto>>

    // 🔹 Get all payments for a specific tenant
    @GET("api/Payment/tenant/{tenantId}")
    suspend fun getTenantPayments(
        @Header("Authorization") bearer: String,
        @Path("tenantId") tenantId: Int
    ): Response<List<PaymentDto>>

    // 🔹 Get a single payment
    @GET("api/Payment/{id}")
    suspend fun getPaymentById(
        @Header("Authorization") bearer: String,
        @Path("id") paymentId: Int
    ): Response<PaymentDto>

    // 🔹 Download proof of payment
    @GET("api/Payment/{id}/proof")
    suspend fun downloadProof(
        @Header("Authorization") bearer: String,
        @Path("id") paymentId: Int
    ): Response<ResponseBody>

    // 🔹 Update payment status (Admin/Manager)
    @PUT("api/Payment/{id}/status")
    suspend fun updatePaymentStatus(
        @Header("Authorization") bearer: String,
        @Path("id") paymentId: Int,
        @Body dto: PaymentStatusDto
    ): Response<Unit>

    @PATCH("api/users/{id}/password")
    fun changePassword(
        @Path("id") id: Int,
        @Body dto: ChangePasswordRequest
    ): Call<ApiResponse>

    @Multipart
    @POST("api/Maintenance")
    suspend fun createMaintenance(
        @Header("Authorization") token: String,
        @Part("tenantId") tenantId: RequestBody,
        @Part("propertyId") propertyId: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("urgency") urgency: RequestBody,
        @Part proof: MultipartBody.Part? = null
    ): Response<Void>

    @GET("api/Maintenance/my-requests")
    suspend fun getMyRequests(
        @Header("Authorization") token: String
    ): Response<List<Maintenance>>


    // 🔹 Get inbox messages
    @GET("api/Message/inbox/{userId}")
    fun getInboxMessages(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: Int
    ): Call<List<MessageDto>>

    // 🔹 Send a message
    @POST("api/Message")
    fun sendMessage(
        @Header("Authorization") bearer: String,
        @Body message: MessageDto
    ): Call<MessageDto>

    @GET("api/Message/conversation/{user1}/{user2}")
    fun getConversation(
        @Header("Authorization") bearer: String,
        @Path("user1") user1: Int,
        @Path("user2") user2: Int
    ): Call<List<MessageDto>>

    @GET("api/Users/{id}")
    fun getUserById(
        @Header("Authorization") bearer: String,
        @Path("id") userId: Int
    ): Call<UserDto>

    @GET("api/Message/announcements/{userId}")
    fun getAnnouncements(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: Int
    ): Call<List<MessageDto>>



}
