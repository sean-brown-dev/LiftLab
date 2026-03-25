package com.browntowndev.liftlab.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

class NetworkMonitor(
    private val context: Context,
    appScope: CoroutineScope,
) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnlineFlow: StateFlow<Boolean> =
        callbackFlow {
            fun now(): Boolean {
                val n = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(n) ?: return false
                // INTERNET + VALIDATED means it really has internet access
                return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }

            // Emit initial state
            trySend(now())

            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(now())
                }

                override fun onLost(network: Network) {
                    trySend(now())
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    trySend(now())
                }
            }

            cm.registerDefaultNetworkCallback(cb)

            awaitClose {
                try {
                    cm.unregisterNetworkCallback(cb)
                } catch (_: Exception) { /* ignore */ }
            }
        }.distinctUntilChanged().stateIn(appScope, SharingStarted.Eagerly, false)
}
