package com.browntowndev.liftlab.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.browntowndev.liftlab.core.common.executeInCoroutineScope
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import org.koin.core.Koin
import org.koin.core.context.GlobalContext.get


class RestTimerButtonHandler: BroadcastReceiver() {
    companion object {
        const val TAG = "RestTimerButtonHandler"
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
                    executeInCoroutineScope {
                        val koin: Koin = get()
                        val restTimerRepository: RestTimerInProgressRepository = koin.get()
                        val programsRepository: ProgramsRepository = koin.get()
                        val workoutsRepository: WorkoutsRepository = koin.get()
                        val workoutInProgressRepository: WorkoutInProgressRepository = koin.get()
                        val setResultsRepository: LiveWorkoutCompletedSetsRepository = koin.get()

                        restTimerRepository.deleteAll()

                        val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
                        context.stopService(restTimerIntent)

                        NotificationHelper(
                            programRepository = programsRepository,
                            workoutsRepository = workoutsRepository,
                            workoutInProgressRepository = workoutInProgressRepository,
                            setResultsRepository = setResultsRepository,
                            restTimerInProgressRepository = restTimerRepository,
                        ).startActiveWorkoutNotification(context)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }
}