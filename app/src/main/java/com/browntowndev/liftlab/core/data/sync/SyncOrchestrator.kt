package com.browntowndev.liftlab.core.data.sync

import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.local.dao.SyncQueueDao

// In data/sync/SyncOrchestrator.kt
class SyncOrchestrator(
    private val syncQueueDao: SyncQueueDao,
    private val programsDao: ProgramsDao,
    private val remoteDataSource: MasterRemoteDataSource,
    private val syncPrefs: SyncPreferences
) {
    suspend fun performSync() {
        uploadPendingChanges()
        downloadNewerChanges()
    }

    private suspend fun uploadPendingChanges() {
        val pendingSyncs = syncQueueDao.getAll()
        if (pendingSyncs.isEmpty()) return

        val deletes = pendingSyncs.filter { it.syncType == SyncType.Upsert }
        val upserts = pendingSyncs.filter { it.syncType == SyncType.Delete }

        // Fetch the actual data for upserts
        val programIdsToUpsert = upserts.filter { it.collectionName == RemoteCollectionNames.PROGRAMS_COLLECTION }.map { it.entityId }
        val programEntities = programsDao.getMany(programIdsToUpsert)
        val programDtos = programEntities.map { it.toDto() } // Assumes an Entity -> DTO mapper

        // Execute the batch remotely
        remoteDataSource.executeSyncBatch(deletes, mapOf("programs" to programDtos))

        // On success, clear the queue
        syncQueueDao.deleteAll(pendingSyncs)
    }

    private suspend fun downloadNewerChanges() {
        val lastSync = syncPrefs.getLastSyncTimestamp("programs")
        val newProgramDtos = remoteDataSource.getNewPrograms(lastSync)

        if (newProgramDtos.isNotEmpty()) {
            val newEntities = newProgramDtos.map { it.toEntity() } // Assumes a DTO -> Entity mapper
            programsDao.upsertAll(newEntities)

            val latestTimestamp = newProgramDtos.maxOf { it.lastUpdated?.time ?: 0L }
            syncPrefs.setLastSyncTimestamp("programs", latestTimestamp)
        }
    }
}