package com.browntowndev.liftlab.core.common

import androidx.compose.ui.util.fastFlatMap
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.BACKUP_DIRECTORY
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_BACKUP_DIRECTORY
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class Utils {
    sealed class General {
        companion object {
            fun percentageStringToFloat(percentageString: String): Float {
                return percentageString.removeSuffix("%").toFloat() / 100
            }

            fun getCurrentDate(): Date {
                val localDateTime = LocalDateTime.now()
                val zoneId = ZoneId.systemDefault()
                return Date.from(localDateTime.atZone(zoneId).toInstant())
            }

            val backupFile: File
                get() {
                    val backupDir = SettingsManager.getSetting(BACKUP_DIRECTORY, DEFAULT_BACKUP_DIRECTORY)
                    return File(backupDir, backupFileName)
                }
        }
    }

    sealed class StepSize {
        companion object {
            fun getAllLiftsWithRecalculatedStepSize(workouts: List<WorkoutDto>, deloadToUseInsteadOfLiftLevel: Int?): Map<Long, StandardWorkoutLiftDto> {
                return workouts
                    .fastFlatMap { workout ->
                        workout.lifts
                    }
                    .filterIsInstance<StandardWorkoutLiftDto>()
                    .mapNotNull { workoutLift ->
                        getRecalculatedStepSizeForLift(
                            currStepSize = workoutLift.stepSize,
                            progressionScheme = workoutLift.progressionScheme,
                            repRangeTop = workoutLift.repRangeTop,
                            repRangeBottom = workoutLift.repRangeBottom,
                            deloadWeek = deloadToUseInsteadOfLiftLevel ?: workoutLift.deloadWeek,
                        ).let { newStepSize ->
                            if (workoutLift.stepSize != newStepSize) {
                                workoutLift.id to workoutLift.copy(stepSize = newStepSize)
                            } else null
                        }
                    }.associate { it.first to it.second }
            }

            fun getRecalculatedStepSizeForLift(
                currStepSize: Int?,
                progressionScheme: ProgressionScheme,
                repRangeTop: Int,
                repRangeBottom: Int,
                deloadWeek: Int?
            ): Int? {
                return if (progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION) {
                    getPossibleStepSizes(
                        repRangeTop = repRangeTop,
                        repRangeBottom = repRangeBottom,
                        stepCount = deloadWeek?.let { deloadWeek - 2 },
                    ).let { availableStepSizes ->
                        if (availableStepSizes.contains(currStepSize)) {
                            currStepSize
                        } else {
                            availableStepSizes.firstOrNull()
                        }
                    }
                } else null
            }

            fun getPossibleStepSizes(
                repRangeTop: Int,
                repRangeBottom: Int,
                stepCount: Int?
            ): List<Int> {
                val rangeSize = repRangeTop - repRangeBottom
                val stepSizes = mutableListOf<Int>()

                // Calculate possible step sizes
                for (i in 1..rangeSize) {
                    val canBeReachedInSteps =
                        stepCount == null || (stepCount + 1) % ((rangeSize / i) + 1) == 0
                    if (rangeSize % i == 0 && canBeReachedInSteps) {
                        stepSizes.add(i)
                    }
                }

                return stepSizes
            }

            fun generateFirstCompleteStepSequence(
                repRangeTop: Int,
                repRangeBottom: Int,
                stepSize: Int
            ): List<Int> {
                val steps = mutableListOf<Int>()
                val stepsToRepRangeBottom = (repRangeTop - repRangeBottom) / stepSize

                for (i in 0..stepsToRepRangeBottom) {
                    val currStepSizeFromTop = i * stepSize
                    steps.add(repRangeTop - currStepSizeFromTop)
                }

                return steps
            }

            fun generateCompleteStepSequence(
                repRangeTop: Int,
                repRangeBottom: Int,
                stepSize: Int,
                totalStepsToTake: Int
            ): List<Int> {
                val steps = mutableListOf<Int>()
                val stepsToRepRangeBottom = (repRangeTop - repRangeBottom) / stepSize

                for (i in 0..stepsToRepRangeBottom) {
                    val currStepSizeFromTop = i * stepSize
                    steps.add(repRangeTop - currStepSizeFromTop)
                }

                return List(size = totalStepsToTake) { steps[it % steps.size] }
            }
        }
    }
}