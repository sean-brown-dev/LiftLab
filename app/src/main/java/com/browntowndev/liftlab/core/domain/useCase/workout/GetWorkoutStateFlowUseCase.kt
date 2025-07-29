package com.browntowndev.liftlab.core.domain.useCase.workout

import android.util.Log
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.CalculatedWorkoutData
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.domain.useCase.utils.InProgressResultsKey
import com.browntowndev.liftlab.core.domain.useCase.workout.progression.CalculateLoggingWorkoutUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan

/**
 * Use case responsible for orchestrating all data flows to build the current
 * state of the active workout (e.g., next lifts, completed sets, personal records).
 * It encapsulates the complex reactive data pipeline.
 */
class GetWorkoutStateFlowUseCase(
    private val workoutsRepository: WorkoutsRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val liftsRepository: LiftsRepository,
    private val calculateLoggingWorkoutUseCase: CalculateLoggingWorkoutUseCase,
    private val hydrateLoggingWorkoutWithCompletedSetsUseCase: HydrateLoggingWorkoutWithCompletedSetsUseCase,
    private val hydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase: HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase,
    private val getPersonalRecordsUseCase: GetPersonalRecordsUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    // The 'invoke' operator allows calling the use case like a function: getWorkoutStateFlowUseCase(programMetadata)
    operator fun invoke(programMetadata: ActiveProgramMetadata): Flow<CalculatedWorkoutData> {

        // Settings flows that influence workout calculation
        val useAllWorkoutDataFlow = SettingsManager.getSettingFlow(
            USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
            DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
        )
        val useOnlySamePositionFlow = SettingsManager.getSettingFlow(
            ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
            DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
        )
        val useLiftSpecificDeloadingFlow = SettingsManager.getSettingFlow(
            LIFT_SPECIFIC_DELOADING,
            DEFAULT_LIFT_SPECIFIC_DELOADING
        )

        val baseCalculatedDataFlow = workoutsRepository.getByMicrocyclePositionForCalculation(
            programId = programMetadata.programId,
            microcyclePosition = programMetadata.currentMicrocyclePosition
        ).distinctUntilChanged().flatMapLatest { nullableWorkout ->
            Log.d("GetWorkoutStateFlowUseCase", "Workout: $nullableWorkout")
            if (nullableWorkout == null) {
                // If no workout is found for the current position, emit a default empty state
                flowOf(CalculatedWorkoutData())
            } else {
                // Personal records are fetched once, as they typically don't change reactively
                // based on workout progression, but rather represent historical bests.
                val personalRecords = getPersonalRecordsUseCase(
                    workoutId = nullableWorkout.id,
                    liftIds = nullableWorkout.lifts.map { it.liftId },
                    mesoCycle = programMetadata.currentMesocycle,
                    microCycle = programMetadata.currentMicrocycle,
                )

                // Flow for previous set results, potentially including all workout data
                val previousResultsFlow = useAllWorkoutDataFlow.flatMapLatest { useAllData ->
                    getSetResultsFlowInternal( // Call internal helper function
                        workout = nullableWorkout,
                        programMetadata = programMetadata,
                        useAllData = useAllData,
                    ).distinctUntilChanged()
                }

                // Flow for in-progress set results for the current workout
                val inProgressResultsFlow = setResultsRepository.getForWorkoutFlow(
                    workoutId = nullableWorkout.id,
                    mesoCycle = programMetadata.currentMesocycle,
                    microCycle = programMetadata.currentMicrocycle,
                ).distinctUntilChanged()

                // Previous results needed for display (e.g., in UI hints)
                val previousResultsForDisplay = getResultsFromAllPreviousWorkouts(
                    liftIdsToSearchFor = nullableWorkout.lifts.map { it.liftId },
                    workout = nullableWorkout,
                    existingResultsForOtherLifts = emptyList(),
                    includeDeload = true // Include deloaded results for display purposes
                )

                // Combine settings and previous results to calculate the base logging workout
                val calculatedWorkoutDataFlow = combine(
                    useOnlySamePositionFlow,
                    useLiftSpecificDeloadingFlow,
                    previousResultsFlow
                ) { onlySamePos, liftDeloadEnabled, previousResults ->
                    Log.d("GetWorkoutStateFlowUseCase", "Calculating workout")
                    calculateLoggingWorkoutUseCase(
                        workout = nullableWorkout,
                        previousSetResults = previousResults,
                        previousResultsForDisplay = previousResultsForDisplay,
                        programDeloadWeek = programMetadata.deloadWeek,
                        useLiftSpecificDeloading = liftDeloadEnabled,
                        microCycle = programMetadata.currentMicrocycle,
                        onlyUseResultsForLiftsInSamePosition = onlySamePos
                    )
                }

                combine(
                    calculatedWorkoutDataFlow,
                    inProgressResultsFlow
                ) { calculatedWorkoutData, inProgressResults ->
                    // Create a pair of our two main data sources
                    calculatedWorkoutData to inProgressResults
                }.scan(CalculatedWorkoutData(personalRecords = personalRecords)) { previousState, (newCalculatedWorkoutData, inProgressResults) ->
                    // 'scan' is our state reducer.
                    // previousState: The last CalculatedWorkoutData we emitted.
                    // newBasePlan: The latest workout plan, fresh from settings.
                    // inProgressResults: The latest list of completed sets.
                    Log.d(
                        "GetWorkoutStateFlowUseCase",
                        "Reducing state. Has new base plan? ${newCalculatedWorkoutData !== previousState.calculatedWorkoutPlan}"
                    )

                    val partiallyHydratedPlan =
                        hydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase(
                            loggingWorkout = newCalculatedWorkoutData, // Hydrate the NEW plan...
                            liftsToUpdateFrom = previousState.calculatedWorkoutPlan?.lifts
                                ?: emptyList() // ...with the previous plan
                        )

                    val liftsToHydrate = getLiftsToHydrate(previousState, inProgressResults, partiallyHydratedPlan)
                    // 4. Now, apply the fully completed sets to this partially hydrated plan.
                    val hydratedLiftsById = hydrateLoggingWorkoutWithCompletedSetsUseCase(
                        liftsToHydrate = liftsToHydrate, // Use the plan that has the UI state
                        setResults = inProgressResults,
                        microCycle = programMetadata.currentMicrocycle,
                    ).associateBy { it.id }
                    val finalPlan = partiallyHydratedPlan.copy(
                        lifts = partiallyHydratedPlan.lifts.fastMap { lift ->
                            val hydratedLift = hydratedLiftsById[lift.id]
                            hydratedLift ?: lift
                        }
                    )

                    // 5. Emit the new, final state. This becomes 'previousState' in the next run.
                    CalculatedWorkoutData(
                        completedSetsForSession = inProgressResults,
                        personalRecords = personalRecords,
                        calculatedWorkoutPlan = finalPlan
                    )
                }.distinctUntilChanged()
            }
        }

        // Apply metadata from lifts and workouts to the calculated workout data flow
        return baseCalculatedDataFlow.flatMapLatest { calculatedWorkoutData ->
            val plan = calculatedWorkoutData.calculatedWorkoutPlan
            if (plan == null) {
                flowOf(calculatedWorkoutData)
            } else {
                val workoutMetadataFlow = workoutsRepository.getMetadataFlow(plan.id).distinctUntilChanged()
                val liftIds = plan.lifts.fastMap { it.liftId }
                val liftsMetadataFlow = liftsRepository.getManyMetadataFlow(liftIds).distinctUntilChanged()

                combine(
                    workoutMetadataFlow,
                    liftsMetadataFlow
                ) { workoutMetadata, liftsMetadata ->
                    val liftsMetadataMap = liftsMetadata.associateBy { it.id }

                    val updatedLifts = plan.lifts.map { lift ->
                        liftsMetadataMap[lift.liftId]?.let { metadata ->
                            lift.copy(
                                liftName = metadata.name,
                                note = metadata.note,
                                liftMovementPattern = metadata.movementPattern
                            )
                        } ?: lift
                    }

                    val updatedPlan = plan.copy(
                        name = workoutMetadata.name,
                        lifts = updatedLifts
                    )

                    calculatedWorkoutData.copy(
                        calculatedWorkoutPlan = updatedPlan
                    )
                }
            }
        }.distinctUntilChanged()
    }

    private fun getLiftsToHydrate(
        previousState: CalculatedWorkoutData,
        inProgressResults: List<SetResult>,
        partiallyHydratedPlan: LoggingWorkout
    ): List<LoggingWorkoutLift> {
        val previousInProgressResults = previousState.completedSetsForSession
        val liftsWithChangedResults =
            inProgressResults.filter { it !in previousInProgressResults }.fastMap {
                InProgressResultsKey(
                    liftId = it.liftId,
                    liftPosition = it.liftPosition
                )
            }.toMutableList().apply {
                addAll(
                    previousInProgressResults.filter { it !in inProgressResults }.fastMap {
                        InProgressResultsKey(
                            liftId = it.liftId,
                            liftPosition = it.liftPosition
                        )
                    }
                )
            }.toSet()

        val liftsToHydrate = partiallyHydratedPlan.lifts.fastFilter { lift ->
            InProgressResultsKey(
                liftId = lift.liftId,
                liftPosition = lift.position
            ) in liftsWithChangedResults
        }
        return liftsToHydrate
    }

    /**
     * Internal helper function to get the flow of previous set results based on settings.
     * Moved from ViewModel.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getSetResultsFlowInternal(
        workout: CalculationWorkout?,
        programMetadata: ActiveProgramMetadata,
        useAllData: Boolean,
    ): Flow<List<SetResult>> {
        if (workout == null) return flowOf(emptyList())
        return setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(
            workoutId = workout.id,
            mesoCycle = programMetadata.currentMesocycle,
            microCycle = programMetadata.currentMicrocycle,
        ).mapLatest { resultsFromLastWorkout ->
            if (useAllData) {
                // Append results from all workouts if the setting is enabled
                getResultsWithAllWorkoutDataAppendedInternal(
                    workout = workout,
                    resultsFromLastWorkout = resultsFromLastWorkout
                )
            } else resultsFromLastWorkout
        }
    }

    /**
     * Internal helper function to append all relevant workout data to results if setting is enabled.
     * Moved from ViewModel.
     */
    private suspend fun getResultsWithAllWorkoutDataAppendedInternal(
        workout: CalculationWorkout,
        resultsFromLastWorkout: List<SetResult>,
    ): List<SetResult> {
        val liftIdsOfResults = resultsFromLastWorkout.map { it.liftId }.toHashSet()
        val liftIdsToSearchFor = workout.lifts
            .filter { !liftIdsOfResults.contains(it.liftId) }
            .map { workoutLift -> workoutLift.liftId }

        return getResultsFromAllPreviousWorkouts(
            workout = workout,
            liftIdsToSearchFor = liftIdsToSearchFor,
            existingResultsForOtherLifts = resultsFromLastWorkout,
            includeDeload = false, // Do not include deloaded results when calculating progressions
        )
    }

    /**
     * Internal helper function to get results from all previous workouts.
     */
    private suspend fun getResultsFromAllPreviousWorkouts(
        workout: CalculationWorkout,
        liftIdsToSearchFor: List<Long>,
        existingResultsForOtherLifts: List<SetResult>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return if (liftIdsToSearchFor.isNotEmpty()) {
            val linearProgressionLiftIds = workout.lifts
                .filter {
                    it.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION
                }.map { it.liftId }
                .toHashSet()

            existingResultsForOtherLifts.toMutableList().apply {
                val resultsFromOtherWorkouts =
                    workoutLogRepository.getMostRecentSetResultsForLiftIds(
                        liftIds = liftIdsToSearchFor,
                        linearProgressionLiftIds = linearProgressionLiftIds,
                        includeDeload = includeDeload,
                    )

                addAll(resultsFromOtherWorkouts)
            }
        } else existingResultsForOtherLifts
    }
}
