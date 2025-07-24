package com.browntowndev.liftlab.ui.viewmodels.states

data class RemoteSyncState(
    val syncing: Boolean = true,
    val showSyncFailedDialog: Boolean = false,
)
