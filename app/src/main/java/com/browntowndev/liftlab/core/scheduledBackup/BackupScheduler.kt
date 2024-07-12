package com.browntowndev.liftlab.core.scheduledBackup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.browntowndev.liftlab.core.common.toSimpleDateTimeString
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class BackupScheduler {
    companion object {
        private const val WORK_NAME = "DailyBackupWork"
        fun scheduleNew(context: Context, runTime: LocalTime) {
            val currentTime = LocalTime.now()
            val delayDuration = if (runTime.isBefore(currentTime))
                ChronoUnit.MINUTES.between(currentTime, runTime) + (24 * 60)
            else
                ChronoUnit.MINUTES.between(currentTime, runTime).takeIf { it > 0L } ?: 1L

            val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                .setInitialDelay(delayDuration, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5.minutes.toJavaDuration())
                .build()

            Log.d(Log.DEBUG.toString(), "Backup scheduled to run in ~$delayDuration minutes.")
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                backupRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}