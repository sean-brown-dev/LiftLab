package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.isCompleteWithSameDataAs
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.utils.MyoRepContinuationResult
import com.browntowndev.liftlab.core.domain.utils.MyoRepSetGoalUtils
import com.browntowndev.liftlab.core.domain.utils.SetResultKey
import com.browntowndev.liftlab.core.domain.utils.WeightCalculationUtils
import com.browntowndev.liftlab.core.domain.utils.calculateMissedGoalResult
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hydrates the lifts in a logging workout with the latest set results.
 */
class HydrateLoggingWorkoutWithCompletedSetsUseCase {
    companion object {
        private const val TAG = "HydrateLoggingWorkoutWithCompletedSetsUseCase"
    }

    operator fun invoke(
        liftsToHydrate: List<LoggingWorkoutLift>,
        setResults: List<SetResult>,
        microCycle: Int,
        programDeloadWeek: Int,
    ): List<LoggingWorkoutLift> {
        return if (setResults.isNotEmpty() || liftsToHydrate.any { it.sets.any { set -> set.complete } }) {
            liftsToHydrate.fastMap { workoutLift ->
                workoutLift.copy(
                    sets =
                        getSetsWithUpdatedCompletionData(
                            workoutLift = workoutLift,
                            allSetResults = setResults,
                            deloadWeek = workoutLift.deloadWeek ?: programDeloadWeek,
                            microCycle = microCycle,
                        )
                )
            }
        } else liftsToHydrate
    }

    private data class SetKey(
        val setPosition: Int,
        val myoRepSetPosition: Int?,
    )

    /**
     * Hydrates the lifts in a logging workout with the latest set results.
     *
     * @param workoutLift The workout lift to hydrate.
     * @param allSetResults The list of all set results.
     * @param deloadWeek The deload week.
     * @return The hydrated workout lift.
     */
    private fun getSetsWithUpdatedCompletionData(
        workoutLift: LoggingWorkoutLift,
        allSetResults: List<SetResult>,
        deloadWeek: Int,
        microCycle: Int,
    ): List<GenericLoggingSet> {
        val setResultsByKey = allSetResults.associateBy { setResult ->
            SetResultKey(
                liftId = setResult.liftId,
                liftPosition = setResult.liftPosition,
                setPosition = setResult.setPosition,
                myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition,
            )
        }

        val previousCompletedSetsByKey = mutableMapOf<SetKey, GenericLoggingSet>()
        fun getPreviousSet(set: GenericLoggingSet): GenericLoggingSet? =
            if (set is LoggingMyoRepSet && set.myoRepSetPosition != null) {
                // Mini sets share the same position as the activation set.
                val previousMyoRepSetPosition = if (set.myoRepSetPosition == 0) null else set.myoRepSetPosition - 1
                val key = SetKey(
                    setPosition = set.position,
                    myoRepSetPosition = previousMyoRepSetPosition,
                )
                previousCompletedSetsByKey[key]
            } else {
                val key = SetKey(
                    setPosition = set.position - 1,
                    myoRepSetPosition = null
                )
                previousCompletedSetsByKey[key]
            }

        val increment = workoutLift.incrementOverride
            ?: SettingsManager.getSetting(
                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
            )

        val updatedSets = workoutLift.sets.fastMap { set ->
            val currSetKey = SetResultKey(
                liftId = workoutLift.liftId,
                liftPosition = workoutLift.position,
                setPosition = set.position,
                myoRepSetPosition = (set as? LoggingMyoRepSet)?.myoRepSetPosition,
            )
            val completedSetResult = setResultsByKey[currSetKey]
            val previousSet = getPreviousSet(set)

            val updatedSet = when {
                // Case 1: A result exists for this set
                completedSetResult != null -> {
                    // If set has new data, update it, otherwise return the same set
                    if (!set.isCompleteWithSameDataAs(completedSetResult)) {
                        set.copyCompletionData(
                            complete = true,
                            completedWeight = completedSetResult.weight,
                            completedReps = completedSetResult.reps,
                            completedRpe = completedSetResult.rpe,
                        )
                    } else set
                }

                // Case 2: No set result, but it's currently marked as complete.
                // This means it needs to be "un-completed".
                set.complete ->
                    set.copyCompletionData(
                        complete = false,
                        completedWeight = null,
                        completedReps = null,
                        completedRpe = null,
                    )

                // Case 3: No result, not complete, and previous is complete, update weight recommendation (conditionally).
                previousSet != null &&
                (
                    (set is LoggingMyoRepSet && previousSet is LoggingMyoRepSet) ||
                    (set is LoggingStandardSet && previousSet is LoggingStandardSet) ||
                    (set is LoggingDropSet && (previousSet is LoggingStandardSet || previousSet is LoggingDropSet))
                ) -> {
                    getWithWeightRecommendation(
                        set = set,
                        lastCompletedSet = previousSet,
                        increment = increment,
                        setCount = workoutLift.setCount
                    )
                }

                // Case 4: None of the above apply, return the set as-is.
                else -> set
            }

            if (updatedSet.complete) {
                val setKey = SetKey(
                    setPosition = updatedSet.position,
                    myoRepSetPosition = (updatedSet as? LoggingMyoRepSet)?.myoRepSetPosition,
                )
                previousCompletedSetsByKey[setKey] = updatedSet
            }
            updatedSet
        }

        // Add new myo reps if needed
        return updatedSets.toMutableList().apply {
            addMyoRepSequence(
                liftId = workoutLift.liftId,
                liftPosition = workoutLift.position,
                isDeloadWeek = (microCycle - 1) == deloadWeek,
                incrementOverride = workoutLift.incrementOverride?: SettingsManager.getSetting(
                    SettingsManager.SettingNames.INCREMENT_AMOUNT,
                    SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT,
                ),
                allSetResults = allSetResults,
            )
        }
    }

