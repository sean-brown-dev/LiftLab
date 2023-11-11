package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.common.toFloorAndCeiling
import kotlin.math.pow
import kotlin.math.roundToInt

class CalculationEngine {
    companion object {
        private val rep1RMPercentages = mapOf(
            1 to 1.0f,
            2 to 0.94f,
            3 to 0.91f,
            4 to 0.883f,
            5 to 0.86f,
            6 to 0.833f,
            7 to 0.805f,
            8 to 0.777f,
            9 to 0.752f,
            10 to 0.733f
        )

        private fun getOneRepMax(weight: Float, reps: Float): Int {
            val oneRepMax = if (reps.isWholeNumber() && rep1RMPercentages.containsKey(reps.roundToInt())) {
                weight / rep1RMPercentages[reps.roundToInt()]!!
            } else {
                reps.toFloorAndCeiling()
                    .map { roundedReps ->
                        if (rep1RMPercentages.containsKey(roundedReps)) {
                            weight / rep1RMPercentages[roundedReps]!!
                        } else {
                            val dBrzyckiCalc = weight * (36.0f / (37.0f - roundedReps))
                            val dBaechleCalc = weight * (1.0f + .033f * roundedReps)
                            val dLanderCalc = weight / (1.013f - .0267123f * roundedReps)
                            val dEpleyCalc = weight * (1.0f + roundedReps / 30.0f)
                            val dLombardiCalc: Float = weight * roundedReps.toFloat().pow(.10f)
                            (dBrzyckiCalc + dBaechleCalc + dLanderCalc + dEpleyCalc + dLombardiCalc) / 5f
                        }
                    }.average().toFloat()
            }

            return oneRepMax.roundToInt()
        }

        fun getOneRepMax(weight: Float, reps: Int, rpe: Float): Int {
            if (weight == 0f || reps == 0) return 0
            val repsConsideringRpe = reps + (10 - rpe)
            return getOneRepMax(weight, repsConsideringRpe)
        }

        fun calculateSuggestedWeight(
            completedWeight: Float,
            completedReps: Int,
            completedRpe: Float,
            repGoal: Int,
            rpeGoal: Float,
            roundingFactor: Float,
        ): Float {
            val completedRepsConsideringRpe = completedReps + (10 - completedRpe)
            val oneRepMax = getOneRepMax(completedWeight, completedRepsConsideringRpe)

            val repGoalConsideringRpe = repGoal + (10 - rpeGoal)
            val brzyckiCalc = oneRepMax * ((37.0f - repGoalConsideringRpe) / 36.0f)
            val baechleCalc = oneRepMax / (1.0f + (0.033f * repGoalConsideringRpe))
            val landerCalc = oneRepMax * (1.013f - (0.0267123f * repGoalConsideringRpe))
            val epleyCalc = (oneRepMax * 30f) / (repGoalConsideringRpe + 30f)
            val lombardiCalc = oneRepMax / repGoalConsideringRpe.pow(0.10f)

            val averageWeightRecommendation = (brzyckiCalc + baechleCalc + landerCalc + epleyCalc + lombardiCalc) / 5
            return averageWeightRecommendation.roundToNearestFactor(roundingFactor)
        }

    }
}