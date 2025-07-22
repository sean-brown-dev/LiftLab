package com.browntowndev.liftlab.core.data.sync

import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto

interface RemoteDataClient {
    fun getAllSince(lastUpdated: Date): List<BaseRemoteDto>
    fun getMany(ids: List<String>)
    fun executeBatch(toDelete: List<String>, toUpsert: List<BaseRemoteDto>)
}