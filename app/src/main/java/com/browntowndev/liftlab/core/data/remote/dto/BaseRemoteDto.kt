package com.browntowndev.liftlab.core.data.remote.dto

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
abstract class BaseRemoteDto {
    @DocumentId
    open var remoteId: String? = null

    @ServerTimestamp
    open var lastUpdated: Date? = null

    open var synced: Boolean = false

    open var deleted: Boolean = false

    abstract fun copyWithBase(): BaseRemoteDto
}