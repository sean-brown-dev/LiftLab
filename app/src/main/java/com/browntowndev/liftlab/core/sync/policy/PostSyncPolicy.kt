package com.browntowndev.liftlab.core.sync.policy

interface PostSyncPolicy {
    val collectionName: String
    suspend fun apply(remoteIds: List<String> = emptyList())
}