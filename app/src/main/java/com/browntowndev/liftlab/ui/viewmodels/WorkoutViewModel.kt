package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.entities.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.progression.MyoRepSetGoalValidator
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.time.Duration

class WorkoutViewModel(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository,
    private val loggingRepository: LoggingRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val liftsRepository: LiftsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): LiftLabViewModel(transactionScope, eventBus) {
    private var _state = MutableStateFlow(WorkoutState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
            val programMetadata = programsRepository.getActiveProgramMetadata()
            if (programMetadata != null) {
                workoutsRepository.getNextToPerform(programMetadata)
                    .observeForever { workout ->
                        viewModelScope.launch {
                            val inProgressWorkout = workoutInProgressRepository.get(
                                programMetadata.currentMesocycle,
                                programMetadata.currentMicrocycle
                            )

                            _state.update { currentState ->
                                currentState.copy(
                                    inProgressWorkout = inProgressWorkout,
                                    programMetadata = programMetadata,
                                    workout = workout,
                                )
                            }

                            restTimerInProgressRepository.getLive()
                                .observeForever { restTimerInProgress ->
                                    _state.update { currentState ->
                                        currentState.copy(
                                            restTimerStartedAt = restTimerInProgress?.timeStartedInMillis?.toDate(),
                                            restTime = restTimerInProgress?.restTime ?: 0L,
                                        )
                                    }
                                }
                        }
                    }
            }
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> _state.update {
                it.copy(workoutLogVisible = false)
            }
            TopAppBarAction.RestTimerCompleted -> {
                executeInTransactionScope {
                    restTimerInProgressRepository.deleteAll()
                    _state.update {
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
                workoutId = _state.value.workout!!.id,
                completedSets = listOf(),
            )
            workoutInProgressRepository.insert(inProgressWorkout)
            _state.update {
                it.copy(
                    inProgressWorkout = inProgressWorkout,
                    workoutLogVisible = true,
                )
            }
        }
    }

    fun cancelWorkout() {
        executeInTransactionScope {
            // Remove the workout from in progress
            workoutInProgressRepository.delete()

            // Delete all set results from the workout
            val programMetadata = _state.value.programMetadata!!
            setResultsRepository.deleteAllForWorkout(
                workoutId = _state.value.workout!!.id,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
            )

            _state.update {
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

    fun setWorkoutLogVisibility(visible: Boolean) {
        _state.update {
            it.copy(
                workoutLogVisible = visible
            )
        }
    }

    fun setRpe(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newRpe: Float) {
        // Don't persist this. Persistence happens when entire set is completed
        _state.update { currentState ->
            currentState.copy(
                workout = currentState.workout!!.copy(
                    lifts = currentState.workout.lifts.map { workoutLift ->
                        if (workoutLift.id == workoutLiftId) {
                            workoutLift.copy(
                                sets = workoutLift.sets.map { set ->
                                    if (set.setPosition == setPosition &&
                                        (set as? LoggingMyoRepSetDto)?.myoRepSetPosition == myoRepSetPosition
                                    ) {
                                        when (set) {
                                            is LoggingStandardSetDto -> set.copy(
                                                completedRpe = newRpe,
                                            )

                                            is LoggingDropSetDto -> set.copy(
                                                completedRpe = newRpe,
                                            )

                                            is LoggingMyoRepSetDto -> set.copy(
                                                completedRpe = newRpe,
                                            )

                                            else -> throw Exception("${set::class.simpleName} is not defined.")
                                        }
                                    } else set
                                }
                            )
                        } else workoutLift
                    }
                )
            )
        }
    }

    private fun updateLoggingSetsOnMyoRepSetCompletion(
        mutableLoggingSets: MutableList<GenericLoggingSet>,
        thisMyoRepSet: LoggingMyoRepSetDto,
        result: MyoRepSetResultDto,
    ): List<GenericLoggingSet> {
        val myoRepSets = mutableLoggingSets.filterIsInstance<LoggingMyoRepSetDto>()
        val previousMyoRepResults = myoRepSets
            .filter {
                (thisMyoRepSet.myoRepSetPosition ?: -1) > (it.myoRepSetPosition ?: -1)
            }

        val hasMyoRepSetsAfter = myoRepSets
            .any {
                (thisMyoRepSet.myoRepSetPosition ?: -1) < (it.myoRepSetPosition ?: -1)
            }

        return if (!hasMyoRepSetsAfter && // Don't add more myorep sets if it already has them
            MyoRepSetGoalValidator.shouldContinueMyoReps(
                completedSet = thisMyoRepSet,
                previousMyoRepSets = previousMyoRepResults,
            )
        ) {
            mutableLoggingSets.apply {
                val myoRepSetIndex = if(thisMyoRepSet.myoRepSetPosition != null) {
                    thisMyoRepSet.myoRepSetPosition + 1
                } else 0
                val insertAtIndex = 1 + thisMyoRepSet.setPosition + myoRepSetIndex
                add(
                    index = insertAtIndex,
                    thisMyoRepSet.copy(
                        myoRepSetPosition = previousMyoRepResults.size,
                        weightRecommendation = result.weight,
                        repRangePlaceholder = if (thisMyoRepSet.repFloor != null) ">${thisMyoRepSet.repFloor}"
                            else "—",
                        complete = false,
                        completedWeight = null,
                        completedReps = null,
                        completedRpe = null,
                    )
                )
            }
        } else mutableLoggingSets
    }

    private fun copyDropSetWithUpdatedWeightRecommendation(
        set: LoggingDropSetDto,
        result: SetResult,
        increment: Float,
    ): LoggingDropSetDto {
        if (set.setPosition < 1) throw Exception ("Drop set must come after another set.")

        return set.copy(
            weightRecommendation = (result.weight * (1 - set.dropPercentage))
                .roundToNearestFactor(increment)
        )
    }

    private fun copySetsOnCompletion(
        result: SetResult,
        currentSets: List<GenericLoggingSet>,
        increment: Float,
    ): List<GenericLoggingSet> {
        var thisMyoRepSet: LoggingMyoRepSetDto? = null
        val mutableSetCopy = currentSets.fastMap { set ->
            if (result.setPosition == set.setPosition &&
                (result as? MyoRepSetResultDto)?.myoRepSetPosition == (set as? LoggingMyoRepSetDto)?.myoRepSetPosition) {
                when (set) {
                    is LoggingStandardSetDto -> set.copy(
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

                    is LoggingMyoRepSetDto -> {
                        thisMyoRepSet = set.copy(
                            complete = true,
                            completedWeight = result.weight,
                            completedReps = result.reps,
                            completedRpe = result.rpe,
                        )
                        thisMyoRepSet!!
                    }

                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            } else if (set.setPosition == (result.setPosition + 1) && set is LoggingDropSetDto) {
                // If there is a drop set after this completed set update it's weight recommendation
                copyDropSetWithUpdatedWeightRecommendation(
                    set = set,
                    result = result,
                    increment = increment,
                )
            } else set
        }.toMutableList()

        return if (thisMyoRepSet != null) {
            updateLoggingSetsOnMyoRepSetCompletion(
                mutableLoggingSets = mutableSetCopy,
                thisMyoRepSet = thisMyoRepSet!!,
                result = result as MyoRepSetResultDto,
            )
        } else mutableSetCopy
    }

    fun completeSet(restTime: Long, result: SetResult) {
        executeInTransactionScope {
            restTimerInProgressRepository.insert(restTime)
            _state.update { currentState ->
                currentState.copy(
                    restTime = restTime,
                    restTimerStartedAt = Utils.getCurrentDate(),
                    inProgressWorkout = currentState.inProgressWorkout?.copy(
                        completedSets = currentState.inProgressWorkout.completedSets.toMutableList().apply {
                            val existingResult = find { existing ->
                                existing.setPosition == result.setPosition &&
                                        (existing as? MyoRepSetResultDto)?.myoRepSetPosition ==
                                         (result as? MyoRepSetResultDto)?.myoRepSetPosition
                            }

                            if (existingResult != null) {
                                val updatedResult = when (result) {
                                    is StandardSetResultDto -> result.copy(id = existingResult.id)
                                    is MyoRepSetResultDto -> result.copy(id = existingResult.id)
                                    is LinearProgressionSetResultDto -> result.copy(id = existingResult.id)
                                    else -> throw Exception("${result::class.simpleName} is not defined.")
                                }

                                setResultsRepository.upsert(updatedResult)
                                set(indexOf(existingResult), updatedResult)
                            } else {
                                val id = setResultsRepository.upsert(result)
                                val insertedResult = when (result) {
                                    is StandardSetResultDto -> result.copy(id = id)
                                    is MyoRepSetResultDto -> result.copy(id = id)
                                    is LinearProgressionSetResultDto -> result.copy(id = id)
                                    else -> throw Exception("${result::class.simpleName} is not defined.")
                                }
                                add(insertedResult)
                            }

                        }
                    ),
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.map { workoutLift ->
                            if (workoutLift.liftId == result.liftId) {
                                workoutLift.copy(
                                    sets = copySetsOnCompletion(
                                        result = result,
                                        currentSets = workoutLift.sets,
                                        increment = workoutLift.incrementOverride
                                            ?: workoutLift.liftIncrementOverride
                                            ?: SettingsManager.getSetting(
                                                SettingsManager.SettingNames.INCREMENT_AMOUNT,
                                                5f
                                            ))
                                )
                            } else workoutLift
                        }
                    )
                )
            }
        }
    }

    fun undoSetCompletion(liftId: Long, setPosition: Int, myoRepSetPosition: Int?) {
        executeInTransactionScope {
            setResultsRepository.delete(
                workoutId = _state.value.workout!!.id,
                liftId = liftId,
                setPosition = setPosition,
                myoRepSetPosition = myoRepSetPosition
            )

            _state.update { currentState ->
                currentState.copy(
                    restTimerStartedAt = null,
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { workoutLift ->
                            var hasWeightRecommendation = false
                            if (workoutLift.liftId == liftId) {
                                workoutLift.copy(
                                    sets = workoutLift.sets.fastMap { set ->
                                        if (set.setPosition == setPosition &&
                                            (set as? LoggingMyoRepSetDto)?.myoRepSetPosition == myoRepSetPosition) {
                                            hasWeightRecommendation = set.weightRecommendation != null
                                            when (set) {
                                                is LoggingStandardSetDto -> set.copy(complete = false)
                                                is LoggingDropSetDto -> set.copy(complete = false)
                                                is LoggingMyoRepSetDto -> set.copy(complete = false)
                                                else -> throw Exception("${set::class.simpleName} is not defined.")
                                            }
                                        } else if (!hasWeightRecommendation &&
                                            set.setPosition == (setPosition + 1) &&
                                            set is LoggingDropSetDto) {
                                            // undo the drop set's weight recommendation when its main
                                            // set is set as incomplete and it has no recommendation
                                            set.copy(weightRecommendation = null)
                                        } else set
                                    }
                                )
                            } else workoutLift
                        }
                    )
                )
            }
        }
    }

    private fun finishWorkout() {
        executeInTransactionScope {
            val startTimeInMillis = _state.value.inProgressWorkout!!.startTime.time
            val durationInMillis = (Utils.getCurrentDate().time - startTimeInMillis)

            // Remove the workout from in progress
            workoutInProgressRepository.delete()

            // Delete all set results from the previous workout
            val programMetadata = _state.value.programMetadata!!
            val workout = _state.value.workout!!
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
            val newMesoCycle = if (deloadWeekComplete) programMetadata.currentMesocycle + 1 else programMetadata.currentMesocycle
            val newMicroCycle = if (microCycleComplete) programMetadata.currentMicrocycle + 1 else programMetadata.currentMicrocycle
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

            // Update any Linear Progression failures
            updateLinearProgressionFailures()

            // Copy all of the set results from this workout into the set history table
            loggingRepository.insertFromPreviousSetResults(workoutLogEntryId)

            initialize()
        }
    }

    private suspend fun updateLinearProgressionFailures() {
        val resultsByLift = _state.value.inProgressWorkout!!.completedSets.associateBy {
            "${it.liftId}-${it.setPosition}"
        }
        val setResultsToUpdate = mutableListOf<SetResult>()

        _state.value.workout!!.lifts.fastForEach { workoutLift ->
            if (workoutLift.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION) {
                workoutLift.sets.fastForEach { set ->
                    val result = resultsByLift["${workoutLift.liftId}-${set.setPosition}"]
                    if(result != null &&
                        ((set.completedWeight ?: -2f) < (set.weightRecommendation ?: -1f) ||
                                (set.completedReps ?: -1) < set.repRangeTop ||
                                (set.completedRpe ?: -1f) <= set.rpeTarget)) {
                        val lpResults = result as LinearProgressionSetResultDto
                        setResultsToUpdate.add(
                            lpResults.copy(
                                missedLpGoals = lpResults.missedLpGoals + 1
                            )
                        )
                    }
                }
            }
        }

        setResultsRepository.insertMany(setResultsToUpdate)
    }

    fun updateRestTime(workoutLiftId: Long, newRestTime: Duration, applyToLift: Boolean) {
        executeInTransactionScope {
            val workoutLift = _state.value.workout!!.lifts.find{ it.id == workoutLiftId }!!
            val workoutLiftCopy =  if (applyToLift) workoutLift.copy(restTime = null, liftRestTime = newRestTime)
                else workoutLift.copy(restTime = newRestTime)

            if (applyToLift) {
                liftsRepository.updateRestTime(
                    id = workoutLift.liftId,
                    newRestTime = newRestTime
                )
            } else {
                workoutLiftsRepository.updateRestTime(
                    workoutLiftId = workoutLiftId,
                    restTime = workoutLiftCopy.restTime
                )
            }

            _state.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { lift ->
                            if (lift.id == workoutLiftId) workoutLiftCopy
                            else lift
                        }
                    )
                )
            }
        }
    }

    fun deleteMyoRepSet(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int) {
        executeInTransactionScope {
            val liftId = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!.liftId
            val isComplete = _state.value.inProgressWorkout!!.completedSets.find {
                (it is MyoRepSetResultDto) &&
                        (it.liftId == liftId) &&
                        (it.setPosition == setPosition) &&
                        (it.myoRepSetPosition == myoRepSetPosition)
            } != null

            if (isComplete) {
                this.undoSetCompletion(
                    liftId = liftId,
                    setPosition = setPosition,
                    myoRepSetPosition = myoRepSetPosition
                )
            }

            _state.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.let { workout ->
                        workout.copy(
                            lifts = workout.lifts.fastMap { workoutLift ->
                                if (workoutLift.id == workoutLiftId) {
                                    workoutLift.copy(
                                        sets = workoutLift.sets.toMutableList().apply {
                                            val toDelete = find { set ->
                                                set.setPosition == setPosition &&
                                                        (set as LoggingMyoRepSetDto).myoRepSetPosition == myoRepSetPosition
                                            }!!

                                            remove(toDelete)
                                        }.mapIndexed { index, set ->
                                            if (index > 0) {
                                                val myoRepSet = set as LoggingMyoRepSetDto
                                                myoRepSet.copy(myoRepSetPosition = index - 1)
                                            } else set
                                        }
                                    )
                                } else workoutLift
                            }
                        )
                    }
                )
            }
        }
    }
}