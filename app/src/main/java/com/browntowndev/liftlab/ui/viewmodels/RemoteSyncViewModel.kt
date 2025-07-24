package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.remote.SyncOrchestrator
import com.browntowndev.liftlab.ui.viewmodels.states.RemoteSyncState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemoteSyncViewModel(
    private val syncOrchestrator: SyncOrchestrator,
): ViewModel() {
    companion object {
        private const val TAG = "RemoteSyncViewModel"
    }

    private val _syncState = MutableStateFlow(RemoteSyncState())
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

                    Log.d(TAG, "Syncing all")
                    retryWithBackoff {
                        syncOrchestrator.syncFromThenToRemote()
                    }
                    Log.d(TAG, "Sync complete")
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
                Log.e(TAG, "Sync failed with exception: ${e.message}. Retrying=${attempt + 1 < maxRetries}", e)
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