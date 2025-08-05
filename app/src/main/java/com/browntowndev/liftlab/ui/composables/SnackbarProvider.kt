package com.browntowndev.liftlab.ui.composables

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SnackbarProvider(
    snackbarHostState: SnackbarHostState,
    messages: SharedFlow<String>,
) {
    // It's generally better to use collectLatest for UI updates from a SharedFlow.
    // This ensures that if new messages arrive quickly, only the latest one is processed,
    // preventing a backlog of snackbars if they are emitted faster than they can be displayed
    // or dismissed.
    LaunchedEffect(messages, snackbarHostState) {
        messages.collectLatest { message ->
            Log.d("SnackbarProvider", "New message: $message")
            snackbarHostState.showSnackbar(
                message = message, duration = SnackbarDuration.Indefinite, withDismissAction = true)
        }
    }
}