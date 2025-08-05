package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.copyGeneric
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.ui.models.TopAppBarEvent
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetLogEntryByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetCompletedWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetLogEntriesFromSetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertExistingSetResultUseCase
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
    private val upsertSetLogEntriesFromSetResultsUseCase: UpsertSetLogEntriesFromSetResultsUseCase,
    private val upsertExistingSetResultUseCase: UpsertExistingSetResultUseCase,
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
        return upsertSetLogEntriesFromSetResultsUseCase(
            workoutLogEntryId = workoutLogEntryId,
            mesoCycle = mutableWorkoutState.value.programMetadata!!.currentMesocycle,
            microCycle = mutableWorkoutState.value.programMetadata!!.currentMicrocycle,
            loggingWorkoutLifts = mutableWorkoutState.value.workout!!.lifts,
            allSetResults = _editWorkoutState.value.setResults,
            setResults = updatedResults,
        )
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        return upsertExistingSetResultUseCase(
            workoutLogEntryId = workoutLogEntryId,
            mesoCycle = mutableWorkoutState.value.programMetadata!!.currentMesocycle,
            microCycle = mutableWorkoutState.value.programMetadata!!.currentMicrocycle,
            setResult = updatedResult,
            loggingWorkoutLift = mutableWorkoutState.value.workout!!.lifts.find { it.id == updatedResult.liftId }!!,
            allSetResults = _editWorkoutState.value.setResults,
        )
    }

    override suspend fun deleteSetResult(id: Long) {
        deleteSetLogEntryByIdUseCase(id)
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
}