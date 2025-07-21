package com.browntowndev.liftlab.core.persistence.firestore.sync

import com.browntowndev.liftlab.core.common.enums.SyncType

data class SyncQueueEntry(
    val collectionName: String,
    val roomEntityIds: List<Long>,
    val syncType: SyncType,
) {
    val sortedIds: List<Long> by lazy { roomEntityIds.sorted() }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is SyncQueueEntry) return false

        return other.collectionName == collectionName &&
                other.sortedIds == sortedIds &&
                other.syncType == syncType
    }

    override fun hashCode(): Int =
        listOf(collectionName, sortedIds, syncType).hashCode()
}
