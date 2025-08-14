package com.browntowndev.liftlab.core.domain.useCase.utils

import android.util.Log
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.roundDownToNearestFactor
import com.browntowndev.liftlab.core.common.roundToOneDecimal
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt

class WeightCalculationUtils {
    companion object {
        const val PERCENTAGE_AT_ONE_REP = 1.0f // 100% of 1RM
        const val PERCENTAGE_AT_TEN_REPS = 0.75f // 75% of 1RM

        // Calculate the decay rate so the curve passes through both anchor points: ≈ -0.031948
        private val decayRatePerRep = ln(PERCENTAGE_AT_TEN_REPS / PERCENTAGE_AT_ONE_REP) / (10f - 1f)

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

        /**
         * Calculates the estimated percentage of 1RM based on an exponential endurance decay curve.
         *
         * @param repsPerformed The number of reps completed (already adjusted for RPE if needed).
         * @return The percentage of 1RM (as a decimal, e.g., 0.65 for 65% 1RM).
         */
        private fun getExponentialDecayPercentageForReps(repsPerformed: Float): Float =
            PERCENTAGE_AT_ONE_REP * exp(decayRatePerRep * (repsPerformed - 1f))

        /**
         * Calculates the estimated percentage of 1RM based on the NSCA curve.
         *
         * @param adjustedRepsRaw The number of reps completed (already adjusted for RPE if needed).
         * @return The percentage of 1RM (as a decimal, e.g., 0.65 for 65% 1RM).
         */
        private fun getNscaPercentageForAdjustedReps(adjustedRepsRaw: Float): Float? {
            // Round to 1 decimal place to eliminate epsilon noise (.0 / .5 only in your app)
            val adjustedRepsRounded = adjustedRepsRaw.roundToOneDecimal()

            // Only use NSCA interpolation if the (rounded) value is truly within 1..10
            if (adjustedRepsRounded < 1f || adjustedRepsRounded > 10f) return null

            if (adjustedRepsRounded.isWholeNumber()) {
                return OneRepPercentagesNSCA[adjustedRepsRounded.toInt()]
            }

            // Rounds to nearest whole number above and below the reps.
            // So, for 8.5 you'll get 8 and 9
            val lowerWholeReps = floor(adjustedRepsRounded).toInt()
            val upperWholeReps = ceil(adjustedRepsRounded).toInt()

            val lowerPercentage = OneRepPercentagesNSCA[lowerWholeReps]!!
            val upperPercentage = OneRepPercentagesNSCA[upperWholeReps]!!

            val fractionBetweenWholeReps = adjustedRepsRounded - lowerWholeReps // 0.0 or 0.5 for your inputs

            // Linear interpolation (midpoint when .5)
            return lowerPercentage + fractionBetweenWholeReps * (upperPercentage - lowerPercentage)
        }

        /**
         * Calculates the estimated one-repetition maximum (1RM) for a given weight and number of reps.
         *
         * @param weight The weight in kilograms.
         * @param reps The number of reps completed.
         * @return The estimated 1RM.
         */
        private fun getOneRepMax(weight: Float, reps: Float): Float {
            if (weight == 0f || reps == 0f) return 0f

            val nscaPercentage = getNscaPercentageForAdjustedReps(reps)
            val oneRepMax = if (nscaPercentage != null) {
                weight / nscaPercentage
            } else {
                weight / getExponentialDecayPercentageForReps(reps)
            }

            return oneRepMax
        }

        /**
         * Calculates the estimated one-repetition maximum (1RM) for a given weight and number of reps.
         *
         * @param weight The weight in kilograms.
         * @param reps The number of reps completed.
         * @return The estimated 1RM.
         */
        fun getOneRepMax(weight: Float, reps: Int, rpe: Float): Int {
            if (weight == 0f || reps == 0) return 0
            val repsConsideringRpe = reps + (10 - rpe)
            return getOneRepMax(weight, repsConsideringRpe).roundToInt()
        }

        /**
         * Calculates the suggested weight for a given set of completion data.
         *
         * @param completedWeight The weight completed in the previous set.
         * @param completedReps The number of reps completed in the previous set.
         * @param completedRpe The RPE completed in the previous set.
         * @param repGoal The target number of reps for the current set.
         * @param rpeGoal The target RPE for the current set.
         * @param roundingFactor The rounding factor to use when calculating the suggested weight.
         * @return The suggested weight for the current set.
         */
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
            if (oneRepMax == 0f) return 0f
            Log.d("WeightCalculationUtils", "oneRepMax: $oneRepMax")

            val repGoalConsideringRpe = repGoal + (10 - rpeGoal)
            Log.d("WeightCalculationUtils", "repGoalConsideringRpe: $repGoalConsideringRpe")

            // The percentage of the 1RM we are targeting
            val targetPercentage = getNscaPercentageForAdjustedReps(repGoalConsideringRpe) ?:
                getExponentialDecayPercentageForReps(repGoalConsideringRpe)

            // Calculate the recommendation
            val recommendation = (oneRepMax * targetPercentage).roundDownToNearestFactor(roundingFactor)
            Log.d("WeightCalculationUtils", "recommendation: $recommendation")

            return recommendation
        }
    }
}