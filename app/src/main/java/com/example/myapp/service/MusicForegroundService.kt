package com.example.myapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.myapp.R
import com.example.myapp.activity.PlaySongActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MusicForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_UPDATE_SONG = "ACTION_UPDATE_SONG"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_UPDATE_PLAY_STATE = "ACTION_UPDATE_PLAY_STATE"

    }

    private var songTitle = ""
    private var artistName = ""
    private var imageUrl: String? = null
    private var notificationBitmap: android.graphics.Bitmap? = null
    private var isPlaying = false
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        when (intent?.action) {
            ACTION_UPDATE_SONG -> {
                songTitle = intent.getStringExtra("song_title") ?: ""
                artistName = intent.getStringExtra("artist_name") ?: ""
                imageUrl = intent.getStringExtra("image_url")

                notificationBitmap = null
                updateNotification()

                if (!imageUrl.isNullOrEmpty()) {
                    loadImageAsync(imageUrl!!)
                }
            }
            ACTION_UPDATE_PLAY_STATE -> {
                isPlaying = intent.getBooleanExtra("is_playing", false) == true
                updateNotification()
            }
            ACTION_PREV -> {
                handlePreviousAction()
                sendBroadcastToActivity("PREV")
            }
            ACTION_PLAY_PAUSE -> {
                handlePlayPauseAction()
                sendBroadcastToActivity("PLAY_PAUSE")
            }
            ACTION_NEXT -> {
                handleNextAction()
                sendBroadcastToActivity("NEXT")
            }
            else -> {
                songTitle = intent?.getStringExtra("song_title") ?: ""
                artistName = intent?.getStringExtra("artist_name") ?: ""
                imageUrl = intent?.getStringExtra("image_url")

                if (!imageUrl.isNullOrEmpty()) {
                    loadImageAsync(imageUrl!!)
                }
            }
        }

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun sendBroadcastToActivity(action: String) {
        val broadcastIntent = Intent("com.example.myapp.MUSIC_CONTROL")
        broadcastIntent.setPackage(packageName)
        val mappedAction = when(action) {
            "PREV" -> "previous"
            "PLAY_PAUSE" -> "play_pause"
            "NEXT" -> "next"
            else -> action.lowercase()
        }
        broadcastIntent.putExtra("action", mappedAction)
        sendBroadcast(broadcastIntent)
    }

    private fun loadImageAsync(url: String) {
        serviceScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection()
                    connection.doInput = true
                    connection.connect()
                    android.graphics.BitmapFactory.decodeStream(connection.getInputStream())
                }
                notificationBitmap = bitmap
                updateNotification()
            } catch (e: Exception) {
                e.printStackTrace()
                notificationBitmap = null
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val customView = RemoteViews(packageName, R.layout.notification_custom)
        customView.setTextViewText(R.id.tv_song_title, songTitle)
        customView.setTextViewText(R.id.tv_artist, artistName)

        if (notificationBitmap != null) {
            customView.setImageViewBitmap(R.id.img_song, notificationBitmap)
        } else {
            customView.setImageViewResource(R.id.img_song, R.drawable.ic_launcher_foreground)
        }


        val openActivityIntent = Intent(this, PlaySongActivity::class.java)
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val openActivityPendingIntent = PendingIntent.getActivity(
            this, 0, openActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val prevIntent = Intent(this, MusicForegroundService::class.java).setAction(ACTION_PREV)
        val prevPendingIntent = PendingIntent.getService(
            this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        customView.setOnClickPendingIntent(R.id.btn_prev, prevPendingIntent)


        val playPauseIntent = Intent(this, MusicForegroundService::class.java).setAction(ACTION_PLAY_PAUSE)
        val playPausePendingIntent = PendingIntent.getService(
            this, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        customView.setOnClickPendingIntent(R.id.btn_play_pause, playPausePendingIntent)

        if (isPlaying) {
            customView.setImageViewResource(R.id.btn_play_pause, android.R.drawable.ic_media_pause)
        } else {
            customView.setImageViewResource(R.id.btn_play_pause, android.R.drawable.ic_media_play)
        }
        val nextIntent = Intent(this, MusicForegroundService::class.java).setAction(ACTION_NEXT)
        val nextPendingIntent = PendingIntent.getService(
            this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        customView.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)



        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)
            .setContentIntent(openActivityPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

    }

    private fun handleNextAction() {
        val musicManager = com.example.myapp.manager.MusicManager.getInstance()
        val currentPos = musicManager.getCurrentSongPosition()
        val playlist = musicManager.getPlaylist()
        
        if (playlist.isNotEmpty()) {
            val newPosition = if (currentPos < playlist.size - 1) {
                currentPos + 1
            } else {
                0
            }
            
            val newSong = playlist[newPosition]
            songTitle = newSong.title
            artistName = newSong.artist.name
            imageUrl = newSong.imageUrl
            
            notificationBitmap = null
            updateNotification()
            
            if (!imageUrl.isNullOrEmpty()) {
                loadImageAsync(imageUrl!!)
            }
            
            val intent = Intent("com.example.myapp.SONG_CHANGED")
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this)
                .sendBroadcast(intent)
        }
    }
    
    private fun handlePreviousAction() {
        val musicManager = com.example.myapp.manager.MusicManager.getInstance()
        val currentPos = musicManager.getCurrentSongPosition()
        val playlist = musicManager.getPlaylist()
        
        if (playlist.isNotEmpty()) {
            val newPosition = if (currentPos > 0) {
                currentPos - 1
            } else {
                playlist.size - 1
            }
            
            val newSong = playlist[newPosition]
            songTitle = newSong.title
            artistName = newSong.artist.name
            imageUrl = newSong.imageUrl
            
            notificationBitmap = null
            updateNotification()
            
            if (!imageUrl.isNullOrEmpty()) {
                loadImageAsync(imageUrl!!)
            }
            
            val intent = Intent("com.example.myapp.SONG_CHANGED")
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this)
                .sendBroadcast(intent)
        }
    }
    
    private fun handlePlayPauseAction() {
        val musicManager = com.example.myapp.manager.MusicManager.getInstance()
        isPlaying = !(musicManager.isPlaying.value ?: false)
        updateNotification()
        
        val intent = Intent("com.example.myapp.PLAY_STATE_CHANGED")
        intent.putExtra("isPlaying", isPlaying)
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }
}