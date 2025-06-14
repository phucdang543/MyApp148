package com.example.myapp.process.playlist

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    val id: Int,
    val name: String,
    val userId: Int,
    val createdAt: String
) : Parcelable