package com.example.myapp.process.history

import com.example.myapp.process.getsong.Song

data class HistoryResponse(
    val data: List<HistoryItem>,
    val total: Int,
    val limit: Int,
    val totalPages: Int,
    val currentPage: Int
)

data class HistoryItem(
    val id: Int,
    val song: Song,
    val playedAt: String
)