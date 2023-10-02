package com.browntowndev.liftlab.ui.views.composables

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import org.greenrobot.eventbus.EventBus

@Composable
fun EventBusDisposalEffect(navHostController: NavHostController, viewModelToUnregister: ViewModel) {
    val eventBus by remember { mutableStateOf(EventBus.getDefault()) }
    DisposableEffect(key1 = navHostController.currentBackStackEntry?.id) {
        onDispose {
            if (eventBus.isRegistered(viewModelToUnregister)) {
                eventBus.unregister(viewModelToUnregister)
                Log.d(Log.DEBUG.toString(), "Unregistered view model for ${viewModelToUnregister::class.simpleName}")
            }
        }
    }
}