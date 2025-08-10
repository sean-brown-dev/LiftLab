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
import androidx.core.app.ServiceCompat
import com.browntowndev.liftlab.MainActivity
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.MAX_TIME_IN_WHOLE_MILLISECONDS
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.ui.utils.ActiveWorkoutNotification.RETURN_TO_WORKOUT_REQUEST_CODE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class ActiveWorkoutNotificationService : Service() {
    companion object {
        const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "ActiveWorkoutForegroundService"
        private const val CHANNEL_NAME = "ActiveWorkoutForegroundService"
    }

    private var nextSet: String = ""
    private val durationTimer: LiftLabTimer by inject(named("DurationTimer"))
    private val notificationHelper: NotificationHelper by inject()
    private val coroutineScope by inject<CoroutineScope>(named("ForegroundServiceScope"))
    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    @OptIn(ExperimentalFoundationApi::class)
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        val returnToWorkoutPendingIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.let {
            PendingIntent.getActivity(this, RETURN_TO_WORKOUT_REQUEST_CODE, it, PendingIntent.FLAG_IMMUTABLE)
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
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notificationBuilder.build(),
                FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(Log.DEBUG.toString(), "onStartCommand()")
        SettingsManager.initialize((this as Context).applicationContext)

        coroutineScope.launch {
            val activeWorkoutMetadata = notificationHelper.getActiveWorkoutMetadata()
            if (activeWorkoutMetadata == null) {
                stopSelf()
                return@launch
            }

            notificationBuilder.setContentTitle(activeWorkoutMetadata.workoutName)
            notificationBuilder.setContentText(activeWorkoutMetadata.nextSet)
            nextSet = activeWorkoutMetadata.nextSet // Used to update the notification on each tick
            val startDuration = getCurrentDate().time - activeWorkoutMetadata.startTime.time

            // CountDownTimer has to run on the main thread
            withContext(Dispatchers.Main.immediate) {
                durationTimer.start(
                    countDown = false,
                    millisInFuture = MAX_TIME_IN_WHOLE_MILLISECONDS,
                    countDownInterval = 1000L,
                    onTick = { newMs ->
                        val newDuration = startDuration + newMs
                        updateTime(newDuration.toTimeString())
                    },
                    onFinish = { stopSelf() }
                )
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        durationTimer.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        coroutineScope.cancel()
    }

    private fun updateTime(time: String) {
        notificationBuilder.setSubText(time)
        notificationBuilder.setContentText(nextSet)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Active WorkoutEntity Foreground Service"
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }
}
