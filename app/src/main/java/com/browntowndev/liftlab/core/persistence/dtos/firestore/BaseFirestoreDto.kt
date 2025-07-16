package com.browntowndev.liftlab.core.persistence.dtos.firestore

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
abstract class BaseFirestoreDto {
    @DocumentId
    open var firestoreId: String? = null

    @ServerTimestamp
    open var lastUpdated: Date? = null

    open var synced: Boolean = false

    abstract fun copyWithBase(): BaseFirestoreDto
}