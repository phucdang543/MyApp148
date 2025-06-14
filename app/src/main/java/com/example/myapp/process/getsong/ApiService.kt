package com.example.myapp.process.getsong

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/song")
    suspend fun getSongs(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): SongResponse

    @GET("api/song/{id}")
    suspend fun getSongById(@Path("id") songId: Int): SongDetailResponse

    @GET("api/song/play/{songId}")
    suspend fun getPlayUrl(@Path("songId") songId: Int): PlaySongResponse
}