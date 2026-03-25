package com.browntowndev.liftlab.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.IBinder
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.browntowndev.liftlab.MainActivity
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.ui.utils.RestTimerNotification.RETURN_TO_WORKOUT_REQUEST_CODE
import com.browntowndev.liftlab.ui.utils.RestTimerNotification.SKIP_REST_TIMER_REQUEST_CODE
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import kotlin.math.round

class RestTimerNotificationService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1

        // Ongoing foreground notification (progress) – low importance, no sound
        private const val REST_TIMER_SERVICE_CHANNEL_ID = "RestTimerForegroundService"
        private const val PROGRESS_CHANNEL_NAME = "Rest Timer Progress"

        const val SKIP_ACTION =
            "com.browntowndev.liftlab.core.notifications.RestTimerNotificationService.SKIP_ACTION"
        const val EXTRA_COUNT_DOWN_FROM =
            "com.browntowndev.liftlab.countDownFrom"
    }

    private val timer: LiftLabTimer by inject(named("CountdownTimer"))

    private val notificationManager: NotificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    @OptIn(ExperimentalFoundationApi::class)
    private val notificationBuilder: NotificationCompat.Builder by lazy {
        val returnToWorkout = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.let {
            PendingIntent.getActivity(
                this,
                RETURN_TO_WORKOUT_REQUEST_CODE,
                it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        val skipPendingIntent = Intent(this, RestTimerButtonHandler::class.java).apply {
            action = SKIP_ACTION
        }.let {
            PendingIntent.getBroadcast(
                this,
                SKIP_REST_TIMER_REQUEST_CODE,
                it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        NotificationCompat.Builder(this, REST_TIMER_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.lab_flask)
            .setContentTitle("Rest Timer")
            .setPriority(NotificationCompat.PRIORITY_LOW) // keep progress quiet
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .addAction(R.drawable.skip_icon, "Skip", skipPendingIntent)
            .setContentIntent(returnToWorkout)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("RestTimerSvc", "onBind()")
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createRestTimerChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notificationBuilder.build(),
            FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RestTimerSvc", "onStartCommand()")

        val countDownFrom = intent?.getLongExtra(EXTRA_COUNT_DOWN_FROM, 0L) ?: 0L
        val rounded = (round(countDownFrom / 1000.0) * 1000).toLong()

        if (rounded <= 0L) {
            updateProgress("Rest complete!")
            postCompletionAlert()
            broadcastTimerCompleted()
            stopSelf()
            return START_NOT_STICKY
        }

        timer.start(
            countDown = true,
            millisInFuture = rounded,
            countDownInterval = 1000L,
            onTick = { millisecondsRemaining ->
                updateProgress(millisecondsRemaining.toTimeString())
            },
            onFinish = {
                updateProgress("Rest complete!")
                postCompletionAlert()
                broadcastTimerCompleted()
                startActiveWorkoutNotification()
                stopSelf()
            }
        )

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    @Synchronized
    private fun updateProgress(text: String) {
        notificationManager.notify(
            NOTIFICATION_ID,
            notificationBuilder.setContentText(text).build()
        )
    }

    private fun createRestTimerChannel() {
        val channel = NotificationChannel(
            REST_TIMER_SERVICE_CHANNEL_ID,
            PROGRESS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows remaining rest time while app is backgrounded"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null) // no sound for progress
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun postCompletionAlert() {
        NotificationHelper.postRestTimerCompletionAlert(context = this)
    }

    private fun broadcastTimerCompleted() {
        val intent = Intent(this, RestTimerCompletedReceiver::class.java).apply {
            action = RestTimerCompletedReceiver.ACTION
        }
        sendBroadcast(intent)
    }

    private fun startActiveWorkoutNotification() {
        val appContext = this.applicationContext
        ContextCompat.startForegroundService(
            appContext, Intent(appContext, ActiveWorkoutNotificationService::class.java)
        )
    }
}
