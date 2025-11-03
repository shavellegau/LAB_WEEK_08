package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = startForegroundNotification()
        val handlerThread = HandlerThread("SecondNotificationThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundNotification(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker done!")
            .setContentText("Second Notification countdown started...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setTicker("Running Second Notification countdown...")

        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        return PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            flag
        )
    }

    private fun createNotificationChannel(): String {
        val channelId = "002"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Second Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val service = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)
        }
        return channelId
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post {
            countDown(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return returnValue
    }

    private fun countDown(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 5 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds remaining for Second Notification")
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA8
        const val EXTRA_ID = "Id2"
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
