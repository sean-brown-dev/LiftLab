package com.browntowndev.liftlab.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.browntowndev.liftlab.MainActivity
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.LiftLabTimer
import com.browntowndev.liftlab.core.common.MAX_TIME_IN_WHOLE_MILLISECONDS
import com.browntowndev.liftlab.core.common.toTimeString

class ActiveWorkoutNotificationService : Service() {
    companion object {
        const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "ActiveWorkoutForegroundService"
        private const val CHANNEL_NAME = "ActiveWorkoutForegroundService"
        const val WORKOUT_NAME_EXTRA = "com.browntowndev.liftlab.workoutname"
        const val NEXT_SET_EXTRA = "com.browntowndev.liftlab.nextset"
        const val DURATION_EXTRA = "com.browntowndev.liftlab.duration"
    }

    private var _nextSet: String = ""
    private var _durationTimer: LiftLabTimer? = null
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

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.lab_flask)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, _notificationBuilder.build(), FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, _notificationBuilder.build())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(Log.DEBUG.toString(), "onStartCommand()")

        val workoutName = intent?.getStringExtra(WORKOUT_NAME_EXTRA) ?: ""
        _notificationBuilder.setContentTitle(workoutName)

        _nextSet = intent?.getStringExtra(NEXT_SET_EXTRA) ?: ""

        val startDuration = intent?.getLongExtra(DURATION_EXTRA, 0L) ?: 0L
        _durationTimer = object : LiftLabTimer(
            countDown = false,
            millisInFuture = MAX_TIME_IN_WHOLE_MILLISECONDS,
            countDownInterval = 1000L,
        ) {
            override fun onTick(newTimeInMillis: Long) {
                val newDuration = startDuration + newTimeInMillis
                updateTime(time = newDuration.toTimeString())
            }

            override fun onFinish() {
                onDestroy()
            }
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _durationTimer?.cancel()
        _notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateTime(time: String) {
        _notificationBuilder.setSubText(time)
        _notificationBuilder.setContentText(_nextSet)
        _notificationManager.notify(NOTIFICATION_ID, _notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Active Workout Foreground Service"
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        _notificationManager.createNotificationChannel(channel)
    }
}
