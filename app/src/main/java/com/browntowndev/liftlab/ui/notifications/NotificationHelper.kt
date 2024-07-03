package com.browntowndev.liftlab.ui.notifications

import android.content.Context
import android.content.Intent
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.models.ActiveWorkoutNotificationMetadata
import org.koin.core.component.inject

class NotificationHelper(
    private val programRepository: ProgramsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val workoutsRepository: WorkoutsRepository,
) {
    suspend fun startActiveWorkoutNotification(context: Context) {
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
                    )
                )
            } else null
        }
    }

    private fun getNextSetText(completedSets: List<SetResult>, lifts: List<GenericWorkoutLift>): String {
        return completedSets.maxWithOrNull(
            compareBy<SetResult> { it.liftPosition }.thenBy { it.setPosition }
        )?.let { lastCompletedSet ->
            val liftOfSet = lifts[lastCompletedSet.liftPosition]
            if (lastCompletedSet.setPosition + 1 < liftOfSet.setCount) {
                getNextSetText(workoutLift = liftOfSet, lastCompletedSetForLift = lastCompletedSet)
            } else if (liftOfSet.position + 1 < lifts.size) {
                val nextLift = lifts[liftOfSet.position + 1]
                getNextSetText(workoutLift = nextLift, lastCompletedSetForLift = null)
            } else {
                "Workout Complete!"
            }
        } ?: getNextSetText(workoutLift = lifts.first(), lastCompletedSetForLift = null)
    }

    private fun getNextSetText(
        workoutLift: GenericWorkoutLift,
        lastCompletedSetForLift: SetResult?
    ) = when (workoutLift) {
        is StandardWorkoutLiftDto -> {
            "${workoutLift.liftName}\n${workoutLift.repRangeBottom}-${workoutLift.repRangeTop} reps"
                .let { nextSet ->
                    if (lastCompletedSetForLift == null || workoutLift.progressionScheme != ProgressionScheme.WAVE_LOADING_PROGRESSION) {
                        nextSet + " @${workoutLift.rpeTarget} RPE"
                    } else nextSet
                }
        }

        is CustomWorkoutLiftDto -> {
            val nextSet = workoutLift.customLiftSets[(lastCompletedSetForLift?.setPosition ?: -1) + 1]
            "${workoutLift.liftName}\n${nextSet.repRangeBottom}-${nextSet.repRangeTop} reps " +
                    "@${nextSet.rpeTarget} RPE"
        }

        else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
    }
}