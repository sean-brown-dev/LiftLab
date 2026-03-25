package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.browntowndev.liftlab.core.domain.models.sync.SyncDto
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
abstract class BaseRemoteDto: SyncDto {
    @DocumentId
    override var remoteId: String? = null

    @ServerTimestamp
    override var lastUpdated: Date? = null

    override var synced: Boolean = false

    override var deleted: Boolean = false

    abstract override fun copyWithBase(): BaseRemoteDto
}
