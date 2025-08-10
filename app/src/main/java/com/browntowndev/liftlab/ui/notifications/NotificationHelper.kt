package com.browntowndev.liftlab.ui.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.domain.utils.generateCompleteStepSequence
import com.browntowndev.liftlab.ui.models.workout.ActiveWorkoutNotificationMetadata
import com.browntowndev.liftlab.ui.utils.RestTimerNotification.ALERT_CHANNEL_ID
import com.browntowndev.liftlab.ui.utils.RestTimerNotification.ALERT_CHANNEL_NAME
import com.browntowndev.liftlab.ui.utils.RestTimerNotification.ALERT_NOTIFICATION_ID
import com.browntowndev.liftlab.ui.utils.RestTimerNotification.ALERT_TEXT
import com.browntowndev.liftlab.ui.utils.RestTimerNotification.ALERT_TITLE
import kotlinx.coroutines.flow.firstOrNull

class NotificationHelper(
    private val programRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val setResultsRepository: LiveWorkoutCompletedSetsRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository
) {
    companion object {
        private fun isActive(context: Context, notificationId: Int): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.activeNotifications.any { it.id == notificationId }
        }

        private fun ensureRestTimerNotificationChannelExists(packageName: String, notificationManager: NotificationManager) {
            val soundUri = "android.resource://$packageName/${R.raw.boxing_bell}".toUri()
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Plays a bell when rest is complete"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(soundUri, attrs) // sound lives on the channel (O+)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        fun postRestTimerCompletionAlert(context: Context) {
            val appContext = context.applicationContext
            val notificationManager =
                ContextCompat.getSystemService(appContext, NotificationManager::class.java) ?: return
            ensureRestTimerNotificationChannelExists(appContext.packageName, notificationManager)

            val notification = NotificationCompat.Builder(appContext, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.lab_flask)
                .setContentTitle(ALERT_TITLE)
                .setContentText(ALERT_TEXT)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
        }
    }

    suspend fun startActiveWorkoutNotification(context: Context) {
        val appContext = context.applicationContext
        Log.d("NotificationHelper", "startActiveWorkoutNotification: Checking for active notification")
        if (isActive(appContext, ActiveWorkoutNotificationService.NOTIFICATION_ID)) {
            Log.d("NotificationHelper", "startActiveWorkoutNotification: Notification already active")
            return
        }

        Log.d("NotificationHelper", "startActiveWorkoutNotification: Starting notification")
        val activeWorkoutIntent = Intent(appContext, ActiveWorkoutNotificationService::class.java)
        appContext.startForegroundService(activeWorkoutIntent)
        Log.d("NotificationHelper", "startActiveWorkoutNotification: Notification started")
    }

    suspend fun getActiveWorkoutMetadata(): ActiveWorkoutNotificationMetadata? {
        return programRepository.getActive()?.let { activeProgramMetadata ->
            val workoutInProgress = workoutInProgressRepository.getFlow(
                mesoCycle = activeProgramMetadata.currentMesocycle,
                microCycle = activeProgramMetadata.currentMicrocycle,
            ).firstOrNull()

            if (workoutInProgress != null) {
                val workout = workoutsRepository.getById(workoutInProgress.workoutId) ?: return null
                val completedSets = setResultsRepository.getAll()
                ActiveWorkoutNotificationMetadata(
                    workoutName = workout.name,
                    startTime = workoutInProgress.startTime,
                    nextSet = getNextSetText(
                        completedSets = completedSets,
                        lifts = workout.lifts,
                        programDeloadWeek = activeProgramMetadata.deloadWeek,
                        microCycle = activeProgramMetadata.currentMicrocycle,
                    )
                )
            } else null
        }
    }

    private fun getNextSetText(
        completedSets: List<SetResult>,
        lifts: List<GenericWorkoutLift>,
        programDeloadWeek: Int,
        microCycle: Int,
    ): String {
        return completedSets.maxWithOrNull(
            compareBy<SetResult> { it.liftPosition }.thenBy { it.setPosition }
        )?.let { lastCompletedSet ->
            val liftOfSet = lifts[lastCompletedSet.liftPosition]
            if (lastCompletedSet.setPosition + 1 < liftOfSet.setCount) {
                getNextSetText(workoutLift = liftOfSet, lastCompletedSetForLift = lastCompletedSet, programDeloadWeek = programDeloadWeek, microCycle = microCycle)
            } else if (liftOfSet.position + 1 < lifts.size) {
                val nextLift = lifts[liftOfSet.position + 1]
                getNextSetText(workoutLift = nextLift, lastCompletedSetForLift = null, programDeloadWeek = programDeloadWeek, microCycle = microCycle)
            } else {
                "Workout Complete!"
            }
        } ?: getNextSetText(workoutLift = lifts.first(), lastCompletedSetForLift = null, programDeloadWeek = programDeloadWeek, microCycle = microCycle)
    }

    private fun getNextSetText(
        workoutLift: GenericWorkoutLift,
        lastCompletedSetForLift: SetResult?,
        programDeloadWeek: Int,
        microCycle: Int,
    ) = when (workoutLift) {
        is StandardWorkoutLift -> {
            val repText = if (workoutLift.stepSize != null && workoutLift.progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION) {
                val liftLevelDeloadsEnabled = SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
                val deloadWeek = if(liftLevelDeloadsEnabled && workoutLift.deloadWeek != null) workoutLift.deloadWeek else programDeloadWeek

                if (microCycle < deloadWeek - 1) {
                    val stepSequence = generateCompleteStepSequence(
                        repRangeTop = workoutLift.repRangeTop,
                        repRangeBottom = workoutLift.repRangeBottom,
                        stepSize = workoutLift.stepSize,
                        totalStepsToTake = deloadWeek - 1,
                    )
                    "${stepSequence[microCycle]}"
                } else "${workoutLift.repRangeBottom}"
            } else "${workoutLift.repRangeBottom}-${workoutLift.repRangeTop}"

            "${workoutLift.liftName}\n$repText reps"
                .let { liftAndRepsText ->
                    if (lastCompletedSetForLift == null || workoutLift.progressionScheme != ProgressionScheme.WAVE_LOADING_PROGRESSION) {
                        "$liftAndRepsText @${workoutLift.rpeTarget} RPE"
                    } else liftAndRepsText
                }
        }

        is CustomWorkoutLift -> {
            val nextSet = workoutLift.customLiftSets[(lastCompletedSetForLift?.setPosition ?: -1) + 1]
            "${workoutLift.liftName}\n${nextSet.repRangeBottom}-${nextSet.repRangeTop} reps " +
                    "@${nextSet.rpeTarget} RPE"
        }

        else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
    }

    suspend fun startRestTimerNotification(context: Context): Boolean {
        val restTimeRemaining = getRestTimeRemaining()
        val restTimerIsActive = restTimeRemaining > 0L
        val notificationIsActive = isActive(context, RestTimerNotificationService.NOTIFICATION_ID)

        if (restTimerIsActive && !notificationIsActive) {
            val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
            restTimerIntent.putExtra(
                RestTimerNotificationService.EXTRA_COUNT_DOWN_FROM,
                restTimeRemaining
            )

            context.startForegroundService(restTimerIntent)
        }

        return restTimerIsActive
    }

    private suspend fun getRestTimeRemaining(): Long {
        val inProgressRestTimer = restTimerInProgressRepository.get()

        val restTimeRemaining = if (inProgressRestTimer != null) {
            val totalRestTime = inProgressRestTimer.restTime
            val timeElapsed = getCurrentDate().time - inProgressRestTimer.timeStartedInMillis
            totalRestTime - timeElapsed
        } else 0L

        return restTimeRemaining
    }

    suspend fun stopActiveNotifications(context: Context) {
        if (isActive(context, RestTimerNotificationService.NOTIFICATION_ID)) {
            if (getRestTimeRemaining() <= 0L) {
                restTimerInProgressRepository.deleteAll()
            }

            val restTimerIntent = Intent(context, RestTimerNotificationService::class.java)
            context.stopService(restTimerIntent)
        } else if (isActive(context, ActiveWorkoutNotificationService.NOTIFICATION_ID)) {
            val activeWorkoutIntent = Intent(context, ActiveWorkoutNotificationService::class.java)
            context.stopService(activeWorkoutIntent)
        }
    }
}
