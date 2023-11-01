package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.Integer.max
import kotlin.time.Duration

class WorkoutViewModel(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    val setResultsRepository: PreviousSetResultsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository,
    private val loggingRepository: LoggingRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val liftsRepository: LiftsRepository,
    private val cancelRestTimer: () -> Unit,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    transactionScope = transactionScope,
    eventBus = eventBus,
) {
    init {
        initialize()
    }

    private fun initialize() {
        restTimerInProgressRepository.getLive().observeForever { restTimerInProgress ->
            mutableState.update { currentState ->
                currentState.copy(
                    restTimerStartedAt = restTimerInProgress?.timeStartedInMillis?.toDate(),
                    restTime = restTimerInProgress?.restTime ?: 0L,
                )
            }
        }
        programsRepository.getActiveProgramMetadata().observeForever { programMetadata ->
            if (programMetadata != null) {
                viewModelScope.launch {
                    workoutsRepository.getNextToPerform(programMetadata).observeForever { workout ->
                        executeInTransactionScope {
                            val inProgressWorkout = workoutInProgressRepository.get(
                                programMetadata.currentMesocycle,
                                programMetadata.currentMicrocycle
                            )

                            mutableState.update { currentState ->
                                currentState.copy(
                                    inProgressWorkout = inProgressWorkout,
                                    programMetadata = programMetadata,
                                    workout = workout,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> mutableState.update {
                it.copy(workoutLogVisible = false)
            }
            TopAppBarAction.RestTimerCompleted -> {
                executeInTransactionScope {
                    restTimerInProgressRepository.deleteAll()
                    mutableState.update {
                        it.copy(restTimerStartedAt = null)
                    }
                }
            }
            TopAppBarAction.FinishWorkout -> finishWorkout() //TODO: add modal & callback to confirm
            else -> {}
        }
    }

    fun startWorkout() {
        executeInTransactionScope {
            val inProgressWorkout = WorkoutInProgressDto(
                startTime = Utils.getCurrentDate(),
                workoutId = mutableState.value.workout!!.id,
                completedSets = listOf(),
            )
            workoutInProgressRepository.insert(inProgressWorkout)
            mutableState.update {
                it.copy(
                    inProgressWorkout = inProgressWorkout,
                    workoutLogVisible = true,
                )
            }
        }
    }

    fun setWorkoutLogVisibility(visible: Boolean) {
        mutableState.update {
            it.copy(
                workoutLogVisible = visible
            )
        }
    }

    fun finishWorkout() {
        executeInTransactionScope {
            val startTimeInMillis = mutableState.value.inProgressWorkout!!.startTime.time
            val durationInMillis = (Utils.getCurrentDate().time - startTimeInMillis)
            val programMetadata = mutableState.value.programMetadata!!
            val workout = mutableState.value.workout!!

            // Remove the workout from in progress
            workoutInProgressRepository.delete()
            restTimerInProgressRepository.deleteAll()

            // Increment the mesocycle and microcycle
            val microCycleComplete =  (programMetadata.workoutCount - 1) == programMetadata.currentMicrocyclePosition
            val lastDeloadWeek = max(programMetadata.deloadWeek, workout.lifts.maxOf { it.deloadWeek })
            val deloadWeekComplete = microCycleComplete && (lastDeloadWeek - 1) == programMetadata.currentMicrocycle
            val newMesoCycle = if (deloadWeekComplete) programMetadata.currentMesocycle + 1 else programMetadata.currentMesocycle
            val newMicroCycle = if (deloadWeekComplete) 0 else if (microCycleComplete) programMetadata.currentMicrocycle + 1 else programMetadata.currentMicrocycle
            val newMicroCyclePosition = if (microCycleComplete) 0 else programMetadata.currentMicrocyclePosition + 1
            programsRepository.updateMesoAndMicroCycle(
                id = programMetadata.programId,
                mesoCycle = newMesoCycle,
                microCycle = newMicroCycle,
                microCyclePosition = newMicroCyclePosition,
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
                date = Utils.getCurrentDate(),
                durationInMillis = durationInMillis,
            )

            // Delete all set results from the previous workout
            setResultsRepository.deleteAllForPreviousWorkout(
                workoutId = workout.id,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
            )

            val liftsAndPositions = mutableState.value.workout!!.lifts.associate {
                it.liftId to it.position
            }
            // If any lifts were changed and had completed results do not copy them
            val excludeFromCopy = mutableState.value.inProgressWorkout!!.completedSets.filter { result ->
                val liftPosition = liftsAndPositions[result.liftId]
                liftPosition != result.liftPosition
            }.map {
                it.id
            }

            // Copy all of the set results from this workout into the set history table
            loggingRepository.insertFromPreviousSetResults(
                workoutLogEntryId = workoutLogEntryId,
                workoutId = mutableState.value.workout!!.id,
                excludeFromCopy = excludeFromCopy
            )

            // Update any Linear Progression failures
            // The reason this is done when the workout is completed is because if it were done on the fly
            // you'd have no easy way of knowing if someone failed (increment), changed result (still failure)
            // and then you get double increment. Or any variation of them going between success/failure by
            // modifying results.
            updateLinearProgressionFailures()

            stopRestTimer()

            // TODO: have summary pop up as dialog and close this on completion instead
            mutableState.update {
                it.copy(workoutLogVisible = false)
            }
        }
    }

    fun cancelWorkout() {
        executeInTransactionScope {
            // Remove the workout from in progress
            workoutInProgressRepository.delete()

            // Delete all set results from the workout
            val programMetadata = mutableState.value.programMetadata!!
            setResultsRepository.deleteAllForWorkout(
                workoutId = mutableState.value.workout!!.id,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
            )

            mutableState.update {
                it.copy(
                    workoutLogVisible = false,
                    inProgressWorkout = null,
                    workout = it.workout?.copy(
                        lifts = it.workout.lifts.fastMap { workoutLift ->
                            workoutLift.copy(
                                sets = workoutLift.sets
                                    .filter { set ->
                                        (set as? LoggingMyoRepSetDto)?.myoRepSetPosition == null
                                    }.fastMap { set ->
                                        when (set) {
                                            is LoggingStandardSetDto -> set.copy(
                                                completedWeight = null,
                                                completedReps = null,
                                                completedRpe = null,
                                            )

                                            is LoggingDropSetDto -> set.copy(
                                                completedWeight = null,
                                                completedReps = null,
                                                completedRpe = null,
                                            )

                                            is LoggingMyoRepSetDto -> set.copy(
                                                completedWeight = null,
                                                completedReps = null,
                                                completedRpe = null,
                                            )

                                            else -> throw Exception("${set::class.simpleName} is not defined.")
                                        }
                                    }
                            )
                        }
                    )
                )
            }
        }
    }

    fun updateRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) {
        executeInTransactionScope {
            mutableState.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { lift ->
                            if (lift.id == workoutLiftId) {
                                val workoutLiftCopy = lift.copy(restTime = newRestTime, restTimerEnabled = enabled)
                                liftsRepository.updateRestTime(
                                    id = lift.liftId,
                                    enabled = enabled,
                                    newRestTime = newRestTime
                                )
                                workoutLiftCopy
                            }
                            else lift
                        }
                    )
                )
            }
        }
    }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> {
        return setResultsRepository.upsertMany(updatedResults)
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        return setResultsRepository.upsert(updatedResult)
    }

    override suspend fun deleteSetResult(
        workoutId: Long,
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int?
    ) {
        setResultsRepository.delete(
            workoutId = workoutId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition
        )
    }

    override suspend fun insertRestTimerInProgress(restTime: Long) {
        restTimerInProgressRepository.insert(restTime)
    }

    override fun stopRestTimer() {
        cancelRestTimer()
    }
}