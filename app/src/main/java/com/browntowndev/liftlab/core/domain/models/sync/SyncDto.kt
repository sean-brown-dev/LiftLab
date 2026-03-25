package com.browntowndev.liftlab.core.domain.models.sync

import java.util.Date

interface SyncDto {
    var remoteId: String?
    var lastUpdated: Date?
    var synced: Boolean
    var deleted: Boolean
    fun copyWithBase(): SyncDto
}
