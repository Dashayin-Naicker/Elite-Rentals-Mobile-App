package com.rentals.eliterentals

import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Call

interface ApiService {
    @POST("api/Users/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}
