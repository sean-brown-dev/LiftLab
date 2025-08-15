package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.common.roundToOneDecimal
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.isCompleteWithSameDataAs
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.useCase.utils.MyoRepContinuationResult
import com.browntowndev.liftlab.core.domain.useCase.utils.MyoRepSetGoalUtils
import com.browntowndev.liftlab.core.domain.useCase.utils.SetResultKey
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils

/**
 * Hydrates the lifts in a logging workout with the latest set results.
 */
class HydrateLoggingWorkoutWithCompletedSetsUseCase {
    companion object {
        private const val TAG = "HydrateLoggingWorkoutWithCompletedSetsUseCase"
        private const val SET_TOO_EASY_REPS_THRESHOLD = 3f
    }

    operator fun invoke(
        liftsToHydrate: List<LoggingWorkoutLift>,
        setResults: List<SetResult>,
        microCycle: Int,
    ): List<LoggingWorkoutLift> {
        return if (setResults.isNotEmpty() || liftsToHydrate.any { it.sets.any { set -> set.complete } }) {
            liftsToHydrate.fastMap { workoutLift ->
                workoutLift.copy(
                    sets =
                        getSetsWithUpdatedCompletionData(
                            workoutLift = workoutLift,
                            allSetResults = setResults,
                            isDeloadWeek = (microCycle + 1) == workoutLift.deloadWeek,
                        )
                )
            }
        } else liftsToHydrate
    }

    private fun getSetsWithUpdatedCompletionData(
        workoutLift: LoggingWorkoutLift,
        allSetResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val setResultsByKey = allSetResults.associateBy { setResult ->
            val myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition
            SetResultKey(
                liftId = setResult.liftId,
                liftPosition = setResult.liftPosition,
                setPosition = setResult.setPosition,
                myoRepSetPosition = myoRepSetPosition,
            )
        }

        var lastCompletedStandardSet: GenericLoggingSet? = null
        val updatedSets = workoutLift.sets.fastMap { set ->
            val currSetKey = SetResultKey(
                liftId = workoutLift.liftId,
                liftPosition = workoutLift.position,
                setPosition = set.position,
                myoRepSetPosition = (set as? LoggingMyoRepSet)?.myoRepSetPosition,
            )
            val completedSetResult = setResultsByKey[currSetKey]

            val updatedSet = when {
                // Case 1: A result exists for this set.
                completedSetResult != null -> {
                    // Only copy if data is actually different.
                    if (set.isCompleteWithSameDataAs(completedSetResult)) {
                        set // Return the original object
                    } else {
                        // Data is new or different, so we must copy.
                        set.copyCompletionData(
                            complete = true,
                            completedWeight = completedSetResult.weight,
                            completedReps = completedSetResult.reps,
                            completedRpe = completedSetResult.rpe,
                        )
                    }
                }

                // Case 2: No set result for non-activation-myo-rep set, but it's currently marked as complete.
                // This means it needs to be "un-completed".
                set.complete -> {
                    set.copyCompletionData(
                        complete = false,
                        completedWeight = set.completedWeight, // Keep old data, more convenient
                        completedReps = set.completedReps,
                        completedRpe = set.completedRpe,
                    )
                }

                // Case 3: No result, not complete, and previous is complete, update weight recommendation
                set !is LoggingMyoRepSet && lastCompletedStandardSet?.position == set.position - 1 -> {
                    getWithWeightRecommendation(
                        workoutLift = workoutLift,
                        set = set,
                        lastCompletedStandardSet = lastCompletedStandardSet
                    )
                }

                // Case 4: None of the above apply, return the set as-is.
                else -> set
            }

            // Update the last completed set for the next iteration.
            lastCompletedStandardSet = if (updatedSet.complete && (updatedSet is LoggingStandardSet ||
                        (updatedSet is LoggingMyoRepSet && updatedSet.myoRepSetPosition == null))) {
                updatedSet
            } else {
                lastCompletedStandardSet
            }

            updatedSet
        }

        // Add new myo reps if needed
        return updatedSets.toMutableList().apply {
            addMyoRepSequence(
                liftId = workoutLift.liftId,
                liftPosition = workoutLift.position,
                isDeloadWeek = isDeloadWeek,
                incrementOverride = workoutLift.incrementOverride?: SettingsManager.getSetting(
                    SettingsManager.SettingNames.INCREMENT_AMOUNT,
                    SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT,
                ),
                allSetResults = allSetResults,
            )
        }
    }

