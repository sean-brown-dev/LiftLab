package com.browntowndev.liftlab.core.data.sync


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val syncOrchestrator: SyncOrchestrator // Injected
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            syncOrchestrator.syncAll()
            Result.success()
        } catch (e: Exception) {
            // Let WorkManager handle retries based on the policy
            Result.retry()
        }
    }
}