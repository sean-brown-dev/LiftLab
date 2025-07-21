package com.browntowndev.liftlab.core.persistence.firestore.documents

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
abstract class BaseFirestoreDoc {
    @DocumentId
    open var firestoreId: String? = null

    abstract var id: Long

    @ServerTimestamp
    open var lastUpdated: Date? = null

    open var synced: Boolean = false

    abstract fun copyWithBase(): BaseFirestoreDoc
}