    private fun getWithWeightRecommendation(
        workoutLift: LoggingWorkoutLift,
        set: GenericLoggingSet,
        lastCompletedStandardSet: GenericLoggingSet,
    ): GenericLoggingSet = when (set) {
        is LoggingDropSet -> {
            val increment = workoutLift.incrementOverride
                ?: SettingsManager.getSetting(
                    SettingsManager.SettingNames.INCREMENT_AMOUNT,
                    SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
                )

            val weightRecommendation = (lastCompletedStandardSet.completedWeight!! * (1 - set.dropPercentage))
                .roundToNearestFactor(increment)
            set.copy(weightRecommendation = weightRecommendation)
        }

        is LoggingStandardSet -> {
            val rpeAdjustedCompletedReps = lastCompletedStandardSet.completedReps!! + (10f - lastCompletedStandardSet.completedRpe!!)
            val rpeAdjustedRepRangeTop = set.repRangeTop + (10f - set.rpeTarget)
            val rpeAdjustedRepRangeBottom = set.repRangeBottom + (10f - set.rpeTarget)

            val exceededRepRangeTop = rpeAdjustedCompletedReps >= (rpeAdjustedRepRangeTop + SET_TOO_EASY_REPS_THRESHOLD)
            val missedRepRangeBottom = rpeAdjustedCompletedReps < rpeAdjustedRepRangeBottom

            val lastGoalDiffered =
                lastCompletedStandardSet.repRangeBottom != set.repRangeBottom ||
                lastCompletedStandardSet.rpeTarget.roundToOneDecimal() != set.rpeTarget.roundToOneDecimal()

            val shouldRecalculate = exceededRepRangeTop || missedRepRangeBottom || lastGoalDiffered

            val weightRecommendation = if (shouldRecalculate) {
                WeightCalculationUtils.calculateSuggestedWeight(
                    completedWeight = lastCompletedStandardSet.completedWeight!!,
                    completedReps = lastCompletedStandardSet.completedReps!!,
                    completedRpe = lastCompletedStandardSet.completedRpe!!,
                    repGoal = if (exceededRepRangeTop) set.repRangeBottom else set.repRangeTop,
                    rpeGoal = set.rpeTarget,
                    roundingFactor = workoutLift.incrementOverride ?: SettingsManager.getSetting(
                        SettingsManager.SettingNames.INCREMENT_AMOUNT,
                        SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT,
                    ),
                )
            } else set.weightRecommendation ?: lastCompletedStandardSet.completedWeight
            set.copy(weightRecommendation = weightRecommendation)
        }
        else -> throw Exception("${set::class.simpleName} is not defined.")
    }

    /**
     * Key of a specific myo rep set within a lift.
     */
    private data class MyoKey(val setPosition: Int, val myoPos: Int?)

