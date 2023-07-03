package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.toFloorAndCeiling
import kotlin.math.pow

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

        private fun getOneRepMax(weight: Int, reps: Float): Float {
            var oneRepMax = 0f

            reps.toFloorAndCeiling().forEach {
                val currOneRepMax = if (reps.isWholeNumber() && rep1RMPercentages.containsKey(it)) {
                    weight / rep1RMPercentages[it]!!
                } else {
                    val dBrzyckiCalc = weight * (36.0f / (37.0f - reps))
                    val dBaechleCalc = weight * (1.0f + .033f * reps)
                    val dLanderCalc = weight / (1.013f - .0267123f * reps)
                    val dEpleyCalc = weight * (1.0f + reps / 30.0f)
                    val dLombardiCalc: Float = weight * reps.pow(.10f)
                    (dBrzyckiCalc + dBaechleCalc + dLanderCalc + dEpleyCalc + dLombardiCalc) / 5f
                }

                oneRepMax = if (oneRepMax > 0) (oneRepMax + currOneRepMax) / 2 else currOneRepMax
            }

            return oneRepMax
        }

        fun calculateSuggestedWeight (weight: Int, reps: Int, rpe: Float, roundingFactor: Float): Int {
            val repsConsideringRpe = reps + (10 - rpe)
            val oneRepMax = getOneRepMax(weight, repsConsideringRpe)

            val brzyckiCalc = oneRepMax * ((37.0f - repsConsideringRpe) / 36.0f)
            val baechleCalc = oneRepMax / (1.0f + (0.033f * repsConsideringRpe))
            val landerCalc = oneRepMax * (1.013f - (0.0267123f * repsConsideringRpe))
            val epleyCalc = (oneRepMax * 30f) / (repsConsideringRpe + 30f)
            val lombardiCalc = oneRepMax / repsConsideringRpe.pow(0.10f)

            return ((brzyckiCalc + baechleCalc + landerCalc + epleyCalc + lombardiCalc) / roundingFactor).toInt()
        }

    }
}