package com.browntowndev.liftlab.core.data.remote.client

import com.browntowndev.liftlab.core.common.authStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FirestoreClientImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    appScope: CoroutineScope,
): FirestoreClient {
    override val isUserLoggedInFlow: StateFlow<Boolean> = auth
        .authStateFlow()
        .map { it != null }
        .distinctUntilChanged()
        .stateIn(
            appScope,
            SharingStarted.Eagerly,
            auth.currentUser != null
        )
    override val isUserLoggedIn: Boolean
        get() = isUserLoggedInFlow.value

    override fun batch() = firestore.batch()

    override fun userCollection(collectionName: String) =
        firestore.collection("users")
            .document(auth.uid ?: throw IllegalStateException("User not logged in"))
            .collection(collectionName)
}