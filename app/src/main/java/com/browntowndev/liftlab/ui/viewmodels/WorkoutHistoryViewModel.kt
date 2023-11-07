package com.browntowndev.liftlab.ui.viewmodels

import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.progression.CalculationEngine
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutHistoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class WorkoutHistoryViewModel(
    private val navHostController: NavHostController,
    private val loggingRepository: LoggingRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _loggingLiveData: LiveData<List<WorkoutLogEntryDto>>? = null
    private var _logObserver: Observer<List<WorkoutLogEntryDto>>? = null
    private val _state = MutableStateFlow(WorkoutHistoryState())
    val state = _state.asStateFlow()

    init {
        _logObserver = Observer { workoutLogs ->
            val dateOrderedWorkoutLogs = sortAndSetPersonalRecords(workoutLogs)
            val topSets = getTopSets(dateOrderedWorkoutLogs)
            _state.update {
                it.copy(
                    dateOrderedWorkoutLogs = dateOrderedWorkoutLogs,
                    topSets = topSets,
                )
            }
        }
        _loggingLiveData = loggingRepository.getAll()
        _loggingLiveData!!.observeForever(_logObserver!!)
    }

    override fun onCleared() {
        super.onCleared()
        _loggingLiveData?.removeObserver(_logObserver!!)
    }

    @Subscribe
    fun handleTopAppBarActionEvent(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> navHostController.popBackStack()
            else -> { }
        }
    }

    private fun sortAndSetPersonalRecords(workoutLogs: List<WorkoutLogEntryDto>): List<WorkoutLogEntryDto> {
        val personalRecords = getPersonalRecords(workoutLogs)
        val updatedLogs = workoutLogs
            .sortedByDescending { it.date }
            .fastMap { workoutLog ->
                workoutLog.copy(
                    setResults = workoutLog.setResults.fastMap { setLog ->
                        if (personalRecords.contains(setLog)) {
                            setLog.copy(
                                isPersonalRecord = true
                            )
                        } else setLog
                    }
                )
            }

        return updatedLogs
    }

    private fun getPersonalRecords(workoutLogs: List<WorkoutLogEntryDto>): HashSet<SetLogEntryDto> {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults
                .groupBy { it.liftId }
                .map { liftSetResults ->
                    liftSetResults.value.maxBy {
                        CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                    }
                }
        }.toHashSet()
    }

    private fun getTopSets(workoutLogs: List<WorkoutLogEntryDto>): Map<Long, Map<Long, Pair<Int, SetLogEntryDto>>> {
        return workoutLogs.associate { workoutLog ->
            workoutLog.id to getTopSetsForWorkout(workoutLog)
        }
    }

    private fun getTopSetsForWorkout(workoutLog: WorkoutLogEntryDto): Map<Long, Pair<Int, SetLogEntryDto>> {
        return workoutLog.setResults
            .groupBy { it.liftId }
            .filterValues { set -> set.isNotEmpty() }
            .mapValues { (_, sets) ->
                val setSize = sets.size
                val topSet = sets.maxBy {
                    CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                }
                setSize to topSet
            }
    }
}