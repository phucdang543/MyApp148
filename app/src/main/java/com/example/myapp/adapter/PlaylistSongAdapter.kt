package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ItemPlaylistSongBinding
import com.example.myapp.process.playlist.PlaylistSongItem
import java.util.Locale

class PlaylistSongAdapter :
    ListAdapter<PlaylistSongItem, PlaylistSongAdapter.PlaylistSongViewHolder>(DiffCallback) {

    private var onItemClick: ((PlaylistSongItem, Int) -> Unit)? = null
    private var onRemoveClick: ((PlaylistSongItem, Int) -> Unit)? = null

    inner class PlaylistSongViewHolder(val binding: ItemPlaylistSongBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistSongViewHolder {
        val binding = ItemPlaylistSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistSongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistSongViewHolder, position: Int) {
        val songItem = getItem(position)
        val song = songItem.song

        holder.binding.apply {
            tvSongName.text = song.title
            tvArtistName.text = song.artist.name
            tvDuration.text = formatDuration(song.duration)
            tvAddedDate.text = formatAddedDate(songItem.addedAt)

            Glide.with(holder.itemView.context)
                .load(song.imageUrl)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(imgSong)

            root.setOnClickListener {
                onItemClick?.invoke(songItem, position)
            }

            imgbtnRemove.setOnClickListener {
                onRemoveClick?.invoke(songItem, position)
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    private fun formatAddedDate(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.getDefault()
            )
            val outputFormat = java.text.SimpleDateFormat("dd/MM", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: return "")
        } catch (e: Exception) {
            ""
        }
    }

    fun setOnItemClickListener(listener: (PlaylistSongItem, Int) -> Unit) {
        onItemClick = listener
    }

    fun setOnRemoveClickListener(listener: (PlaylistSongItem, Int) -> Unit) {
        onRemoveClick = listener
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<PlaylistSongItem>() {
            override fun areItemsTheSame(
                oldItem: PlaylistSongItem,
                newItem: PlaylistSongItem
            ): Boolean {
                return oldItem.songId == newItem.songId && oldItem.playlistId == newItem.playlistId
            }

            override fun areContentsTheSame(
                oldItem: PlaylistSongItem,
                newItem: PlaylistSongItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}