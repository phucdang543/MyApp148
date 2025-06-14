package com.example.myapp.process.favorite

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query

interface FavoriteApiService {

    @POST("api/song/favorite")
    suspend fun addToFavorites(@Body request: FavoriteRequest): FavoriteResponse

    @HTTP(method = "DELETE", path = "api/song/favorite", hasBody = true)
    suspend fun removeFromFavorites(@Body request: FavoriteRequest): FavoriteResponse

    @GET("api/song/favorite")
    suspend fun getFavorites(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): FavoriteListResponse
}