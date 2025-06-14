package com.example.myapp.process.getsong

data class SongDetailResponse(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int
)

data class PlaySongResponse(
    val message: String,
    val data: PlaySongData
)

data class PlaySongData(
    val url: String
)