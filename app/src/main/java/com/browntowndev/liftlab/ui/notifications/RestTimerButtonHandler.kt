package com.browntowndev.liftlab.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.browntowndev.liftlab.core.common.executeInCoroutineScope
import com.browntowndev.liftlab.core.persistence.LiftLabDatabase
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLiftMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RepositoryHelper
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository


class RestTimerButtonHandler: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(Log.DEBUG.toString(), "ButtonHandler.onReceive()")
        when (intent?.action) {
            RestTimerNotificationService.SKIP_ACTION -> {
                executeInCoroutineScope {
                    val repoHelper = RepositoryHelper(context!!)
                    repoHelper.restTimer.deleteAll()

                    val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
                    context.stopService(restTimerIntent)
                    
                    NotificationHelper(
                        programRepository = repoHelper.programs,
                        workoutsRepository = repoHelper.workouts,
                        workoutInProgressRepository = repoHelper.workoutInProgress,
                    ).startActiveWorkoutNotification(context)
                }
            }
        }
    }
}