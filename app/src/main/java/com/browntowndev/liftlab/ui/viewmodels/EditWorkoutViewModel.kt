package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.copyGeneric
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.ui.models.TopAppBarEvent
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.extensions.toSetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetLogEntryByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetCompletedWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertManySetLogEntriesUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetLogEntryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetResultUseCase
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.EditWorkoutState
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class EditWorkoutViewModel(
    private val workoutLogEntryId: Long,
    private val upsertSetResultUseCase: UpsertSetResultUseCase,
    private val upsertManySetLogEntriesUseCase: UpsertManySetLogEntriesUseCase,
    private val upsertSetLogEntryUseCase: UpsertSetLogEntryUseCase,
    private val deleteSetLogEntryByIdUseCase: DeleteSetLogEntryByIdUseCase,
    private val onNavigateBack: () -> Unit,
    getCompletedWorkoutStateFlowUseCase: GetCompletedWorkoutStateFlowUseCase,
    undoSetCompletionUseCase: UndoSetCompletionUseCase,
    completeSetUseCase: CompleteSetUseCase,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    completeSetUseCase = completeSetUseCase,
    undoSetCompletionUseCase = undoSetCompletionUseCase,
    eventBus = eventBus,
) {
    private val _editWorkoutState = MutableStateFlow(EditWorkoutState())
    val editWorkoutState = _editWorkoutState.asStateFlow()

    private val _liftsById by lazy {
        mutableWorkoutState.value.workout!!.lifts.associateBy { it.liftId }
    }

    private val _setsByPosition by lazy {
        mutableWorkoutState.value.workout!!.lifts
            .associate { lift ->
                lift.position to
                        lift.sets.associateBy { set -> "${set.position}-${(set as? LoggingMyoRepSet)?.myoRepSetPosition}" }
            }
    }

    private val _setResultsByPosition by lazy {
        _editWorkoutState.value.setResults
            .associateBy { setResult ->
                "${setResult.liftPosition}-${setResult.setPosition}"
            }
    }

    init {
        getCompletedWorkoutStateFlowUseCase(workoutLogEntryId = workoutLogEntryId)
            .map { completedWorkoutState ->
                EditWorkoutState(
                    duration = completedWorkoutState.duration ?: "00:00:00",
                    setResults = completedWorkoutState.inProgressSetResults ?: emptyList(),
                ) to
                WorkoutState(
                    workout = completedWorkoutState.workout,
                    programMetadata = completedWorkoutState.programMetadata,
                    completedSets = completedWorkoutState.completedSetsFromLog,
                )
            }.onEach { (editWorkoutState, workoutState) ->
                _editWorkoutState.update { editWorkoutState }
                mutableWorkoutState.update { currentState ->
                    currentState.copy(
                        workout = workoutState.workout,
                        programMetadata = workoutState.programMetadata,
                        completedSets = workoutState.completedSets,
                        inProgressWorkout = WorkoutInProgressUiModel(
                            startTime = getCurrentDate(),
                        ),
                    )
                }
            }.catch {
                FirebaseCrashlytics.getInstance().recordException(it)
                Log.e("EditWorkoutViewModel", "Error getting completed workout state flow", it)
                emitUserMessage("Failed to load workout")
            }.launchIn(viewModelScope)
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> viewModelScope.launch {
                updateLinearProgressionFailures()
                onNavigateBack()
            }
            else -> {}
        }
    }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> {
        if (_editWorkoutState.value.setResults.isNotEmpty()) {
            updatedResults.fastMap { setResult ->
                updateSetResult(updatedResult = setResult)
            }
        }

        return upsertManySetLogEntriesUseCase(
            updatedResults.fastMap { setResult ->
                getSetLogEntryFromSetResult(setResult = setResult)
            }
        )
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        if (_editWorkoutState.value.setResults.isNotEmpty()) {
            updateSetResult(updatedResult = updatedResult)
        }
        return upsertSetLogEntryUseCase(
            getSetLogEntryFromSetResult(setResult = updatedResult),
        )
    }

    override suspend fun deleteSetResult(id: Long) {
        deleteSetLogEntryByIdUseCase(id)
    }

    private suspend fun updateSetResult(updatedResult: SetResult) {
        // The id on updatedResult is for setLogEntry so you can't just call upsert
        val resultToUpsert = _setResultsByPosition["${updatedResult.liftPosition}-${updatedResult.setPosition}"]
            ?.let { prevSetResult ->
                when (prevSetResult) {
                    is StandardSetResult -> prevSetResult.copy(
                        reps = updatedResult.reps,
                        weight = updatedResult.weight,
                        rpe = updatedResult.rpe,
                    )

                    is MyoRepSetResult -> prevSetResult.copy(
                        reps = updatedResult.reps,
                        weight = updatedResult.weight,
                        rpe = updatedResult.rpe,
                    )

                    is LinearProgressionSetResult -> prevSetResult.copy(
                        reps = updatedResult.reps,
                        weight = updatedResult.weight,
                        rpe = updatedResult.rpe,
                    )

                    else -> throw Exception("${prevSetResult::class.simpleName} is not defined.")
                }
            } ?: updatedResult

        upsertSetResultUseCase(resultToUpsert)
    }

    public override suspend fun updateLinearProgressionFailures() = executeWithErrorHandling("Failed to update linear progression failures.") {
        super.updateLinearProgressionFailures()
    }

    fun addSet(workoutLiftId: Long) = executeWithErrorHandling("Failed to add set.") {
        val workoutLift = mutableWorkoutState.value.workout!!.lifts.find { it.id == workoutLiftId }!!
        val lastSet = workoutLift.sets.last()
        val newSet = lastSet.copyGeneric(
            position = lastSet.position + 1,
            myoRepSetPosition = (lastSet as? LoggingMyoRepSet)?.myoRepSetPosition?.let {
                it + 1
            },
        )

        completeSet(
            restTime = 0L, restTimerEnabled = false, onBuildSetResult = {
                buildSetResult(
                    setType = when (newSet) {
                        is LoggingStandardSet -> SetType.STANDARD
                        is LoggingDropSet -> SetType.DROP_SET
                        is LoggingMyoRepSet -> SetType.MYOREP
                        else -> throw Exception("${newSet::class.simpleName} is not defined")
                    },
                    liftId = workoutLift.liftId,
                    progressionScheme = workoutLift.progressionScheme,
                    liftPosition = workoutLift.position,
                    setPosition = newSet.position,
                    myoRepSetPosition = (newSet as? LoggingMyoRepSet)?.myoRepSetPosition,
                    weight = newSet.completedWeight ?: 0f,
                    reps = newSet.completedReps ?: 0,
                    rpe = newSet.completedRpe ?: 8f,
                )
            }
        )
    }

    private fun getSet(liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?): GenericLoggingSet {
        val setsForLift = _setsByPosition[liftPosition]!!
        return setsForLift["$setPosition-$myoRepSetPosition"]!!
    }

    private fun getSetLogEntryFromSetResult(setResult: SetResult): SetLogEntry {
        val lift = _liftsById[setResult.liftId]!!
        val set = getSet(
            liftPosition = setResult.liftPosition,
            setPosition = setResult.setPosition,
            myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition
        )

        return setResult.toSetLogEntry(
            liftName = lift.liftName,
            liftMovementPattern = lift.liftMovementPattern,
            progressionScheme = lift.progressionScheme,
            workoutLogEntryId = workoutLogEntryId,
            repRangeTop = set.repRangeTop,
            repRangeBottom = set.repRangeBottom,
            rpeTarget = set.rpeTarget,
            weightRecommendation = set.weightRecommendation,
            mesoCycle = mutableWorkoutState.value.programMetadata!!.currentMesocycle,
            microCycle = mutableWorkoutState.value.programMetadata!!.currentMicrocycle,
        )
    }
}