package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ItemFavoriteBinding
import com.example.myapp.process.getsong.Song
import java.util.Locale

class FavoriteAdapter : ListAdapter<Song, FavoriteAdapter.FavoriteViewHolder>(DiffCallback) {

    private var onItemClick: ((Song, Int) -> Unit)? = null
    private var onRemoveClick: ((Song, Int) -> Unit)? = null

    inner class FavoriteViewHolder(val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val song = getItem(position)

        holder.binding.apply {
            tvSongName.text = song.title
            tvArtistName.text = song.artist.name
            tvDuration.text = formatDuration(song.duration)

            Glide.with(holder.itemView.context)
                .load(song.imageUrl)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(imgSong)

            root.setOnClickListener {
                onItemClick?.invoke(song, position)
            }

            imgbtnRemoveFavorite.setOnClickListener {
                onRemoveClick?.invoke(song, position)
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    fun setOnItemClickListener(listener: (Song, Int) -> Unit) {
        onItemClick = listener
    }

    fun setOnRemoveClickListener(listener: (Song, Int) -> Unit) {
        onRemoveClick = listener
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
                return oldItem == newItem
            }
        }
    }
}