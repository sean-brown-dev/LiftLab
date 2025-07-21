package com.browntowndev.liftlab.core.persistence.firestore.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreClientImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
): FirestoreClient {
    override val userId: String?
        get() = auth.currentUser?.uid

    override fun batch() = firestore.batch()

    override fun userCollection(collectionName: String) =
        firestore.collection("users")
            .document(userId ?: throw IllegalStateException("User not logged in"))
            .collection(collectionName)
}