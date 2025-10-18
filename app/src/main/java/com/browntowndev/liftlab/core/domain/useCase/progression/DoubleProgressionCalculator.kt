package com.browntowndev.liftlab.core.domain.useCase.progression

import android.util.Log
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationCustomLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult

class DoubleProgressionCalculator: BaseWholeLiftProgressionCalculator() {
    override fun allSetsMetCriterion(
        lift: CalculationStandardWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean {
        // Dupes should be impossible, but just in case
        val distinctResults = previousSetResults.distinctBy { it.setPosition }

        // If no results, or less results than we have sets, return false
        if (distinctResults.isEmpty() || distinctResults.size < lift.setCount) return false

        // If there are missing set results (sequence not contiguous from 0 to set count - 1), return false
        // Volume cycling should not do this because prev week results will be fewer until ceiling is hit.
        if (lift.volumeCyclingSetCeiling == null) {
            val setPositions = distinctResults.fastMap { it.setPosition }.toSet()
            val liftSetPositions = (0..<lift.setCount).toSet()
            if (!setPositions.containsAll(liftSetPositions)) return false
        }

        // See if there's a first set result, if not return false
        val firstSetResult = distinctResults.minByOrNull { it.setPosition }
        if (firstSetResult == null) return false

        // See if the first set hit the top of the rep range, if it failed return early
        val firstSetPassed = firstSetResult.reps >= lift.repRangeTop
        if (!firstSetPassed) return false

        // Get the last set result, and if there isn't one, return early
        val lastSetResult = distinctResults.firstOrNull { it.setPosition == (lift.setCount - 1) }
        if (lastSetResult == null || (lastSetResult.setPosition == firstSetResult.setPosition)) return true

        // See if all the intermediate sets hit the top of the rep range
        val intermediateSetsPassed = distinctResults
            .filter { it.setPosition > firstSetResult.setPosition && it.setPosition <  lastSetResult.setPosition }
            .fastAll { result ->
                result.reps >= lift.repRangeTop
            }

        // Finally, see if the last set hit the top of the rep range
        return intermediateSetsPassed && lastSetResult.reps >= lift.repRangeTop
    }

    override fun allSetsMetCriterion(
        lift: CalculationCustomWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean {
        // Dupes should be impossible, but just in case
        val distinctResults = previousSetResults.distinctBy {
            DistinctSetKey(
                position = it.setPosition,
                myoRepSetPosition = (it as? MyoRepSetResult)?.myoRepSetPosition
            )
        }

        // Return false if no results exist or no sets exist
        if (distinctResults.isEmpty() || lift.customLiftSets.isEmpty()) return false

        // Group results by set position, and if there are missing results return
        val groupedSetData = distinctResults.sortedBy { it.setPosition }.groupBy { it.setPosition }
        val liftSetsByPosition = lift.customLiftSets.associateBy { it.position }
        if (!groupedSetData.keys.containsAll(liftSetsByPosition.keys)) return false

        // These are sorted already, but just to be safe
        val sortedSets = lift.customLiftSets.sortedBy { it.position }

        // See if the sets meet their configured goals
        val setsPassed = sortedSets.fastAll { set ->
            val setResults = groupedSetData[set.position]
            if (setResults == null || setResults.isEmpty()) {
                // Should be impossible since we checked above that all sets exist
                Log.e("DoubleProgressionCalculator", "Intermediate set results not found for position: ${set.position}")
                return false
            }
            customSetResultsPassed(
                set,
                setResults,
            )
        }

        return setsPassed
    }

    private fun customSetResultsPassed(
        set: CalculationCustomLiftSet,
        results: List<SetResult>,
        rpeTargetOverride: Float? = null,
    ): Boolean {
        return when (set) {
            is CalculationMyoRepSet -> {
                customSetMeetsCriterion(
                    set = set,
                    setData = results.filterIsInstance<MyoRepSetResult>()
                )
            }
            else -> {
                customSetMeetsCriterion(
                    set = set,
                    result = results.firstOrNull(),
                    rpeTargetOverride = rpeTargetOverride
                )
            }
        }
    }

    private data class DistinctSetKey(
        val position: Int,
        val myoRepSetPosition: Int?
    )
}