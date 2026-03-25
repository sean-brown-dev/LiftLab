package com.browntowndev.liftlab.ui.viewmodels.remoteSync

import androidx.compose.runtime.Stable

@Stable
data class RemoteSyncState(
    val syncing: Boolean = true,
    val showSyncFailedDialog: Boolean = false,
)
