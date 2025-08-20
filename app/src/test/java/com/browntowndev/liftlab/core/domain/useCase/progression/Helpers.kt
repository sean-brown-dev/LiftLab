package com.browntowndev.liftlab.core.domain.useCase.progression

import com.browntowndev.liftlab.core.domain.utils.WeightCalculationUtils
import io.mockk.every

data class CalcCall(
    val completedWeight: Float,
    val completedReps: Int,
    val completedRpe: Float,
    val repGoal: Int,
    val rpeGoal: Float,
    val rounding: Float
)

fun stubWeightCalcReturns(returns: List<Float>, capture: MutableList<CalcCall>) {
    var i = 0
    every {
        WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = any<Float>(),
            completedReps = any<Int>(),
            completedRpe = any<Float>(),
            repGoal = any<Int>(),
            rpeGoal = any<Float>(),
            roundingFactor = any<Float>()
        )
    } answers {
        capture += CalcCall(
            firstArg<Float>(),
            secondArg<Int>(),
            thirdArg<Float>(),
            arg<Int>(3),
            arg<Float>(4),
            lastArg<Float>()
        )
        returns[if (i < returns.size) i++ else returns.lastIndex]
    }
}
