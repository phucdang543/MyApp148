package com.example.myapp.process.user

import retrofit2.http.GET

interface UserApiService {

    @GET("api/user")
    suspend fun getUserProfile(): UserResponse
}
