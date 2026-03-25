package com.browntowndev.liftlab.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.browntowndev.liftlab.core.common.executeInCoroutineScope
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.core.Koin
import org.koin.core.context.GlobalContext.get

class RestTimerButtonHandler: BroadcastReceiver() {
    companion object {
        const val TAG = "com.browntownde.liftlab.action.RestTimerButtonHandler"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "Context or intent is null")
            return
        }

        try {
            Log.d(TAG, "ButtonHandler.onReceive()")
            when (intent.action) {
                RestTimerNotificationService.SKIP_ACTION -> {
                    val appContext = context.applicationContext
                    executeInCoroutineScope {
                        val koin: Koin = get()
                        val notificationHelper: NotificationHelper = koin.get()
                        val restTimerRepository: RestTimerInProgressRepository = koin.get()

                        restTimerRepository.delete()

                        val restTimerIntent = Intent(appContext, RestTimerNotificationService::class.java)
                        appContext.stopService(restTimerIntent)

                        notificationHelper.startActiveWorkoutNotification(appContext)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
}