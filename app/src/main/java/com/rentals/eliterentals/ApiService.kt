package com.rentals.eliterentals

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

}
