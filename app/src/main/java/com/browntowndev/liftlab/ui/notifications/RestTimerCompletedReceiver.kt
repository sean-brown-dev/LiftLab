package com.browntowndev.liftlab.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.browntowndev.liftlab.core.common.executeInCoroutineScope
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import org.koin.core.context.GlobalContext.get

class RestTimerCompletedReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "com.browntowndev.liftlab.action.REST_TIMER_COMPLETED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RestTimerCompletedReceiver", "onReceive() action=${intent.action}")
        if (intent.action != ACTION) return

        executeInCoroutineScope {
            val repo: RestTimerInProgressRepository = get().get()
            repo.delete()
            Log.d("RestTimerCompletedReceiver", "Rest timers all deleted")

        }
    }
}