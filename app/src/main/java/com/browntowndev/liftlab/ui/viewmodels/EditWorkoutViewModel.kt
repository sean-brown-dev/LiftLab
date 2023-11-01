package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.EditWorkoutMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class EditWorkoutViewModel(
    private val workoutsRepository: WorkoutsRepository,
    private val loggingRepository: LoggingRepository,
    private val editingWorkout: EditWorkoutMetadataDto,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    transactionScope = transactionScope,
    eventBus = eventBus,
) {
    private var _workoutLogEntryId by mutableLongStateOf(-1L)
    private val _liftNames by lazy {
        mutableState.value.workout!!.lifts.associate {
            it.liftId to it.liftName
        }
    }

    init {
        viewModelScope.launch {
            mutableState.update { currentState ->
                val workout = workoutsRepository.getForLogging(editingWorkout.workoutId)
                currentState.copy(
                    workout = workout,
                    inProgressWorkout = WorkoutInProgressDto(
                        workoutId = editingWorkout.workoutId,
                        startTime = Utils.getCurrentDate(),
                        completedSets = listOf(),
                    )
                )
            }

            val workoutLog = loggingRepository.getForWorkout(
                workoutId = editingWorkout.workoutId,
                mesoCycle = editingWorkout.mesoCycle,
                microCycle = editingWorkout.microCycle,
            )

            if (workoutLog != null) {
                _workoutLogEntryId = workoutLog.id
                updateFromWorkoutLog(workoutLog)
            }
        }
    }

    private fun updateFromWorkoutLog(workoutLog: WorkoutLogEntryDto) {
        val setResults = workoutLog.setResults.associateBy {
            "${it.liftPosition}-${it.setPosition}"
        }
        val progressionSchemes = mutableState.value.workout!!.lifts.associate {
            it.position to it.progressionScheme
        }
        mutableState.update { currentState ->
            currentState.copy(
                workout = currentState.workout!!.copy(
                    lifts = currentState.workout.lifts.fastMap { workoutLift ->
                        workoutLift.copy(
                            sets = workoutLift.sets.fastMap { set ->
                                val result = setResults["${workoutLift.position}-${set.position}"]
                                if (result != null) {
                                    when (set) {
                                        is LoggingStandardSetDto -> set.copy(
                                            complete = true,
                                            completedWeight = result.weight,
                                            completedReps = result.reps,
                                            completedRpe = result.rpe,
                                        )

                                        is LoggingMyoRepSetDto -> set.copy(
                                            complete = true,
                                            completedWeight = result.weight,
                                            completedReps = result.reps,
                                            completedRpe = result.rpe,
                                        )

                                        is LoggingDropSetDto -> set.copy(
                                            complete = true,
                                            completedWeight = result.weight,
                                            completedReps = result.reps,
                                            completedRpe = result.rpe,
                                        )

                                        else -> throw Exception("${set::class.simpleName} is not defined.")
                                    }
                                } else {
                                    set
                                }
                            }
                        )
                    }
                ),
                inProgressWorkout = currentState.inProgressWorkout!!.copy(
                    completedSets = workoutLog.setResults.fastMap { setLogEntry ->
                        super.buildSetResult(
                            liftId = setLogEntry.liftId,
                            setType = setLogEntry.setType,
                            progressionScheme = progressionSchemes[setLogEntry.liftPosition]!!,
                            liftPosition = setLogEntry.liftPosition,
                            setPosition = setLogEntry.setPosition,
                            myoRepSetPosition = setLogEntry.myoRepSetPosition,
                            weight = setLogEntry.weight,
                            reps = setLogEntry.reps,
                            rpe = setLogEntry.rpe,
                        )
                    }
                ),
            )
        }
    }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> {
        return loggingRepository.upsertMany(
            workoutLogEntryId = _workoutLogEntryId,
            updatedResults.fastMap { setResult ->
                SetLogEntryDto(
                    liftId = setResult.liftId,
                    liftName = _liftNames[setResult.liftId]!!,
                    setType = setResult.setType,
                    liftPosition = setResult.liftPosition,
                    setPosition = setResult.setPosition,
                    myoRepSetPosition = (setResult as? MyoRepSetResultDto)?.myoRepSetPosition,
                    weight = setResult.weight,
                    reps = setResult.reps,
                    rpe = setResult.rpe,
                    mesoCycle = editingWorkout.mesoCycle,
                    microCycle = editingWorkout.microCycle,
                )
            }
        )
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        return loggingRepository.upsert(
            workoutLogEntryId = _workoutLogEntryId,
            SetLogEntryDto(
                liftId = updatedResult.liftId,
                liftName = _liftNames[updatedResult.liftId]!!,
                setType = updatedResult.setType,
                liftPosition = updatedResult.liftPosition,
                setPosition = updatedResult.setPosition,
                myoRepSetPosition = (updatedResult as? MyoRepSetResultDto)?.myoRepSetPosition,
                weight = updatedResult.weight,
                reps = updatedResult.reps,
                rpe = updatedResult.rpe,
                mesoCycle = editingWorkout.mesoCycle,
                microCycle = editingWorkout.microCycle,
            )
        )
    }

    override suspend fun deleteSetResult(
        workoutId: Long,
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?
    ) {
        loggingRepository.delete(
            workoutId = workoutId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
        )
    }
}