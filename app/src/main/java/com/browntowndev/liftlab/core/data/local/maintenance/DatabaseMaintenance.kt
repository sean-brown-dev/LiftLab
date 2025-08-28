package com.browntowndev.liftlab.core.data.local.maintenance

interface DatabaseMaintenance {
    suspend fun checkpointTruncate()
}