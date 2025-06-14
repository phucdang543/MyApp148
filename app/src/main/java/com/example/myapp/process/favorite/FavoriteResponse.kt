package com.example.myapp.process.favorite

import com.example.myapp.process.getsong.Song

data class FavoriteResponse(
    val message: String,
    val data: FavoriteData
)

data class FavoriteData(
    val userId: Int,
    val songId: Int
)

data class FavoriteListResponse(
    val data: List<Song>,
    val total: Int,
    val limit: Int,
    val totalPages: Int,
    val currentPage: Int
)