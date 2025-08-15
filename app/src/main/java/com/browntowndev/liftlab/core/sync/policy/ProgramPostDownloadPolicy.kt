package com.browntowndev.liftlab.core.sync.policy

import android.util.Log
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.repositories.ProgramSyncPolicyRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames.PROGRAMS_COLLECTION
import com.google.firebase.crashlytics.FirebaseCrashlytics

class ProgramPostDownloadPolicy(
    override val collectionName: String = PROGRAMS_COLLECTION,
    private val programsRepository: ProgramsRepository,
    private val programsSyncPolicyRepository: ProgramSyncPolicyRepository,
    private val transactionScope: TransactionScope,
) : PostSyncPolicy {
    companion object {
        private const val TAG = "ProgramPostDownloadPolicy"
    }

    override suspend fun apply(remoteIds: List<String>) = transactionScope.execute {
        // If this created multiple active programs, deactivate all but one
        val activePrograms = programsRepository.getAllActive()

        if (activePrograms.size > 1) {
            val activeCloudPrograms = programsSyncPolicyRepository.getManyByRemoteId(remoteIds).filter { it.isActive }
            if (activeCloudPrograms.size > 1) {
                Log.w(TAG, "Multiple active programs found: $activeCloudPrograms")
                FirebaseCrashlytics.getInstance().recordException(Exception("Multiple active cloud programs found: $activeCloudPrograms"))
            }

            val cloudProgramIdToKeepActive = activeCloudPrograms.firstOrNull()?.id
            Log.d(TAG, "Keep active program ID: $cloudProgramIdToKeepActive")

            val programsToDeactivate = if (cloudProgramIdToKeepActive == null) {
                Log.w(TAG, "Multiple local programs were active.")
                FirebaseCrashlytics.getInstance().recordException(Exception("Multiple active local programs found: $activePrograms"))

                // Activate newest program
                activePrograms.sortedByDescending { it.id }.drop(1).fastMap { it }
            } else {
                activePrograms.filter { it.id != cloudProgramIdToKeepActive }
            }

            programsToDeactivate.fastMap { program ->
                program.id to programDelta {
                    updateProgram(isActive = Patch.Set(false))
                }
            }.fastForEach { (programId, delta) ->
                programsRepository.applyDelta(programId, delta)
            }
        }
    }
}