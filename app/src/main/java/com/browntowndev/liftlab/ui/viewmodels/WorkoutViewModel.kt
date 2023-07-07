package com.browntowndev.liftlab.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.getCurrentDate
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.entities.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class WorkoutViewModel(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository,
    private val loggingRepository: LoggingRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(WorkoutState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { currentState ->
                val programMetadata = programsRepository.getActiveProgramMetadata()
                if (programMetadata != null) {
                    currentState.copy(
                        programMetadata = programMetadata,
                        workoutWithProgression = workoutsRepository.getNextToPerform(programMetadata)
                    )
                } else currentState
            }
        }
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> _state.update {
                it.copy(workoutLogVisible = false)
            }
            else -> {}
        }
    }

    fun setInProgress(inProgress: Boolean) {
        executeInTransactionScope {
            workoutInProgressRepository.insert(
                WorkoutInProgressDto(
                    startTime = getCurrentDate(),
                    workoutId = _state.value.workoutWithProgression!!.workout.id,
                )
            )
            _state.update {
                it.copy(
                    inProgress = inProgress,
                    workoutLogVisible = inProgress,
                )
            }
        }
    }

    fun setWorkoutLogVisibility(visible: Boolean) {
        _state.update {
            it.copy(
                workoutLogVisible = visible
            )
        }
    }

    fun completeSet(result: SetResult) {
        executeInTransactionScope {
            setResultsRepository.insert(result)
            _state.update {
                it.copy(
                    completedSets = it.completedSets.toMutableMap().apply {
                        when (result) {
                            is MyoRepSetResultDto -> set("${result.liftId}-${result.setPosition}-${result.myoRepSetPosition}", result)
                            else -> set("${result.liftId}-${result.setPosition}", result)
                        }
                    }
                )
            }
        }
    }

    fun undoSetCompletion(liftId: Long, setPosition: Int) {
        executeInTransactionScope {
            setResultsRepository.delete(
                workoutId = _state.value.workoutWithProgression!!.workout.id,
                liftId = liftId,
                setPosition = setPosition
            )
            removeSetFromCompletedSetsHashSet("$liftId-$setPosition")
        }
    }

    fun undoSetCompletion(liftId: Long, setPosition: Int, myoRepSetPosition: Int) {
        executeInTransactionScope {
            setResultsRepository.delete(
                workoutId = _state.value.workoutWithProgression!!.workout.id,
                liftId = liftId,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition
            )
            removeSetFromCompletedSetsHashSet("$liftId-$setPosition-$myoRepSetPosition")
        }
    }

    private fun removeSetFromCompletedSetsHashSet(key: String) {
        _state.update {
            it.copy(
                completedSets = it.completedSets.toMutableMap().apply {
                    remove(key)
                }
            )
        }
    }

    fun finishWorkout(durationInMillis: Long) {
        executeInTransactionScope {
            val workout = _state.value.workoutWithProgression!!.workout
            workoutInProgressRepository.delete(workout.id)

            // Delete all set results from the previous workout
            val programMetadata = _state.value.programMetadata!!
            setResultsRepository.deleteAllNotForWorkout(
                workoutId = workout.id,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
            )

            // Increment the mesocycle and microcycle
            val microCycleComplete =
                (programMetadata.workoutCount - 1) == programMetadata.currentMicrocyclePosition
            val deloadWeekComplete =
                microCycleComplete && (programMetadata.deloadWeek - 1) == programMetadata.currentMicrocycle
            programsRepository.updateMesoAndMicroCycle(
                id = programMetadata.programId,
                mesoCycle = if (deloadWeekComplete) programMetadata.currentMesocycle + 1 else programMetadata.currentMesocycle,
                microCycle = if (microCycleComplete) programMetadata.currentMicrocycle + 1 else programMetadata.currentMicrocycle,
                microCyclePosition = if (microCycleComplete) 0 else programMetadata.currentMicrocyclePosition + 1
            )

            // Get/create the historical workout name entry then use it to insert a workout log entry
            var historicalWorkoutNameId =
                historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(
                    programId = programMetadata.programId,
                    workoutId = workout.id,
                )
            if (historicalWorkoutNameId == null) {
                historicalWorkoutNameId = historicalWorkoutNamesRepository.insert(
                    programId = programMetadata.programId,
                    workoutId = workout.id,
                    programName = programMetadata.name,
                    workoutName = workout.name,
                )
            }
            val workoutLogEntryId = loggingRepository.insertWorkoutLogEntry(
                historicalWorkoutNameId = historicalWorkoutNameId,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
                date = getCurrentDate(),
                durationInMillis = durationInMillis,
            )

            // Copy all of the set results from this workout into the set history table
            loggingRepository.insertFromPreviousSetResults(workoutLogEntryId)
        }
    }

    fun cancelWorkout() {
        executeInTransactionScope {
            // Delete the in progress entry
            val workout = _state.value.workoutWithProgression!!.workout
            workoutInProgressRepository.delete(workout.id)

            // Delete all set results from the workout
            val programMetadata = _state.value.programMetadata!!
            setResultsRepository.deleteAllNotForWorkout(
                workoutId = workout.id,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
            )
        }
    }
}