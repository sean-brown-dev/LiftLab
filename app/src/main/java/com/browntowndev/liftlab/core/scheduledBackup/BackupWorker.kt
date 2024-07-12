package com.browntowndev.liftlab.core.scheduledBackup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_SCHEDULED_BACKUP_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.SCHEDULED_BACKUP_TIME
import com.browntowndev.liftlab.core.common.backupFile
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import java.time.LocalTime

class BackupWorker(context: Context, workerParameters: WorkerParameters): CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        var result: Result
        try {
            if (!LiftLabDatabase.isOpen) {
                val context = super.getApplicationContext()
                SettingsManager.initialize(context)

                LiftLabRoomBackup(
                    context = context,
                    roomDatabase = LiftLabDatabase.getInstance(context),
                    backupFile = backupFile,
                    encryptionKey = context.getString(R.string.db_encryption_key)
                ).backup()

                BackupScheduler.scheduleNew(
                    context = context,
                    runTime = LocalTime.ofNanoOfDay(SettingsManager.getSetting(
                        SCHEDULED_BACKUP_TIME, DEFAULT_SCHEDULED_BACKUP_TIME)
                    )
                )

                result = Result.success()
            } else {
                Log.d(Log.DEBUG.toString(), "Database open. Retrying.")
                result = Result.retry()
            }
        }
        catch (ex: Exception) {
            Log.e(Log.ERROR.toString(), "Scheduled backup failed: ${ex.message}")
            Log.e(Log.ERROR.toString(), ex.stackTraceToString())

            result = Result.failure()
        }

        return result
    }
}