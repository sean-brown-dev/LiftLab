package com.browntowndev.liftlab.ui.viewmodels.states

data class FirestoreSyncState(
    val syncing: Boolean = false,
    val showSyncFailedDialog: Boolean = false,
)
