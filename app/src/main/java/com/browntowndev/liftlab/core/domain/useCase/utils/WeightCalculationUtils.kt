package com.browntowndev.liftlab.core.domain.useCase.utils

import android.util.Log
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.roundDownToNearestFactor
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.common.toFloorAndCeiling
import kotlin.math.pow
import kotlin.math.roundToInt

class WeightCalculationUtils {
    companion object {
        private val OneRepPercentagesNSCA = mapOf(
            1 to 1.00f,
            2 to 0.95f,
            3 to 0.93f,
            4 to 0.90f,
            5 to 0.87f,
            6 to 0.85f,
            7 to 0.83f,
            8 to 0.80f,
            9 to 0.77f,
            10 to 0.75f
        )

        private fun getOneRepMax(weight: Float, reps: Float): Int {
            // Helper formulas
            fun brzycki(weight: Float, reps: Float) = weight * (36.0f / (37.0f - reps))
            fun epley(weight: Float, reps: Float) = weight * (1.0f + reps / 30.0f)
            fun baechle(weight: Float, reps: Float) = weight / (1.0278f - 0.0278f * reps)
            fun lander(weight: Float, reps: Float) = weight / (1.013f - 0.0267123f * reps)

            // Use table if integer in 1–10
            if (reps.isWholeNumber() && OneRepPercentagesNSCA.containsKey(reps.roundToInt())) {
                return (weight / OneRepPercentagesNSCA[reps.roundToInt()]!!).roundToInt()
            }

            // Otherwise, use best formulas by rep range (use float reps)
            val formulas = when {
                reps <= 5f -> listOf(
                    brzycki(weight, reps),
                    epley(weight, reps),
                    baechle(weight, reps)
                )
                reps <= 10f -> listOf(
                    brzycki(weight, reps),
                    baechle(weight, reps),
                    lander(weight, reps)
                )
                reps <= 15f -> listOf(
                    brzycki(weight, reps),
                    baechle(weight, reps)
                )
                else -> listOf(
                    brzycki(weight, reps)
                )
            }
            val oneRepMax = formulas.average().toFloat()

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
            Log.d("WeightCalculationUtils", "completedRepsConsideringRpe: $completedRepsConsideringRpe")
            val oneRepMax = getOneRepMax(completedWeight, completedRepsConsideringRpe)
            Log.d("WeightCalculationUtils", "oneRepMax: $oneRepMax")

            val repGoalConsideringRpe = repGoal + (10 - rpeGoal)
            Log.d("WeightCalculationUtils", "repGoalConsideringRpe: $repGoalConsideringRpe")

            // Calculate each formula result
            val brzyckiCalc = oneRepMax * ((37.0f - repGoalConsideringRpe) / 36.0f)
            val baechleCalc = oneRepMax / (1.0f + (0.033f * repGoalConsideringRpe))
            val landerCalc = oneRepMax * (1.013f - (0.0267123f * repGoalConsideringRpe))
            val epleyCalc = (oneRepMax * 30f) / (repGoalConsideringRpe + 30f)

            // Select best formulas based on repGoal
            val formulas = when {
                repGoal <= 5 -> listOf(brzyckiCalc, epleyCalc, baechleCalc)
                repGoal <= 10 -> listOf(brzyckiCalc, baechleCalc, landerCalc)
                repGoal <= 15 -> listOf(brzyckiCalc, baechleCalc)
                else -> listOf(brzyckiCalc)
            }

            // Average the selected formulas
            val averageWeightRecommendation = formulas.average().toFloat()
            Log.d("WeightCalculationUtils", "averageWeightRecommendation: $averageWeightRecommendation")
            return averageWeightRecommendation.roundDownToNearestFactor(roundingFactor)
        }
    }
}