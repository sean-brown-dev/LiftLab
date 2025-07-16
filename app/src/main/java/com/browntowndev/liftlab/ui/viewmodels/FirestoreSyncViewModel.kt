package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.ui.viewmodels.states.FirestoreSyncState
import kotlinx.coroutines.Dispatchers
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
                    syncManager.syncAll()
                    Log.d("FirestoreSyncViewModel", "Sync complete")
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

    fun toggleSyncErrorDialog() {
        _syncState.update {
            it.copy(showSyncFailedDialog = !it.showSyncFailedDialog)
        }
    }
}