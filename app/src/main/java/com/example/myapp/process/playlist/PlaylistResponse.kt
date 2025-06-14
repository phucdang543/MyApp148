package com.example.myapp.process.playlist

import com.example.myapp.process.getsong.Song

data class PlaylistResponse(
    val id: Int,
    val name: String,
    val userId: Int,
    val createdAt: String
)

data class PlaylistListResponse(
    val data: List<PlaylistResponse>
)

data class PlaylistSongsResponse(
    val data: List<PlaylistSongItem>,
    val total: Int,
    val currentPage: Int,
    val totalPages: Int,
    val limit: Int
)

data class PlaylistSongItem(
    val playlistId: Int,
    val songId: Int,
    val addedAt: String,
    val song: Song
)

data class AddSongResponse(
    val message: String,
    val data: AddSongData
)

data class AddSongData(
    val playlistId: Int,
    val songId: Int,
    val addedAt: String
)

data class RemoveSongResponse(
    val message: String,
    val data: RemoveSongData
)

data class RemoveSongData(
    val playlistId: Int,
    val songId: Int,
    val addedAt: String
)