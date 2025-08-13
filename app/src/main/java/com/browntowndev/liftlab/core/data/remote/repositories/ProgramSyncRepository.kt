package com.browntowndev.liftlab.core.data.remote.repositories

import android.util.Log
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.ProgramRemoteDto
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.reflect.KClass

class ProgramSyncRepository(
    private val programsDao: ProgramsDao,
) : BaseRemoteSyncRepository<ProgramRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.PROGRAMS_COLLECTION
    override val remoteDtoClass: KClass<ProgramRemoteDto> = ProgramRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<ProgramRemoteDto> =
        programsDao.getManyByRemoteId(remoteIds).map { it.programEntity.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<ProgramRemoteDto> =
        programsDao.getAllUnsynced().map { it.programEntity.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<ProgramRemoteDto>): List<Long> {
        val upsertedCloudEntities = programsDao.upsertMany(entities.fastMap { it.toEntity() })
            .let { upsertIds ->
                entities.zip(upsertIds).fastMap { (entity, id) ->
                    if (id == -1L) {
                        entity
                    } else {
                        entity.copy(id = id)
                    }
                }
            }

        // If this created multiple active programs, deactivate all but one
        val activePrograms = programsDao.getAllActive()
        Log.d("ProgramSyncRepository", "Active programs: $activePrograms")
        if (activePrograms.size > 1) {
            val cloudProgramToKeepActive = upsertedCloudEntities.fastFirstOrNull { it.isActive }
            val programsToDeactivate = if (cloudProgramToKeepActive == null) {
                // Multiple local programs were already active at once, or multiple cloud ones were active, log and self heal
                val logMessage = "Multiple programs were active at once. local: $activePrograms, cloud: $upsertedCloudEntities"
                FirebaseCrashlytics.getInstance().recordException(Exception(logMessage))
                Log.e("ProgramSyncRepository", logMessage)
                activePrograms.drop(1).fastMap { it.programEntity.copy(isActive = false) }
            } else {
                // Deactivate all but the active cloud entity
                activePrograms.filter { it.programEntity.id != cloudProgramToKeepActive.id }.fastMap { it.programEntity.copy(isActive = false) }
            }
            Log.d("ProgramSyncRepository", "Deactivating $programsToDeactivate")
            programsDao.updateMany(programsToDeactivate)
        }

        return upsertedCloudEntities.fastMap { it.id }
    }

    override suspend fun deleteByRemoteId(remoteId: String): Int {
        val toDelete = programsDao.getByRemoteId(remoteId) ?: return 0
        return programsDao.delete(toDelete.programEntity)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = programsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return programsDao.deleteMany(toDelete.fastMap { it.programEntity })
    }
}