    /**
     * Adds the myo rep sequence(s) to the list of sets. The list of sets
     * only contains the activation sets since myo reps are dynamically created upon completion.
     *
     * @param liftId The lift id.
     * @param liftPosition The lift position.
     * @param isDeloadWeek Whether the deload week is currently active.
     * @param incrementOverride The increment override to use when calculating the weight recommendation.
     * @param allSetResults The list of all set results.
     * @return The list of sets with the myo rep sequence(s) added.
     */
    private fun MutableList<GenericLoggingSet>.addMyoRepSequence(
        liftId: Long,
        liftPosition: Int,
        isDeloadWeek: Boolean,
        incrementOverride: Float,
        allSetResults: List<SetResult>,
    ) {
        val myoRepSetResults = allSetResults.filterIsInstance<MyoRepSetResult>()
        if (myoRepSetResults.isEmpty()) return

        val myoRepSets = mutableListOf<LoggingMyoRepSet>()
        this.fastForEachIndexed { index, set ->
            if (set is LoggingMyoRepSet) {
                val nonNewMyoSet = set.copy(isNew = false)
                myoRepSets.add(nonNewMyoSet)
                set(index, nonNewMyoSet)
            }
        }
        if (myoRepSets.isEmpty()) return

        val activationMyoRepSetsByPosition = myoRepSets
            .fastFilter { it.myoRepSetPosition == null }
            .associateBy { it.position }

        val liveMyoRepSetsByPosition = myoRepSets.groupBy { it.position }.toMutableMap()
        fun addNewMyoRepSetToSetsByPositionMap(set: LoggingMyoRepSet) {
            val currentSetsAtPosition = liveMyoRepSetsByPosition[set.position]?.toMutableList() ?: mutableListOf()
            currentSetsAtPosition.add(set)
            liveMyoRepSetsByPosition[set.position] = currentSetsAtPosition
        }

        val existingMyoRepSetsByKey = myoRepSets
            .associateBy { MyoKey(it.position, it.myoRepSetPosition) }

        var previousMyoRepSet: LoggingMyoRepSet? = null
        myoRepSetResults
            .fastFilter { it.liftId == liftId &&  it.liftPosition == liftPosition }
            .sortedWith(compareBy({ it.setPosition }, { it.myoRepSetPosition }))
            .fastDistinctBy { it.setPosition to it.myoRepSetPosition }
            .fastForEach { setResult ->
                // Get the last myo rep set for the sequence
                val lastMyoRepSetForCurrentSequence = when {
                    // LastMyoRepSet is null, we are on the first set of the first sequence
                    previousMyoRepSet == null -> activationMyoRepSetsByPosition[setResult.setPosition] ?: error("No activation myo rep set found")

                    // Last myo rep set still in same sequence, use it
                    previousMyoRepSet.position == setResult.setPosition -> previousMyoRepSet

                    // We have a new sequence, get the activation set for it
                    else -> activationMyoRepSetsByPosition[setResult.setPosition] ?: error("No activation myo rep set found")
                }

                // See if this is a new sequence so we can add a new myo rep set for the previous sequence if goals were completed
                val newSequence = previousMyoRepSet != null && lastMyoRepSetForCurrentSequence.position != previousMyoRepSet.position
                if (newSequence) {
                    val myoRepSetResult = MyoRepSetGoalUtils.shouldContinueMyoReps(
                        lastMyoRepSet = previousMyoRepSet,
                        myoRepSetResults = liveMyoRepSetsByPosition[previousMyoRepSet.position] ?:
                            error("No myo rep sets found at position: ${previousMyoRepSet.position}"),
                    )
                    addMyoRepSet(
                        lastMyoRepSet = previousMyoRepSet,
                        isDeloadWeek = isDeloadWeek,
                        incrementOverride = incrementOverride,
                        result = myoRepSetResult,
                    )?.let { newSet ->
                        addNewMyoRepSetToSetsByPositionMap(newSet)
                    }
                }

                val existingSet = existingMyoRepSetsByKey[MyoKey(setResult.setPosition, setResult.myoRepSetPosition)]
                if (existingSet == null) {
                    previousMyoRepSet = addMyoRepSet(
                        lastMyoRepSet = lastMyoRepSetForCurrentSequence,
                        setResult = setResult,
                        isDeloadWeek = isDeloadWeek,
                    )
                    addNewMyoRepSetToSetsByPositionMap(previousMyoRepSet)
                } else {
                    previousMyoRepSet = existingSet
                }
            }

        // Loop will not check the very last sequence's tail to see if a new set needs added,
        // since it only does so on sequence change, so do it here.
        previousMyoRepSet?.let { last ->
            val decision = MyoRepSetGoalUtils.shouldContinueMyoReps(
                lastMyoRepSet = last,
                myoRepSetResults = liveMyoRepSetsByPosition[last.position] ?:
                    error("No myo rep sets found at position: ${last.position}")
            )
            addMyoRepSet(
                lastMyoRepSet = last,
                isDeloadWeek = isDeloadWeek,
                incrementOverride = incrementOverride,
                result = decision
            )
        }
    }

