package com.example.myapp.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ActivityPlaySongBinding
import com.example.myapp.manager.MusicManager
import com.example.myapp.process.RetrofitClient
import com.example.myapp.process.favorite.FavoriteRequest
import com.example.myapp.process.getsong.Song
import com.example.myapp.service.MusicForegroundService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Locale

class PlaySongActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaySongBinding
    private lateinit var playlist: ArrayList<Song>
    private var currentPosition: Int = 0
    private var isFavorite = false
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isUserSeeking = false

    private lateinit var musicManager: MusicManager

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    private val songChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            syncWithMusicManager()

            if (playlist.isNotEmpty()) {
                loadSong(currentPosition)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlaySongBinding.inflate(layoutInflater)
        setContentView(binding.root)

        musicManager = MusicManager.getInstance()

        val songChangedFilter = IntentFilter("com.example.myapp.SONG_CHANGED")
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(songChangedReceiver, songChangedFilter)

        getIntentData()

        syncWithMusicManager()

        setupUI()
        setupClickListeners()
        setupSeekBar()

        if (playlist.isNotEmpty()) {
            musicManager.setPlaylist(playlist, currentPosition)

            val fromMiniPlayer = intent.getBooleanExtra("from_mini_player", false)
            val currentSong = playlist[currentPosition]
            val managerCurrentSong = musicManager.getCurrentSong()

            val isPlayingSameSong = managerCurrentSong != null &&
                    managerCurrentSong.id == currentSong.id &&
                    mediaPlayer != null &&
                    (musicManager.isPlaying.value ?: false)

            if (fromMiniPlayer && isPlayingSameSong) {
                setupUIForCurrentSong(currentPosition)
                checkFavoriteStatus(currentSong.id)
                if (isPlaying) {
                    startProgressUpdate()
                }
            } else {
                loadSong(currentPosition)
            }
        }

        handleControlAction(intent)
    }

    private fun setupUIForCurrentSong(position: Int) {
        if (position < 0 || position >= playlist.size) return

        val currentSong = playlist[position]
        binding.tvSongName.text = currentSong.title
        binding.tvArtistName.text = currentSong.artist.name
        binding.tvTimeMax.text = formatDuration(currentSong.duration)
        binding.seekBar.max = currentSong.duration * 1000

        mediaPlayer?.let { player ->
            binding.seekBar.progress = player.currentPosition
            binding.tvTimeCurrent.text = formatDuration(player.currentPosition / 1000)
        }

        val playButtonRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.imgbtnPlay.setImageResource(playButtonRes)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleControlAction(intent)
    }

    private fun handleControlAction(intent: Intent?) {
        val controlAction = intent?.getStringExtra("control_action")

        when (controlAction) {
            "previous" -> {
                previousSong()
            }
            "play_pause" -> {
                togglePlayPause()
            }
            "next" -> {
                nextSong()
            }
        }
    }

    private fun getIntentData() {
        playlist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("playlist", Song::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("playlist") ?: arrayListOf()
        }
        currentPosition = intent.getIntExtra("position", 0)
    }

    private fun syncWithMusicManager() {
        if (playlist.isEmpty()) {
            val managerPlaylist = musicManager.getPlaylist()
            if (managerPlaylist.isNotEmpty()) {
                playlist = managerPlaylist
                currentPosition = musicManager.getCurrentSongPosition()
            }
        }

        isPlaying = musicManager.isPlaying.value ?: false
        mediaPlayer = musicManager.mediaPlayer
    }

    private fun setupUI() {
        if (playlist.isNotEmpty()) {
            val currentSong = playlist[currentPosition]
            Glide.with(this).load(currentSong.imageUrl).into(binding.imgSong)
            binding.tvSongName.text = currentSong.title
            binding.tvArtistName.text = currentSong.artist.name
            binding.tvTimeMax.text = formatDuration(currentSong.duration)
            binding.tvTimeCurrent.text = "00:00"
            binding.seekBar.max = currentSong.duration * 1000
        } else {
            binding.tvSongName.text = getString(R.string.no_song_playing)
        }
    }

    private fun setupClickListeners() {
        binding.imgbtnNext.setOnClickListener {
            nextSong()
        }

        binding.imgbtnPlayback.setOnClickListener {
            previousSong()
        }

        binding.imgbtnReplay.setOnClickListener {
            replaySong()
        }

        binding.imgbtnShuffle.setOnClickListener {
            shuffleSong()
        }

        binding.imgbtnBack.setOnClickListener {
            finish()
        }

        binding.imgbtnFavorite.setOnClickListener {
            toggleFavorite()
        }

        binding.imgbtnPlay.setOnClickListener {
            togglePlayPause()
        }
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvTimeCurrent.text = formatDuration(progress / 1000)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let {
                    mediaPlayer?.seekTo(it.progress)
                }
            }
        })
    }

    private fun loadSong(position: Int) {
        if (position < 0 || position >= playlist.size) return

        currentPosition = position
        val currentSong = playlist[currentPosition]

        binding.tvSongName.text = currentSong.title
        binding.tvArtistName.text = currentSong.artist.name
        Glide.with(this).load(currentSong.imageUrl).into(binding.imgSong)
        binding.tvTimeMax.text = formatDuration(currentSong.duration)
        binding.tvTimeCurrent.text = "00:00"
        binding.seekBar.max = currentSong.duration * 1000
        binding.seekBar.progress = 0

        checkFavoriteStatus(currentSong.id)
        getPlayUrlAndPlay(currentSong.id)
    }

    private fun getPlayUrlAndPlay(songId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPlayUrl(songId)
                val playUrl = response.data.url
                playSong(playUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                playSong(playlist[currentPosition].url)
            }
        }
    }

    private fun playSong(url: String) {
        try {
            mediaPlayer?.release()
            isPlaying = false
            musicManager.setPlayingState(false)

            val currentSong = playlist[currentPosition]

            musicManager.startMusicService(this, currentSong)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener {
                    it.start()
                    this@PlaySongActivity.isPlaying = true
                    musicManager.setPlayingState(true)
                    musicManager.mediaPlayer = this
                    musicManager.setCurrentSong(currentSong)
                    musicManager.updateCurrentSongPosition(currentPosition)
                    musicManager.setDuration(currentSong.duration)
                    musicManager.syncStateFromActivity(currentSong, true, currentPosition, this)

                    binding.imgbtnPlay.setImageResource(R.drawable.ic_pause)
                    startProgressUpdate()
                    updatePlayStateToService()
                    trackPlaybackHistory(currentSong.id)
                }
                setOnCompletionListener {
                    nextSong()
                }
                setOnErrorListener { _, _, _ ->
                    showError(getString(R.string.cannot_play_music))
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showError(getString(R.string.playback_error))
        }
    }

    private fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (isPlaying) {
                player.pause()
                isPlaying = false
                musicManager.setPlayingState(false)
                binding.imgbtnPlay.setImageResource(R.drawable.ic_play)
                stopProgressUpdate()
            } else {
                player.start()
                isPlaying = true
                musicManager.setPlayingState(true)
                binding.imgbtnPlay.setImageResource(R.drawable.ic_pause)
                startProgressUpdate()
            }

            musicManager.syncStateFromActivity(
                playlist[currentPosition],
                isPlaying,
                currentPosition,
                player
            )
            updatePlayStateToService()
        }
    }

    private fun nextSong() {
        if (currentPosition < playlist.size - 1) {
            currentPosition++
            loadSong(currentPosition)
            updateNotificationWithCurrentSong()
        }
    }

    private fun previousSong() {
        if (currentPosition > 0) {
            currentPosition--
            loadSong(currentPosition)
            updateNotificationWithCurrentSong()
        }
    }

    private fun updateNotificationWithCurrentSong() {
        val currentSong = playlist[currentPosition]
        val updateIntent = Intent(this, MusicForegroundService::class.java)
        updateIntent.action = MusicForegroundService.ACTION_UPDATE_SONG
        updateIntent.putExtra("song_title", currentSong.title)
        updateIntent.putExtra("artist_name", currentSong.artist.name)
        updateIntent.putExtra("image_url", currentSong.imageUrl)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(updateIntent)
        } else {
            startService(updateIntent)
        }
    }

    private fun updatePlayStateToService() {
        val updateIntent = Intent(this, MusicForegroundService::class.java)
        updateIntent.action = MusicForegroundService.ACTION_UPDATE_PLAY_STATE
        updateIntent.putExtra("is_playing", isPlaying)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(updateIntent)
        } else {
            startService(updateIntent)
        }
    }

    private fun replaySong() {
        mediaPlayer?.seekTo(0)
        binding.seekBar.progress = 0
        binding.tvTimeCurrent.text = "00:00"
    }

    private fun shuffleSong() {
        if (playlist.size > 1) {
            var randomPosition: Int
            do {
                randomPosition = (0 until playlist.size).random()
            } while (randomPosition == currentPosition)
            loadSong(randomPosition)
        }
    }

    private fun toggleFavorite() {
        val currentSong = playlist[currentPosition]

        lifecycleScope.launch {
            try {
                val request = FavoriteRequest(songId = currentSong.id)

                if (isFavorite) {
                    RetrofitClient.favoriteService.removeFromFavorites(request)
                    isFavorite = false
                    binding.imgbtnFavorite.setImageResource(R.drawable.ic_add_favorite)
                    showSuccess(getString(R.string.removed_from_favorites_success))
                } else {
                    RetrofitClient.favoriteService.addToFavorites(request)
                    isFavorite = true
                    binding.imgbtnFavorite.setImageResource(R.drawable.ic_delete_favorite)
                    showSuccess(getString(R.string.added_to_favorites_success))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError(getString(R.string.error_update_favorites))
            }
        }
    }

    private fun checkFavoriteStatus(songId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.favoriteService.getFavorites(page = 1, limit = 1000)
                isFavorite = response.data.any { it.id == songId }
                updateFavoriteIcon()
            } catch (e: Exception) {
                e.printStackTrace()
                isFavorite = loadFavoriteStatusLocal(songId)
                updateFavoriteIcon()
            }
        }
    }

    private fun updateFavoriteIcon() {
        val iconRes = if (isFavorite) R.drawable.ic_delete_favorite else R.drawable.ic_add_favorite
        binding.imgbtnFavorite.setImageResource(iconRes)
    }

    private fun loadFavoriteStatusLocal(songId: Int): Boolean {
        val prefs = getSharedPreferences("favorites", MODE_PRIVATE)
        return prefs.getBoolean(songId.toString(), false)
    }

    private fun startProgressUpdate() {
        handler.post(updateProgressRunnable)
    }

    private fun stopProgressUpdate() {
        handler.removeCallbacks(updateProgressRunnable)
    }

    private fun updateProgress() {
        mediaPlayer?.let { player ->
            if (isPlaying && !isUserSeeking) {
                val currentPosition = player.currentPosition
                binding.seekBar.progress = currentPosition
                binding.tvTimeCurrent.text = formatDuration(currentPosition / 1000)
                musicManager.setCurrentPosition(currentPosition / 1000)
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        stopProgressUpdate()

        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(songChangedReceiver)
        } catch (e: Exception) {
        }

        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        stopProgressUpdate()
    }

    override fun onResume() {
        super.onResume()

        syncWithMusicManager()
        updateUIFromCurrentState()

        if (isPlaying) {
            startProgressUpdate()
        }
    }

    private fun updateUIFromCurrentState() {
        if (playlist.isNotEmpty() && currentPosition < playlist.size) {
            val currentSong = playlist[currentPosition]
            binding.tvSongName.text = currentSong.title
            binding.tvArtistName.text = currentSong.artist.name
            Glide.with(this).load(currentSong.imageUrl).into(binding.imgSong)

            val playButtonRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            binding.imgbtnPlay.setImageResource(playButtonRes)

            mediaPlayer?.let { player ->
                if (player.isPlaying || isPlaying) {
                    binding.seekBar.progress = player.currentPosition
                    binding.tvTimeCurrent.text = formatDuration(player.currentPosition / 1000)
                }
            }
        }
    }

    private fun trackPlaybackHistory(songId: Int) {
        lifecycleScope.launch {
            try {
                savePlaybackHistoryLocal(songId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun savePlaybackHistoryLocal(songId: Int) {
        val prefs = getSharedPreferences("playback_history", MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        prefs.edit { putLong("song_${songId}_last_played", currentTime) }
    }

}