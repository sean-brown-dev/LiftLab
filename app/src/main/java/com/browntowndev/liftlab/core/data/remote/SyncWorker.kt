package com.browntowndev.liftlab.core.data.remote


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val syncOrchestrator: SyncOrchestrator,
) : CoroutineWorker(appContext, params) {
    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            syncOrchestrator.syncToRemote()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync.", e)
            FirebaseCrashlytics.getInstance().recordException(e)

            // Let WorkManager handle retries based on the policy
            Result.retry()
        }
    }
}