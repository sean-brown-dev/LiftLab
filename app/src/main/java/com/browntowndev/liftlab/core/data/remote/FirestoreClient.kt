package com.browntowndev.liftlab.core.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.WriteBatch

interface FirestoreClient {
    val isUserLoggedIn: Boolean
    fun batch(): WriteBatch
    fun userCollection(collectionName: String): CollectionReference
}