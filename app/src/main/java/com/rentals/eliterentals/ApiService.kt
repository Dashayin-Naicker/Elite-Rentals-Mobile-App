package com.rentals.eliterentals

import okhttp3.ResponseBody
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("api/Users/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/Users/sso")
    fun ssoLogin(@Body request: SsoLoginRequest): Call<LoginResponse>

    @POST("api/Users/signup")
    suspend fun registerUser(@Body request: RegisterRequest): Response<UserDto>

    @GET("api/Users")
    suspend fun getAllUsers(@Header("Authorization") bearer: String): Response<List<UserDto>>

    @PUT("api/Users/{id}")
    suspend fun updateUser(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int,
        @Body update: UserUpdateDto
    ): Response<UserDto>

    @GET("api/Property")
    suspend fun getAllProperties(
        @Header("Authorization") bearer: String
    ): Response<List<PropertyDto>?>


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

    @DELETE("api/Property/{id}")
    suspend fun deleteProperty(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int
    ): Response<Unit>

    // Leases
    @POST("api/Lease")
    suspend fun createLease(
        @Header("Authorization") bearer: String,
        @Body lease: CreateLeaseRequest
    ): Response<LeaseDto>


}
