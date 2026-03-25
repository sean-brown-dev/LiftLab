package com.browntowndev.liftlab.ui.infrastructure

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.browntowndev.liftlab.core.data.local.maintenance.DatabaseMaintenance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class WalCaretaker(
    private val maintenance: DatabaseMaintenance,
    private val appScope: CoroutineScope
) : DefaultLifecycleObserver {

    // simple throttle so we don’t checkpoint repeatedly if stop/start bounce
    @OptIn(ExperimentalAtomicApi::class)
    private val lastRunMillis = AtomicLong(0)
    private val minIntervalMillis = 30_000L

    @OptIn(ExperimentalAtomicApi::class)
    override fun onStop(owner: LifecycleOwner) {
        val now = System.currentTimeMillis()
        if (now - lastRunMillis.load() < minIntervalMillis) return
        lastRunMillis.store(now)

        appScope.launch {
            runCatching {
                maintenance.checkpointTruncate()
                Log.d("WalCaretaker", "Checkpoint successful")
            }.onFailure {
                Log.w("WalCaretaker", "Checkpoint failed; will try later", it)
            }
        }
    }
}