package com.example.myapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.databinding.ItemPlaylistBinding
import com.example.myapp.process.playlist.PlaylistResponse

class PlaylistAdapter :
    ListAdapter<PlaylistResponse, PlaylistAdapter.PlaylistViewHolder>(DiffCallback) {

    private var onItemClick: ((PlaylistResponse, Int) -> Unit)? = null
    private var onEditClick: ((PlaylistResponse, Int) -> Unit)? = null
    private var onDeleteClick: ((PlaylistResponse, Int) -> Unit)? = null

    inner class PlaylistViewHolder(val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = getItem(position)

        holder.binding.apply {
            tvPlaylistName.text = playlist.name
            tvCreatedDate.text = formatDate(playlist.createdAt)

            root.setOnClickListener {
                onItemClick?.invoke(playlist, position)
            }

            imgbtnEdit.setOnClickListener {
                onEditClick?.invoke(playlist, position)
            }

            imgbtnDelete.setOnClickListener {
                onDeleteClick?.invoke(playlist, position)
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                java.util.Locale.getDefault()
            )
            val outputFormat =
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: return "")
        } catch (e: Exception) {
            ""
        }
    }

    fun setOnItemClickListener(listener: (PlaylistResponse, Int) -> Unit) {
        onItemClick = listener
    }

    fun setOnEditClickListener(listener: (PlaylistResponse, Int) -> Unit) {
        onEditClick = listener
    }

    fun setOnDeleteClickListener(listener: (PlaylistResponse, Int) -> Unit) {
        onDeleteClick = listener
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<PlaylistResponse>() {
            override fun areItemsTheSame(
                oldItem: PlaylistResponse,
                newItem: PlaylistResponse
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: PlaylistResponse,
                newItem: PlaylistResponse
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}