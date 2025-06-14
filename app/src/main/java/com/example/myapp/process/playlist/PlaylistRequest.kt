package com.example.myapp.process.playlist

data class CreatePlaylistRequest(
    val name: String
)

data class UpdatePlaylistRequest(
    val name: String
)

data class AddSongToPlaylistRequest(
    val songId: Int
)

data class RemoveSongFromPlaylistRequest(
    val songId: Int
)