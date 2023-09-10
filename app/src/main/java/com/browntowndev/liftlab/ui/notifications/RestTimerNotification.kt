package com.browntowndev.liftlab.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.browntowndev.liftlab.MainActivity
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.LiftLabTimer
import com.browntowndev.liftlab.core.common.toTimeString

class RestTimerNotificationService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "RestTimerForegroundService"
        private const val CHANNEL_NAME = "RestTimerForegroundService"
        const val SKIP_ACTION = "Skip"
        const val EXTRA_COUNT_DOWN_FROM = "com.browntowndev.liftlab.countDownFrom"
    }

    private var _countDownTimer: LiftLabTimer? = null
    private val _notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @OptIn(ExperimentalFoundationApi::class)
    private val _notificationBuilder: NotificationCompat.Builder by lazy {
        val returnToWorkoutPendingIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val skipButtonPendingIntent = Intent(this, RestTimerButtonHandler::class.java).apply {
            action = SKIP_ACTION
        }.let {
            PendingIntent.getBroadcast(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.lab_flask)
            .setContentTitle("Rest Timer")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .addAction(R.drawable.skip_icon, "Skip", skipButtonPendingIntent)
            .setContentIntent(returnToWorkoutPendingIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(Log.DEBUG.toString(), "onBind()")
        return null
    }

    override fun onCreate() {
        Log.d(Log.DEBUG.toString(), "onCreate()")
        super.onCreate()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, _notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(Log.DEBUG.toString(), "onStartCommand()")

        val countDownFrom = intent?.getLongExtra(EXTRA_COUNT_DOWN_FROM, 0L) ?: 0L
        _countDownTimer = object : LiftLabTimer(
            countDown = true,
            millisInFuture = countDownFrom,
            countDownInterval = 100L,
        ) {
            override fun onTick(newTimeInMillis: Long) {
                val timeRemaining = newTimeInMillis.toTimeString()
                updateTime(time = timeRemaining)
            }

            override fun onFinish() {
                onDestroy()
            }
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _countDownTimer?.cancel()
        _notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateTime(time: String) {
        _notificationBuilder.setContentText(time)
        _notificationManager.notify(NOTIFICATION_ID, _notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Rest Timer Foreground Service"
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        _notificationManager.createNotificationChannel(channel)
    }
}
