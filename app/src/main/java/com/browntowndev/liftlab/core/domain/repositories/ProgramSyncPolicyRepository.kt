package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program

interface ProgramSyncPolicyRepository {
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<Program>
}