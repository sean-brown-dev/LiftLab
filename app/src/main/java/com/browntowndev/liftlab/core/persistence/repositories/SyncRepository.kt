package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.SyncDao
import com.browntowndev.liftlab.core.persistence.entities.SyncMetadata
import java.util.Date


class SyncRepository(private val syncDao: SyncDao) {
    suspend fun getLastSyncDateForCollection(collectionName: String): Date? {
        return syncDao.getForCollection(collectionName)?.lastSyncTimestamp
    }

    suspend fun saveSyncDateForCollection(collectionName: String, syncDate: Date) {
        syncDao.upsert(SyncMetadata(collectionName, syncDate))
    }
}