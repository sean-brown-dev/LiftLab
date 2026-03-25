package com.browntowndev.liftlab.core.data.remote.client

import android.util.Log
import com.browntowndev.liftlab.core.common.authStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.tasks.await

class FirestoreClientImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    appScope: CoroutineScope,
): FirestoreClient {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val isUserLoggedInFlow: StateFlow<Boolean> =
        auth.authStateFlow()
            .map { user -> user?.uid }
            .distinctUntilChanged()
            .transformLatest { uid ->
                if (uid == null) {
                    emit(false)
                } else {
                    val currentUser = requireNotNull(auth.currentUser)
                    runCatching {
                        currentUser.reload().await()
                        currentUser.getIdToken(true).await()
                        emit(true)
                    }.onFailure { t ->
                        Log.w("Auth", "ID token refresh on login failed", t)
                        emit(false)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
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
