package com.browntowndev.liftlab.core.common

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class Utils {
    companion object {
        fun percentageStringToFloat(percentageString: String): Float {
            return percentageString.removeSuffix("%").toFloat() / 100
        }

        fun getCurrentDate(): Date {
            val localDateTime = LocalDateTime.now()
            val zoneId = ZoneId.systemDefault()
            return Date.from(localDateTime.atZone(zoneId).toInstant())
        }

        fun getPossibleStepSizes(repRangeTop: Int, repRangeBottom: Int, stepCount: Int): List<Int> {
            val rangeSize = repRangeTop - repRangeBottom
            val stepSizes = mutableListOf<Int>()

            // Calculate possible step sizes
            for (i in 1..rangeSize) {
                if (rangeSize % i == 0 && (stepCount + 1) % ((rangeSize / i) + 1) == 0) {
                    stepSizes.add(i)
                }
            }

            return stepSizes
        }

        fun generateFirstCompleteStepSequence(repRangeTop: Int, repRangeBottom: Int, stepSize: Int): List<Int> {
            val steps = mutableListOf<Int>()
            val stepsToRepRangeBottom = (repRangeTop - repRangeBottom) / stepSize

            for (i in 0..stepsToRepRangeBottom) {
                val currStepSizeFromTop = i * stepSize
                steps.add(repRangeTop - currStepSizeFromTop)
            }

            return steps
        }

        fun generateCompleteStepSequence(repRangeTop: Int, repRangeBottom: Int, stepSize: Int, totalStepsToTake: Int): List<Int> {
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