    /**
     * Conditionally adds a new myo rep set to the list of sets based on [MyoRepContinuationResult] and returns the set that was added.
     *
     * @param lastMyoRepSet The last myo rep set in the sequence.
     * @param isDeloadWeek Whether the deload week is currently active.
     * @param incrementOverride The increment override to use when calculating the weight recommendation.
     * @param result The added myo rep set.
     */
    private fun MutableList<GenericLoggingSet>.addMyoRepSet(
        lastMyoRepSet: LoggingMyoRepSet,
        isDeloadWeek: Boolean,
        incrementOverride: Float,
        result: MyoRepContinuationResult,
    ): LoggingMyoRepSet? {
        if (!result.shouldContinueMyoReps) return null

        val lastMyoRepSetIncomplete = lastMyoRepSet.completedWeight == null || lastMyoRepSet.completedReps == null || lastMyoRepSet.completedRpe == null

        val weightRecommendation = if (!result.activationSetMissedGoal || lastMyoRepSetIncomplete) {
            lastMyoRepSet.completedWeight
        } else WeightCalculationUtils.calculateSuggestedWeight(
            completedWeight = lastMyoRepSet.completedWeight,
            completedReps = lastMyoRepSet.completedReps,
            completedRpe = lastMyoRepSet.completedRpe,
            repGoal = lastMyoRepSet.repRangeBottom!!,
            rpeGoal = lastMyoRepSet.rpeTarget,
            roundingFactor = incrementOverride,
        )

        val newMyoRepSet = lastMyoRepSet.copy(
            myoRepSetPosition = (lastMyoRepSet.myoRepSetPosition ?: -1) + 1,
            repRangePlaceholder = if (!isDeloadWeek && lastMyoRepSet.repFloor != null) {
                ">${lastMyoRepSet.repFloor}"
            } else if (!isDeloadWeek) {
                "—"
            } else {
                lastMyoRepSet.repRangeBottom.toString()
            },
            weightRecommendation = weightRecommendation,
            complete = false,
            completedWeight = null,
            completedReps = null,
            completedRpe = null,
            isNew = true,
        )
        Log.d(TAG, "newMyoRepSet: $newMyoRepSet")
        add(
            index = this.indexOf(lastMyoRepSet) + 1,
            element = newMyoRepSet
        )

        return newMyoRepSet
    }

    /**
     * Adds a new myo rep set to the list of sets and returns the set that was added.
     *
     * @param lastMyoRepSet The last myo rep set in the sequence.
     * @param setResult The result of the set.
     * @param isDeloadWeek Whether the deload week is currently active.
     */
    private fun MutableList<GenericLoggingSet>.addMyoRepSet(
        lastMyoRepSet: LoggingMyoRepSet,
        setResult: SetResult,
        isDeloadWeek: Boolean,
    ): LoggingMyoRepSet {
        val newMyoRepSet = lastMyoRepSet.copy(
            myoRepSetPosition = (lastMyoRepSet.myoRepSetPosition ?: -1) + 1,
            repRangePlaceholder = if (!isDeloadWeek && lastMyoRepSet.repFloor != null) {
                ">${lastMyoRepSet.repFloor}"
            } else if (!isDeloadWeek) {
                "—"
            } else {
                lastMyoRepSet.repRangeBottom.toString()
            },
            weightRecommendation = lastMyoRepSet.completedWeight,
            complete = true,
            completedWeight = setResult.weight,
            completedReps = setResult.reps,
            completedRpe = setResult.rpe,
            isNew = false,
        )
        Log.d(TAG, "newMyoRepSet: $newMyoRepSet")
        add(
            index = this.indexOf(lastMyoRepSet) + 1,
            element = newMyoRepSet
        )

        return newMyoRepSet
    }
}