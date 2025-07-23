package com.browntowndev.liftlab.core.data.remote

import androidx.work.*
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import java.util.concurrent.TimeUnit

class WorkManagerSyncScheduler(
    private val workManager: WorkManager,
) : SyncScheduler {
    companion object {
        const val WORK_NAME = "LiftLabSyncWork"
    }
    override fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            // It's good practice to set a reasonable backoff policy
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelay = MIN_BACKOFF_MILLIS,
                timeUnit = TimeUnit.MILLISECONDS
            )
            .build()

        // Using enqueueUniqueWork is critical to prevent a flood of sync requests
        workManager.enqueueUniqueWork(
            uniqueWorkName = WORK_NAME, // A constant name for your sync work
            existingWorkPolicy = ExistingWorkPolicy.REPLACE, // If a sync is already queued, this new one replaces it.
            request = syncRequest
        )
    }
}