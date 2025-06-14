package com.example.myapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import com.example.myapp.manager.MusicManager

class MusicControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action")
        val musicManager = MusicManager.getInstance()

        try {
            when (action) {
                "play_pause" -> {
                    musicManager.mediaPlayer?.let { player ->
                        if (musicManager.isPlaying.value == true) {
                            player.pause()
                            musicManager.setPlayingState(false)
                        } else {
                            player.start()
                            musicManager.setPlayingState(true)
                        }
                        musicManager.updatePlayStateToService(
                            context,
                            musicManager.isPlaying.value ?: false
                        )
                    }
                }

                "next" -> {
                    val currentPos = musicManager.getCurrentSongPosition()
                    val playlist = musicManager.getPlaylist()

                    if (playlist.isNotEmpty()) {
                        val newPosition = if (currentPos < playlist.size - 1) {
                            currentPos + 1
                        } else {
                            0
                        }

                        val newSong = playlist[newPosition]

                        musicManager.mediaPlayer?.let { player ->
                            player.stop()
                            player.release()
                        }

                        musicManager.updateCurrentSongPosition(newPosition)
                        musicManager.setCurrentSong(newSong)
                        musicManager.setPlayingState(false)

                        musicManager.updateMusicService(context, newSong)

                        playNewSongDirect(context, newSong)
                    }
                }

                "previous" -> {
                    val currentPos = musicManager.getCurrentSongPosition()
                    val playlist = musicManager.getPlaylist()

                    if (playlist.isNotEmpty()) {
                        val newPosition = if (currentPos > 0) {
                            currentPos - 1
                        } else {
                            playlist.size - 1
                        }

                        val newSong = playlist[newPosition]

                        musicManager.mediaPlayer?.let { player ->
                            player.stop()
                            player.release()
                        }

                        musicManager.updateCurrentSongPosition(newPosition)
                        musicManager.setCurrentSong(newSong)
                        musicManager.setPlayingState(false)

                        musicManager.updateMusicService(context, newSong)

                        playNewSongDirect(context, newSong)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playNewSongDirect(
        context: Context,
        song: com.example.myapp.process.getsong.Song
    ) {
        try {
            val musicManager = MusicManager.getInstance()

            val newMediaPlayer = MediaPlayer()

            musicManager.mediaPlayer = newMediaPlayer

            newMediaPlayer.apply {
                setDataSource(song.url)
                prepareAsync()
                setOnPreparedListener { player ->
                    player.start()

                    musicManager.setPlayingState(true)
                    musicManager.setDuration(song.duration)

                    musicManager.updatePlayStateToService(context, true)

                    val updateIntent = Intent("com.example.myapp.SONG_CHANGED")
                    androidx.localbroadcastmanager.content.LocalBroadcastManager
                        .getInstance(context)
                        .sendBroadcast(updateIntent)
                }
                setOnErrorListener { _, _, _ ->
                    false
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}