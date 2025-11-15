package com.rentals.eliterentals

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ---------- Users ----------
    @POST("api/Users/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/Users/sso")
    fun ssoLogin(@Body request: SsoLoginRequest): Call<LoginResponse>

    @POST("api/Users/signup")
    suspend fun registerUser(@Body request: RegisterRequest): Response<UserDto>

    @GET("api/Users")
    suspend fun getAllUsers(@Header("Authorization") bearer: String): Response<List<UserDto?>>

    @GET("api/Users/{id}")
    fun getUserById(
        @Header("Authorization") bearer: String,
        @Path("id") userId: Int
    ): Call<UserDto>

    @PUT("api/Users/{id}")
    suspend fun updateUser(
        @Header("Authorization") bearer: String,
        @Path("id") userId: Int,
        @Body user: User
    ): Response<User>

    @PATCH("api/Users/{id}/status")
    suspend fun toggleUserStatus(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int
    ): Response<Unit>

    // *** Reverted to match SettingsActivity (auth comes from your OkHttp interceptor) ***
    @PATCH("api/Users/{id}/password")
    fun changePassword(
        @Path("id") id: Int,
        @Body dto: ChangePasswordRequest
    ): Call<ApiResponse>

    @PATCH("api/Users/{id}/fcmtoken")
    fun updateFcmToken(
        @Header("Authorization") bearer: String,
        @Path("id") userId: Int,
        @Body request: FcmTokenRequest
    ): Call<Void>

    // ---------- Properties ----------
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
    ): Response<Unit>

    @DELETE("api/Property/{id}")
    suspend fun deleteProperty(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int
    ): Response<Unit>

    // ---------- Leases ----------
    @POST("api/Lease")
    suspend fun createLease(
        @Header("Authorization") bearer: String,
        @Body lease: CreateLeaseRequest
    ): Response<LeaseDto>

    @GET("api/Lease")
    suspend fun getAllLeases(
        @Header("Authorization") bearer: String
    ): Response<List<LeaseDto>>

    // ---------- Payments ----------
    @Multipart
    @POST("api/Payment")
    suspend fun createPayment(
        @Header("Authorization") bearer: String,
        @Part("TenantId") tenantId: RequestBody,
        @Part("Amount") amount: RequestBody,
        @Part("Date") date: RequestBody,
        @Part proof: MultipartBody.Part? = null
    ): Response<PaymentDto>

    @GET("api/Payment")
    suspend fun getAllPayments(
        @Header("Authorization") bearer: String
    ): Response<List<PaymentDto>>

    @GET("api/Payment/tenant/{tenantId}")
    suspend fun getTenantPayments(
        @Header("Authorization") bearer: String,
        @Path("tenantId") tenantId: Int
    ): Response<List<PaymentDto>>

    @GET("api/Payment/{id}")
    suspend fun getPaymentById(
        @Header("Authorization") bearer: String,
        @Path("id") paymentId: Int
    ): Response<PaymentDto>

    @GET("api/Payment/{id}/proof")
    suspend fun downloadProof(
        @Header("Authorization") bearer: String,
        @Path("id") paymentId: Int
    ): Response<ResponseBody>

    @PUT("api/Payment/{id}/status")
    suspend fun updatePaymentStatus(
        @Header("Authorization") bearer: String,
        @Path("id") paymentId: Int,
        @Body dto: PaymentStatusDto
    ): Response<Unit>

    // ---------- Maintenance ----------
    @Multipart
    @POST("api/Maintenance")
    suspend fun createMaintenance(
        @Header("Authorization") bearer: String,
        @Part("tenantId") tenantId: RequestBody,
        @Part("propertyId") propertyId: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("urgency") urgency: RequestBody,
        @Part proof: MultipartBody.Part? = null
    ): Response<Void>

    @GET("api/Maintenance/my-requests")
    suspend fun getMyRequests(
        @Header("Authorization") bearer: String
    ): Response<List<Maintenance>>

    @GET("api/Maintenance/caretaker-requests")
    suspend fun getCaretakerRequests(
        @Header("Authorization") token: String
    ): Response<List<Maintenance>>

    @Multipart
    @POST("api/Maintenance/{id}/proof")
    suspend fun updateMaintenanceProof(
        @Header("Authorization") token: String,
        @Path("id") maintenanceId: Int,
        @Part proof: MultipartBody.Part
    ): Response<Void>


    @PUT("api/Maintenance/{id}/status")
    suspend fun updateMaintenanceStatus(
        @Header("Authorization") token: String,
        @Path("id") maintenanceId: Int,
        @Body dto: MaintenanceStatusDto
    ): Response<Unit>

    @POST("api/maintenance/{id}/comment")
    suspend fun addMaintenanceComment(
        @Header("Authorization") token: String,
        @Path("id") maintenanceId: Int,
        @Body commentDto: MaintenanceCommentDto
    ): Response<ResponseBody>



    // ---------- Messages / Inbox ----------
    @GET("api/Message/inbox/{userId}")
    fun getInboxMessages(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: Int
    ): Call<List<MessageDto>>

    @POST("api/Message")
    fun sendMessage(
        @Header("Authorization") bearer: String,
        @Body message: MessageDto
    ): retrofit2.Call<MessageDto>

    @GET("api/Message/conversation/{user1}/{user2}")
    fun getConversation(
        @Header("Authorization") bearer: String,
        @Path("user1") user1: Int,
        @Path("user2") user2: Int
    ): Call<List<MessageDto>>

    @GET("api/Message/announcements/{userId}")
    fun getAnnouncements(
        @Header("Authorization") bearer: String,
        @Path("userId") userId: Int
    ): Call<List<MessageDto>>

    // ---------- Chatbot (auth + language) ----------
    data class ChatbotMessageCreate(
        val messageText: String,
        val isChatbot: Boolean = true,
        val language: String? = null // "en" | "zu" | "st"
    )

    @POST("api/Message")
    suspend fun sendChatbotMessage(
        @Header("Authorization") bearer: String,
        @Body body: ChatbotMessageCreate
    ): Response<MessageDto>

    @GET("api/Message")
    suspend fun getMessages(
        @Header("Authorization") bearer: String
    ): Response<List<MessageDto>>

    // ---------- Reports (escalation) ----------
    @POST("api/Report")
    suspend fun createReport(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>

    @POST("api/Message")
    fun sendMessageSimple(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): retrofit2.Call<Void>

    @GET("api/Lease")
    suspend fun getLeases(@Header("Authorization") bearer: String): Response<List<LeaseDto>>

    @GET("api/Maintenance")
    suspend fun getMaintenance(@Header("Authorization") bearer: String): Response<List<Maintenance>>

    @GET("api/Payment")
    suspend fun getPayments(@Header("Authorization") bearer: String): Response<List<PaymentDto>>


}
