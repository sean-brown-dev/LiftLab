package com.browntowndev.liftlab.core.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreClientImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
): FirestoreClient {
    override val isUserLoggedIn: Boolean
        get() = auth.currentUser?.uid != null

    override fun batch() = firestore.batch()

    override fun userCollection(collectionName: String) =
        firestore.collection("users")
            .document(auth.uid ?: throw IllegalStateException("User not logged in"))
            .collection(collectionName)
}