package com.browntowndev.liftlab.ui.viewmodels.remoteSync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.data.remote.SyncOrchestrator
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class RemoteSyncViewModel(
    private val syncOrchestrator: SyncOrchestrator,
    private val isOnlineFlow: Flow<Boolean>,
    private val isLoggedInFlow: Flow<Boolean>,
): ViewModel() {
    companion object {
        private const val TAG = "RemoteSyncViewModel"
    }

    private val _syncState = MutableStateFlow(RemoteSyncState())
    val syncState = _syncState.asStateFlow()

    suspend fun awaitSyncReady(
        maxWaitMs: Long = 2500L, // tweak: 1500–3000ms feels snappy
    ): Boolean = withTimeoutOrNull(maxWaitMs) {
        Log.d(TAG, "Waiting until sync is ready.")
        combine(isOnlineFlow, isLoggedInFlow) { online, loggedIn ->
            online && loggedIn
        }.first { it }
    } ?: false

    suspend fun syncAllSuspending() {
        _syncState.update {
            it.copy(
                syncing = true,
                showSyncFailedDialog = false,
            )
        }

        try {
            if(!awaitSyncReady()) {
                Log.d(TAG, "Skipping sync. User is either not online or not logged in.")
                return
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

    fun syncAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                syncAllSuspending()
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