    /**
     * Gets the set with the updated weight recommendation.
     *
     * @param increment The weight increment.
     * @param set The set.
     * @param lastCompletedSet The last completed set.
     * @return The set with the updated weight recommendation.
     */
    private fun getWithWeightRecommendation(
        set: GenericLoggingSet,
        lastCompletedSet: GenericLoggingSet,
        increment: Float,
        setCount: Int,
    ): GenericLoggingSet {
        if (!lastCompletedSet.complete || lastCompletedSet.completedWeight == null || lastCompletedSet.completedReps == null || lastCompletedSet.completedRpe == null) {
            Log.e(TAG, "lastCompletedSet is incomplete")
            return set
        }

        return when (set) {
            is LoggingDropSet -> {
                if (lastCompletedSet !is LoggingStandardSet && lastCompletedSet !is LoggingDropSet) {
                    Log.e(TAG, "lastCompletedSet is not a LoggingStandardSet or LoggingDropSet")
                    return set
                }

                val weightRecommendation = (lastCompletedSet.completedWeight!! * (1 - set.dropPercentage))
                    .roundToNearestFactor(increment)
                set.copy(weightRecommendation = weightRecommendation)
            }

            is LoggingStandardSet -> {
                if (lastCompletedSet !is LoggingStandardSet) {
                    // Don't calculate weight recommendations on a drop set or from a myo-rep set
                    Log.e(TAG, "lastCompletedSet is not a LoggingStandardSet")
                    return set
                }

                val sameRepRangeAsPrevious = lastCompletedSet.repRangeTop == set.repRangeTop &&
                        lastCompletedSet.repRangeBottom == set.repRangeBottom

                val setsFromBottom = (setCount - 1) - set.position
                val repRangeToRecalculateFor =
                    if (set.repRangeTop != set.repRangeBottom) set.repRangeBottom + setsFromBottom
                    else set.repRangeBottom
                
                val weightRecommendation = when {

                    // Same rep range as previous, see if it missed its goals, and if so recalculate.
                    // Otherwise, prefer current recommendation
                    sameRepRangeAsPrevious && set.weightRecommendation != null -> {
                        val result = calculateMissedGoalResult(
                            completedReps = lastCompletedSet.completedReps!!,
                            completedRpe = lastCompletedSet.completedRpe!!,
                            repRangeTop = lastCompletedSet.repRangeTop,
                            repRangeBottom = lastCompletedSet.repRangeBottom,
                            rpeTarget = lastCompletedSet.rpeTarget,
                            repRangeBottomFatigueOffset = 0f,
                        )
                        if (result.exceededRepRangeTop || result.missedRepRangeBottom) {
                            WeightCalculationUtils.calculateSuggestedWeight(
                                completedWeight = lastCompletedSet.completedWeight!!,
                                completedReps = lastCompletedSet.completedReps - 1,
                                completedRpe = lastCompletedSet.completedRpe,
                                repGoal = repRangeToRecalculateFor,
                                rpeGoal = set.rpeTarget,
                                roundingFactor = increment,
                            )
                        } else set.weightRecommendation
                    }

                    // No weight recommendation, just calculate from previous set
                    set.weightRecommendation == null -> WeightCalculationUtils.calculateSuggestedWeight(
                        completedWeight = lastCompletedSet.completedWeight!!,
                        completedReps = lastCompletedSet.completedReps!! - 1,
                        completedRpe = lastCompletedSet.completedRpe!!,
                        repGoal = repRangeToRecalculateFor,
                        rpeGoal = set.rpeTarget,
                        roundingFactor = increment,
                    )

                    // Different rep range. See if previous set's completion data
                    // results in +/- 5 different weight recommendation than the current one
                    else -> {
                        val prevSetRecommendation = WeightCalculationUtils.calculateSuggestedWeight(
                            completedWeight = lastCompletedSet.completedWeight!!,
                            completedReps = lastCompletedSet.completedReps!!,
                            completedRpe = lastCompletedSet.completedRpe!!,
                            repGoal = repRangeToRecalculateFor,
                            rpeGoal = set.rpeTarget,
                            roundingFactor = increment,
                        )
                        if (abs(prevSetRecommendation - set.weightRecommendation) >= 5) {
                            WeightCalculationUtils.calculateSuggestedWeight(
                                completedWeight = lastCompletedSet.completedWeight,
                                completedReps = lastCompletedSet.completedReps - 1,
                                completedRpe = lastCompletedSet.completedRpe,
                                repGoal = repRangeToRecalculateFor,
                                rpeGoal = set.rpeTarget,
                                roundingFactor = increment,
                            )
                        } else set.weightRecommendation
                    }
                }

                set.copy(weightRecommendation = weightRecommendation)
            }

            is LoggingMyoRepSet -> {
                if (set.myoRepSetPosition == null) {
                    // Activation set. Just do what is already recommended. Calculating from previous set is likely meaningless
                    // since activation sets are such high reps.
                    set
                } else if (set.myoRepSetPosition == 0) {
                    // First mini-set (first set after activation). See if the activation set was successful, and if not adjust the weight recommendation.
                    if (lastCompletedSet !is LoggingMyoRepSet) {
                        Log.e(TAG, "lastCompletedSet is not a LoggingMyoRepSet")
                        return set
                    }
                    if (lastCompletedSet.myoRepSetPosition != null) {
                        Log.e(TAG, "lastCompletedSet is not the activation set")
                        return set
                    }
                    if (lastCompletedSet.repRangeTop == null || lastCompletedSet.repRangeBottom == null) {
                        Log.e(TAG, "lastCompletedSet (activation set) is missing rep range")
                        return set
                    }

                    val missedGoalResult = calculateMissedGoalResult(
                        completedReps = lastCompletedSet.completedReps!!,
                        completedRpe = lastCompletedSet.completedRpe!!,
                        repRangeTop = lastCompletedSet.repRangeTop,
                        repRangeBottom = lastCompletedSet.repRangeBottom,
                        rpeTarget = lastCompletedSet.rpeTarget
                    )
                    val exceededRepRangeTop = missedGoalResult.exceededRepRangeTop
                    val missedRepRangeBottom = missedGoalResult.missedRepRangeBottom
                    val shouldRecalculate = exceededRepRangeTop || missedRepRangeBottom || set.weightRecommendation == null

                    val weightRecommendation = if (shouldRecalculate) {
                        // Guess ~30% drop from activation reps. Set minimum to rep floor or 10 (sane goal)
                        val repGoal = (lastCompletedSet.completedReps * .3f).roundToInt().coerceIn(
                            minimumValue = set.repFloor ?: 10,
                            maximumValue = null,
                        )
                        WeightCalculationUtils.calculateSuggestedWeight(
                            completedWeight = lastCompletedSet.completedWeight!!,
                            completedReps = lastCompletedSet.completedReps,
                            completedRpe = lastCompletedSet.completedRpe,
                            repGoal = repGoal,
                            rpeGoal = set.rpeTarget,
                            roundingFactor = increment,
                        )
                    } else set.weightRecommendation

                    set.copy(weightRecommendation = weightRecommendation)
                } else {
                    // For all mini-sets after the first one, just do what the previous mini-set did.
                    set.copy(weightRecommendation = lastCompletedSet.completedWeight)
                }
            }

            else -> throw Exception("${set::class.simpleName} is not defined.")
        }
    }

    /**
     * Key of a specific myo rep set within a lift.
     */
    private data class MyoKey(val setPosition: Int, val myoPos: Int?)

    /**
     * Adds the myo rep sequence(s) to the list of sets.
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
        val myoRepSetResults = allSetResults
            .filterIsInstance<MyoRepSetResult>()
            .fastFilter { it.liftId == liftId &&  it.liftPosition == liftPosition }
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