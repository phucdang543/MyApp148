package com.example.myapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ItemHistoryBinding
import com.example.myapp.process.history.HistoryItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryAdapter(private val context: Context) :
    ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    private var onItemClick: ((HistoryItem, Int) -> Unit)? = null
    private var onPlayClick: ((HistoryItem, Int) -> Unit)? = null

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = getItem(position)
        val song = historyItem.song

        holder.binding.apply {
            tvSongName.text = song.title
            tvArtistName.text = song.artist.name
            tvDuration.text = formatDuration(song.duration)
            tvPlayedAt.text = formatPlayedTime(historyItem.playedAt)

            Glide.with(holder.itemView.context)
                .load(song.imageUrl)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(imgSong)

            root.setOnClickListener {
                onItemClick?.invoke(historyItem, position)
            }

            imgbtnPlay.setOnClickListener {
                onPlayClick?.invoke(historyItem, position)
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    private fun formatPlayedTime(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString) ?: return ""

            val now = Calendar.getInstance()
            val playedTime = Calendar.getInstance().apply { time = date }

            val diffInMillis = now.timeInMillis - playedTime.timeInMillis
            val diffInHours = diffInMillis / (1000 * 60 * 60)
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

            when {
                diffInHours < 1 -> {
                    val diffInMinutes = diffInMillis / (1000 * 60)
                    if (diffInMinutes < 1) context.getString(R.string.time_just_now) else context.getString(
                        R.string.time_minutes_ago,
                        diffInMinutes.toInt()
                    )
                }

                diffInHours < 24 -> context.getString(R.string.time_hours_ago, diffInHours.toInt())
                diffInDays == 1L -> context.getString(R.string.time_yesterday)
                diffInDays < 7 -> context.getString(R.string.time_days_ago, diffInDays.toInt())
                else -> {
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun setOnItemClickListener(listener: (HistoryItem, Int) -> Unit) {
        onItemClick = listener
    }

    fun setOnPlayClickListener(listener: (HistoryItem, Int) -> Unit) {
        onPlayClick = listener
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}