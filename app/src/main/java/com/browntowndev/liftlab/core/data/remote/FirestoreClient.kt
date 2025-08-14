package com.browntowndev.liftlab.core.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.Flow

interface FirestoreClient {
    val isUserLoggedIn: Boolean
    val isUserLoggedInFlow: Flow<Boolean>
    fun batch(): WriteBatch
    fun userCollection(collectionName: String): CollectionReference
}