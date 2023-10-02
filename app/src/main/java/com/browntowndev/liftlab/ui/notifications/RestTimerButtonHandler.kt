package com.browntowndev.liftlab.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.browntowndev.liftlab.core.common.executeInCoroutineScope
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase


class RestTimerButtonHandler: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(Log.DEBUG.toString(), "ButtonHandler.onReceive()")
        when (intent?.action) {
            RestTimerNotificationService.SKIP_ACTION -> {
                executeInCoroutineScope {
                    LiftLabDatabase.getInstance(context!!).restTimerInProgressDao().deleteAll()

                    val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
                    context.stopService(restTimerIntent)
                }
            }
        }
    }
}