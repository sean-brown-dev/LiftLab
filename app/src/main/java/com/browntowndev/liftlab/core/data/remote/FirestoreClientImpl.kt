package com.browntowndev.liftlab.core.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreClientImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    appScope: CoroutineScope,
): FirestoreClient {
    private val _isUserLoggedIn = MutableStateFlow(auth.currentUser != null)
    override val isUserLoggedInFlow: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()
    override val isUserLoggedIn: Boolean get() = _isUserLoggedIn.value

    init {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            _isUserLoggedIn.value = (firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        // Optional: unregister when app scope ends
        appScope.coroutineContext[Job]?.invokeOnCompletion {
            auth.removeAuthStateListener(listener)
        }
    }

    override fun batch() = firestore.batch()

    override fun userCollection(collectionName: String) =
        firestore.collection("users")
            .document(auth.uid ?: throw IllegalStateException("User not logged in"))
            .collection(collectionName)
}