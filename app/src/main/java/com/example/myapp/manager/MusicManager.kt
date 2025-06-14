package com.example.myapp.manager

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapp.process.getsong.Song
import com.example.myapp.service.MusicForegroundService

class MusicManager private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: MusicManager? = null

        fun getInstance(): MusicManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicManager().also { INSTANCE = it }
            }
        }
    }

    private val _currentSong = MutableLiveData<Song?>()
    val currentSong: LiveData<Song?> = _currentSong

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPosition = MutableLiveData<Int>(0)
    val currentPosition: LiveData<Int> = _currentPosition

    private val _duration = MutableLiveData<Int>(0)
    val duration: LiveData<Int> = _duration

    private var playlist: ArrayList<Song> = arrayListOf()
    private var currentSongPosition: Int = 0

    var mediaPlayer: MediaPlayer? = null

    fun setCurrentSong(song: Song) {
        _currentSong.value = song
    }

    fun setPlaylist(songs: ArrayList<Song>, position: Int) {
        playlist = songs
        currentSongPosition = position
        if (position >= 0 && position < songs.size) {
            _currentSong.value = songs[position]
        }
    }

    fun setPlayingState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun setCurrentPosition(position: Int) {
        _currentPosition.value = position
    }

    fun setDuration(duration: Int) {
        _duration.value = duration
    }

    fun getCurrentSong(): Song? = _currentSong.value

    fun getPlaylist(): ArrayList<Song> = playlist

    fun getCurrentSongPosition(): Int {
        return currentSongPosition
    }

    fun updateCurrentSongPosition(position: Int) {
        currentSongPosition = position
        if (position >= 0 && position < playlist.size) {
            val song = playlist[position]
            _currentSong.value = song
        }
    }

    fun syncStateFromActivity(song: Song?, playing: Boolean, position: Int, player: MediaPlayer?) {
        if (song != null) {
            _currentSong.value = song
        }
        _isPlaying.value = playing
        currentSongPosition = position
        mediaPlayer = player
    }

    fun findSongPositionInPlaylist(song: Song): Int {
        val position = playlist.indexOfFirst { it.id == song.id }
        return position
    }

    fun syncPositionBySong() {
        val currentSong = _currentSong.value
        if (currentSong != null) {
            val position = findSongPositionInPlaylist(currentSong)
            if (position >= 0) {
                currentSongPosition = position
            }
        }
    }

    fun hasNext(): Boolean = currentSongPosition < playlist.size - 1

    fun hasPrevious(): Boolean = currentSongPosition > 0

    fun hasNextSong(): Boolean = currentSongPosition < playlist.size - 1

    fun hasPreviousSong(): Boolean = currentSongPosition > 0

    fun startMusicService(context: Context, song: Song) {
        val serviceIntent = Intent(context, MusicForegroundService::class.java)
        serviceIntent.putExtra("song_title", song.title)
        serviceIntent.putExtra("artist_name", song.artist.name)
        serviceIntent.putExtra("image_url", song.imageUrl)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun updateMusicService(context: Context, song: Song) {
        val updateIntent = Intent(context, MusicForegroundService::class.java)
        updateIntent.action = MusicForegroundService.ACTION_UPDATE_SONG
        updateIntent.putExtra("song_title", song.title)
        updateIntent.putExtra("artist_name", song.artist.name)
        updateIntent.putExtra("image_url", song.imageUrl)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(updateIntent)
        } else {
            context.startService(updateIntent)
        }
    }

    fun updatePlayStateToService(context: Context, isPlaying: Boolean) {
        val updateIntent = Intent(context, MusicForegroundService::class.java)
        updateIntent.action = MusicForegroundService.ACTION_UPDATE_PLAY_STATE
        updateIntent.putExtra("is_playing", isPlaying)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(updateIntent)
        } else {
            context.startService(updateIntent)
        }
    }

    fun stopMusicService(context: Context) {
        val stopIntent = Intent(context, MusicForegroundService::class.java)
        context.stopService(stopIntent)
    }

    fun clear() {
        mediaPlayer?.release()
        mediaPlayer = null
        _currentSong.value = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _duration.value = 0
        playlist.clear()
        currentSongPosition = 0
    }
}