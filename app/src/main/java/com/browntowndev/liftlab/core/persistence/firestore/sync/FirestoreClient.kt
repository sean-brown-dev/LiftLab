package com.browntowndev.liftlab.core.persistence.firestore.sync

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.WriteBatch

interface FirestoreClient {
    val userId: String?
    fun batch(): WriteBatch
    fun userCollection(collectionName: String): CollectionReference
}