package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.ui.viewmodels.states.FirestoreSyncState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirestoreSyncViewModel(
    private val syncManager: FirestoreSyncManager,
): ViewModel() {
    private val _syncState = MutableStateFlow(FirestoreSyncState())
    val syncState = _syncState.asStateFlow()

    fun syncAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    _syncState.update {
                        it.copy(
                            syncing = true,
                            showSyncFailedDialog = false,
                        )
                    }
                    Log.d("FirestoreSyncViewModel", "Syncing all")
                    retryWithBackoff {
                        syncManager.syncAll()
                    }
                    Log.d("FirestoreSyncViewModel", "Upsert complete")

                    Log.d("FirestoreSyncViewModel", "Starting deletion watchers")
                    tryStartDeletionWatchers()
                    Log.d("FirestoreSyncViewModel", "Deletion watchers started")
                } catch (e: Exception) {
                    toggleSyncErrorDialog()
                } finally {
                    _syncState.update {
                        it.copy(syncing = false)
                    }
                }
            }
        }
    }

    suspend fun tryStartDeletionWatchers() {
        try {
            retryWithBackoff {
                syncManager.tryStartDeletionWatchers()
            }
        } catch (e: Exception) {
            // Don't necessarily think user needs to know this failed to start. So just log.
            Log.e("FirestoreSyncViewModel", "Failed to start deletion watchers: ${e.message}")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    suspend fun retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        block: suspend () -> Unit
    ) {
        var attempt = 0
        var delayTime = initialDelay
        while (attempt < maxRetries) {
            try {
                block()
                return // success
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                delay(delayTime)
                delayTime *= 2
                attempt++
            }
        }

        throw Exception("Max retries reached")
    }

    fun toggleSyncErrorDialog() {
        _syncState.update {
            it.copy(showSyncFailedDialog = !it.showSyncFailedDialog)
        }
    }
}