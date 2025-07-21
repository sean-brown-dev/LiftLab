package com.browntowndev.liftlab.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.browntowndev.liftlab.core.common.executeInCoroutineScope
import com.browntowndev.liftlab.core.domain.repositories.standard.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.standard.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.standard.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.standard.WorkoutsRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
                        val firebaseAuth: FirebaseAuth = koin.get()
                        val firestore: FirebaseFirestore = koin.get()
                        val restTimerRepository: RestTimerInProgressRepository = koin.get()
                        val programsRepository: ProgramsRepository = koin.get()
                        val workoutsRepositoryImpl: WorkoutsRepositoryImpl = koin.get()
                        val workoutInProgressRepositoryImpl: WorkoutInProgressRepositoryImpl = koin.get()

                        restTimerRepository.deleteAll()

                        val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
                        context.stopService(restTimerIntent)

                        NotificationHelper(
                            programRepository = programsRepository,
                            workoutsRepositoryImpl = workoutsRepositoryImpl,
                            workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
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