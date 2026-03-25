package com.browntowndev.liftlab.core.domain.utils

fun getRepsForMicrocycle(repRangeBottom: Int, repRangeTop: Int, microCycle: Int, deloadWeek: Int, stepSize: Int?): Int {
    return if (stepSize != null) {
        if (microCycle < deloadWeek - 1) {
            val steps = generateCompleteStepSequence(
                repRangeTop = repRangeTop,
                repRangeBottom = repRangeBottom,
                stepSize = stepSize,
                totalStepsToTake = deloadWeek - 1,
            )
            steps[microCycle]
        } else {
            repRangeBottom
        }
    } else {
        getRepsForMicrocycleWithUnevenStepSize(
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            microCycle = microCycle,
            deloadWeek = deloadWeek,
        )
    }
}

private fun getRepsForMicrocycleWithUnevenStepSize(repRangeBottom: Int, repRangeTop: Int, microCycle: Int, deloadWeek: Int): Int {
    if (repRangeTop <= repRangeBottom) {
        error("Invalid rep range. Top must be greater than bottom.")
    }

    val fullRepRange = (repRangeTop downTo repRangeBottom).toList()

    val repsToStep = fullRepRange.size - 1
    val stepSize = maxOf(1, repsToStep / (deloadWeek - 2))

    // Deload week and the week before both should end on repRangeBottom
    val index = if (microCycle < deloadWeek - 2) {
        val thisStep = microCycle * stepSize
        // Cycle back to start when this step exceeds total step size but previous
        // microcycle did not
        if (thisStep <= repsToStep) thisStep
        else if ((microCycle - 1) * stepSize < repsToStep) repsToStep
        else 0
    } else repsToStep

    return fullRepRange[index]
}