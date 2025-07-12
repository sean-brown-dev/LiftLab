package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

open class BaseEntity() {
    @DocumentId
    @ColumnInfo(defaultValue = "NULL")
    open var firestoreId: String? = null

    @ServerTimestamp
    @ColumnInfo(defaultValue = "NULL")
    open var lastUpdated: Date? = null

    @ColumnInfo(defaultValue = false.toString())
    open var synced: Boolean = false
}
