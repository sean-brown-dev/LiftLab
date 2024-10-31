package com.browntowndev.liftlab.core.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.generateCompleteStepSequence
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.models.ActiveWorkoutNotificationMetadata

class NotificationHelper(
    private val programRepository: ProgramsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository
) {
    companion object {
        private fun isActive(context: Context, notificationId: Int): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.activeNotifications.any { it.id == notificationId }
        }
    }

    suspend fun startActiveWorkoutNotification(context: Context) {
        if (isActive(context, ActiveWorkoutNotificationService.NOTIFICATION_ID)) return

        val activeWorkoutMetadata = getActiveWorkoutMetadata()
        if (activeWorkoutMetadata != null) {
            val activeWorkoutIntent = Intent(context, ActiveWorkoutNotificationService::class.java)
            activeWorkoutIntent.putExtra(
                ActiveWorkoutNotificationService.DURATION_EXTRA,
                getCurrentDate().time - activeWorkoutMetadata.startTime.time
            )
            activeWorkoutIntent.putExtra(
                ActiveWorkoutNotificationService.WORKOUT_NAME_EXTRA,
                activeWorkoutMetadata.workoutName
            )
            activeWorkoutIntent.putExtra(
                ActiveWorkoutNotificationService.NEXT_SET_EXTRA,
                activeWorkoutMetadata.nextSet
            )

            context.startForegroundService(activeWorkoutIntent)
        }
    }

    private suspend fun getActiveWorkoutMetadata(): ActiveWorkoutNotificationMetadata? {
        return programRepository.getActiveNotAsLiveData()?.let { activeProgramMetadata ->
            val workoutInProgress = workoutInProgressRepository.get(
                mesoCycle = activeProgramMetadata.currentMesocycle,
                microCycle = activeProgramMetadata.currentMicrocycle,
            )

            if (workoutInProgress != null) {
                val workout = workoutsRepository.get(workoutInProgress.workoutId)!!

                ActiveWorkoutNotificationMetadata(
                    workoutName = workout.name,
                    startTime = workoutInProgress.startTime,
                    nextSet = getNextSetText(
                        completedSets = workoutInProgress.completedSets,
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
        is StandardWorkoutLiftDto -> {
            val repText = if (workoutLift.stepSize != null && workoutLift.progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION) {
                val liftLevelDeloadsEnabled = SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
                val deloadWeek = if(liftLevelDeloadsEnabled && workoutLift.deloadWeek != null) workoutLift.deloadWeek!! else programDeloadWeek

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

        is CustomWorkoutLiftDto -> {
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