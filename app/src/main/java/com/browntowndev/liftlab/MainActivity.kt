package com.browntowndev.liftlab

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.lifecycleScope
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.ui.notifications.RestTimerNotificationService
import com.browntowndev.liftlab.ui.views.LiftLab
import kotlinx.coroutines.launch
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@ExperimentalFoundationApi
class MainActivity : ComponentActivity(), KoinComponent {
    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = super.getApplicationContext()
        SettingsManager.initialize(context)
        LiftLabDatabase.getInstance(context)

        setContent {
            if(LiftLabDatabase.initialized) {
                KoinAndroidContext {
                    LiftLab()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val context = super.getApplicationContext()

        lifecycleScope.launch {
            val restTimeRemaining: Long = getRestTimeRemaining()

            if (restTimeRemaining > 0L) {
                val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
                restTimerIntent.putExtra(
                    RestTimerNotificationService.EXTRA_COUNT_DOWN_FROM,
                    restTimeRemaining
                )
                context.startForegroundService(restTimerIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val context = super.getApplicationContext()

        val restTimeRepo: RestTimerInProgressRepository by inject()
        lifecycleScope.launch {
            val restTimeRemaining: Long = getRestTimeRemaining()
            if (restTimeRemaining <= 0L) {
                restTimeRepo.deleteAll()
            }

            val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
            context.stopService(restTimerIntent)
        }
    }

    private suspend fun getRestTimeRemaining(): Long {
        val restTimeRepo: RestTimerInProgressRepository by inject()
        val inProgressRestTimer = restTimeRepo.get()

        val restTimeRemaining = if (inProgressRestTimer != null) {
            val totalRestTime = inProgressRestTimer.restTime
            val timeElapsed = Utils.getCurrentDate().time - inProgressRestTimer.timeStartedInMillis
            totalRestTime - timeElapsed
        } else 0L

        Log.d(Log.DEBUG.toString(), "Rest time remaining: $restTimeRemaining")
        return restTimeRemaining
    }
}