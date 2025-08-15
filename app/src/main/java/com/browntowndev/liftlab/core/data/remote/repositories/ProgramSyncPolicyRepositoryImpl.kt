package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramSyncPolicyRepository

class ProgramSyncPolicyRepositoryImpl(
    private val programsDao: ProgramsDao,
): ProgramSyncPolicyRepository {
    override suspend fun getManyByRemoteId(remoteIds: List<String>): List<Program> {
        return programsDao.getManyByRemoteId(remoteIds).fastMap { it.toDomainModel() }
    }
}