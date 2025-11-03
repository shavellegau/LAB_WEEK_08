package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    // Builder for the notification
    private lateinit var notificationBuilder: NotificationCompat.Builder

    // Handler to manage thread execution
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Create and start the foreground notification
        notificationBuilder = startForegroundNotification()

        // Create a background thread for service operations
        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    /**
     * Create and start the notification as a foreground service
     */
    private fun startForegroundNotification(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val builder = getNotificationBuilder(pendingIntent, channelId)

        // Start the foreground service
        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    /**
     * Creates a PendingIntent that opens MainActivity when the user taps the notification
     */
    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, flag)
    }

    /**
     * Creates a Notification Channel for API 26+ (Oreo and above)
     */
    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "Main Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, channelName, channelPriority)

            val notificationManager = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )

            notificationManager.createNotificationChannel(channel)
            return channelId
        }
        return ""
    }

    /**
     * Builds the notification itself
     */
    private fun getNotificationBuilder(
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true)
    }

    // 🔔 This callback is part of the service lifecycle
    // Called when the service is started (after startForeground() is called)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        // Get the channel ID from the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // Post the notification task to a background thread
        serviceHandler.post {
            // Perform a countdown on the notification
            countDownFromTenToZero(notificationBuilder)

            // Notify completion via LiveData
            notifyCompletion(Id)

            // Remove the foreground notification and stop the service
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return returnValue
    }

    /**
     * Updates the notification to display a countdown from 10 to 0
     */
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder
                .setContentText("$i seconds until last warning")
                .setSilent(true)
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    /**
     * Updates the LiveData with the returned channel ID through the Main Thread
     */
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        // LiveData to track completion of the service
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
