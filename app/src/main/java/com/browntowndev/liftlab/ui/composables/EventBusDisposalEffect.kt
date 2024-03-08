package com.browntowndev.liftlab.ui.composables

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import org.greenrobot.eventbus.EventBus

@Composable
fun EventBusDisposalEffect(screenId: String?, viewModelToUnregister: ViewModel) {
    val eventBus by remember { mutableStateOf(EventBus.getDefault()) }
    DisposableEffect(key1 = screenId) {
        onDispose {
            if (eventBus.isRegistered(viewModelToUnregister)) {
                eventBus.unregister(viewModelToUnregister)
                Log.d(Log.DEBUG.toString(), "Unregistered view model for ${viewModelToUnregister::class.simpleName}")
            }
        }
    }
}