package com.example.mediatemplate

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

import com.example.mediatemplate.Constants.CHANNEL_ID
import com.example.mediatemplate.Constants.NOTIFICATION_ID
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.AssetDataSource


class MyService : Service() {


    private lateinit var playerNotificationManager: PlayerNotificationManager
    private var isUnbound: Boolean = false
    private val TAG = Constants.TAG

    private lateinit var exoPlayer: SimpleExoPlayer
    private var mediaList: List<String>? = null

    inner class AudioServiceBinder : Binder() {
        val service get() = this@MyService
        val exoPlayer get() = this@MyService.exoPlayer
    }

    override fun onCreate() {
        super.onCreate()

        exoPlayer = SimpleExoPlayer.Builder(applicationContext).build()
        mediaList = loadNamesFromAssets()
        feedExoPlayerWithFullPlayList()
        playerNotificationManager = PlayerNotificationManager.Builder(
            applicationContext,
            NOTIFICATION_ID,
            CHANNEL_ID,
        )
            .setChannelNameResourceId(R.string.channel_name)
            .setChannelDescriptionResourceId(R.string.channel_description)

            .setMediaDescriptionAdapter(descriptionAdapter)
            .setNotificationListener(notificationListener)
            .build().also { it.setPlayer(exoPlayer)}
        Log.w(TAG, "onCreate: exoplayer hash = ${exoPlayer.hashCode()}")


    }

    override fun onBind(intent: Intent): IBinder {
        Log.w(TAG, "onBind: ")
        isUnbound = false
        return AudioServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.w(TAG, "onUnbind: ")
        isUnbound = true
        // return true to make sure that service can be rebind again somehow from the client(activity) side
        return true
    }

    override fun onRebind(intent: Intent?) {
        isUnbound = false
        Log.w(TAG, "on*************Rebind: ")
        super.onRebind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w(TAG, "onTaskRemoved: *rootIntent* $rootIntent")
        super.onTaskRemoved(rootIntent)
    }

    override fun onLowMemory() {
        Log.w(TAG, "onLowMemory: ")
        super.onLowMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.w(TAG, "onStartCommand: exoplayer  hashcode  .. ${exoPlayer.hashCode()}")
        Log.w(TAG, "onStartCommand: intent .. $intent")
        Log.w(TAG, "onStartCommand: flags .. $flags")
        Log.w(TAG, "onStartCommand: startId .. $startId")
        return START_STICKY
    }


    @MainThread
    private fun getBitmapFromVectorDrawable(context: Context, @DrawableRes drawableId: Int)
            : Bitmap? {
        return ContextCompat.getDrawable(context, drawableId)?.let {

            val drawable = DrawableCompat.wrap(it).mutate()

            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            bitmap
        }
    }

    private val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            val window = player.currentWindowIndex
            return mediaList?.get(window) as CharSequence
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

        override fun getCurrentContentText(player: Player): CharSequence? = null

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? = getBitmapFromVectorDrawable(applicationContext, R.drawable.default_img)


    }

    private val notificationListener = object : PlayerNotificationManager.NotificationListener {
//
//        override fun onNotificationStarted(notificationId: Int, notification: Notification) {
//            Log.w(TAG, "onNotificationStarted: ")
//            startForeground(notificationId, notification)
//        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            Log.w(TAG, "onNotificationCancelled:")
            // if isUnBounded =true that means the client(activity) is destroyed
            if (isUnbound) {
                stopSelf()
                Log.w(TAG, "**stopSelf**")
            }
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        notificationId,
                        notification,
                        FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(notificationId, notification)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    stopForeground(false)
                }
            }
        }
    }

//
//    @Throws(IOException::class)
//    private fun extractAssetsToFile(context: Context, fileName: String): File =
//        File(context.filesDir, fileName)
//            .also {
//                if (!it.exists()) {
//                    it.outputStream().use { cache ->
//                        context.assets.open(fileName).use { inputStream ->
//                            inputStream.copyTo(cache)
//                        }
//                    }
//                }
//            }


    private fun loadNamesFromAssets() =
        assets.list("")?.filter { it.contains(".mp3") or (it.contains(".mp4")) }


    private fun feedExoPlayerWithFullPlayList() = mediaList?.forEach { name ->
        addAssetToPlayList(name)
    }

    private fun addAssetToPlayList(assetName: String, folderName: String = "") {
        val path = "assets:///$assetName"
        val videoSource = ProgressiveMediaSource.Factory { AssetDataSource(applicationContext) }
            .createMediaSource(MediaItem.fromUri(Uri.parse(path)))
        exoPlayer.addMediaSource(videoSource)
        Log.i(TAG, "addAssetToPlayer: path = $path")
    }

    override fun onDestroy() {
        Log.w(TAG, "onDestroy: ")
        playerNotificationManager.setPlayer(null)
        exoPlayer.release()
        super.onDestroy()


    }


}