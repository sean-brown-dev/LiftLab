package com.browntowndev.liftlab.core.data.local.maintenance

import com.browntowndev.liftlab.core.data.local.LiftLabDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomDatabaseMaintenance(
    private val db: LiftLabDatabase
) : DatabaseMaintenance {
    override suspend fun checkpointTruncate() {
        withContext(Dispatchers.IO) {
            db.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)")
                .use { /* executed */ }
        }
    }
}