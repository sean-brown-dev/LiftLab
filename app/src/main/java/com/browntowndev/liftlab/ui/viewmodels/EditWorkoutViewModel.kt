package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetLogEntryByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetCompletedWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertExistingSetResultUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetLogEntriesFromSetResultsUseCase
import com.browntowndev.liftlab.ui.mapping.toUiModel
import com.browntowndev.liftlab.ui.mapping.toDomainModel
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel
import com.browntowndev.liftlab.ui.extensions.copyGeneric
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingDropSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingMyoRepSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingStandardSetUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.EditWorkoutState
import com.browntowndev.liftlab.ui.viewmodels.states.workout.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
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
            .distinctUntilChanged()
            .map { completedWorkoutState ->
                EditWorkoutState(
                    duration = completedWorkoutState.duration ?: "00:00:00",
                ) to
                WorkoutState(
                    workout = completedWorkoutState.workout?.toUiModel(),
                    programMetadata = completedWorkoutState.programMetadata?.toUiModel(),
                    completedSets = completedWorkoutState.completedSetsFromLog.fastMap { it.toUiModel() },
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
        if (mutableWorkoutState.value.programMetadata == null) throw Exception("Program metadata is null")
        if (mutableWorkoutState.value.workout == null) throw Exception("Workout is null")

        return upsertSetLogEntriesFromSetResultsUseCase(
            workoutLogEntryId = workoutLogEntryId,
            loggingWorkoutLifts = mutableWorkoutState.value.workout!!.lifts.fastMap { it.toDomainModel() },
            setResults = updatedResults,
        )
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        if (mutableWorkoutState.value.programMetadata == null) throw Exception("Program metadata is null")
        if (mutableWorkoutState.value.workout == null) throw Exception("Workout is null")
        if (updatedResult.liftPosition >= mutableWorkoutState.value.workout!!.lifts.size) throw Exception("Lift position is out of bounds")
        val loggingWorkoutLift = mutableWorkoutState.value.workout!!.lifts[updatedResult.liftPosition]

        return upsertExistingSetResultUseCase(
            workoutLogEntryId = workoutLogEntryId,
            setResult = updatedResult,
            loggingWorkoutLift = loggingWorkoutLift.toDomainModel(),
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
            myoRepSetPosition = (lastSet as? LoggingMyoRepSetUiModel)?.myoRepSetPosition?.let {
                it + 1
            },
        )

        completeSet(
            restTime = 0L, restTimerEnabled = false, onBuildSetResult = {
                buildSetResult(
                    setType = when (newSet) {
                        is LoggingStandardSetUiModel -> SetType.STANDARD
                        is LoggingDropSetUiModel -> SetType.DROP_SET
                        is LoggingMyoRepSetUiModel -> SetType.MYOREP
                        else -> throw Exception("${newSet::class.simpleName} is not defined")
                    },
                    liftId = workoutLift.liftId,
                    progressionScheme = workoutLift.progressionScheme,
                    liftPosition = workoutLift.position,
                    setPosition = newSet.position,
                    myoRepSetPosition = (newSet as? LoggingMyoRepSetUiModel)?.myoRepSetPosition,
                    weight = newSet.completedWeight ?: 0f,
                    reps = newSet.completedReps ?: 0,
                    rpe = newSet.completedRpe ?: 8f,
                )
            }
        )
    }
}