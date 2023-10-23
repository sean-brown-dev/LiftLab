package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
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
import com.browntowndev.liftlab.core.persistence.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.progression.MyoRepSetGoalValidator
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun setWeight(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newWeight: Float?) {
        // Don't persist this. Persistence happens when entire set is completed
        completeLogEntryItem(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            copySet = { set ->
                when (set) {
                    is LoggingStandardSetDto -> set.copy(completedWeight = newWeight)
                    is LoggingDropSetDto -> set.copy(completedWeight = newWeight)
                    is LoggingMyoRepSetDto -> set.copy(completedWeight = newWeight)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            }
        )
    }

    fun setReps(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newReps: Int?) {
        // Don't persist this. Persistence happens when entire set is completed
        completeLogEntryItem(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            copySet = { set ->
                when (set) {
                    is LoggingStandardSetDto -> set.copy(completedReps = newReps)
                    is LoggingDropSetDto -> set.copy(completedReps = newReps)
                    is LoggingMyoRepSetDto -> set.copy(completedReps = newReps)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            }
        )
    }

    fun setRpe(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, newRpe: Float) {
        // Don't persist this. Persistence happens when entire set is completed
        completeLogEntryItem(
            workoutLiftId = workoutLiftId,
            setPosition = setPosition,
            myoRepSetPosition = myoRepSetPosition,
            copySet = { set ->
                when (set) {
                    is LoggingStandardSetDto -> set.copy(completedRpe = newRpe)
                    is LoggingDropSetDto -> set.copy(completedRpe = newRpe)
                    is LoggingMyoRepSetDto -> set.copy(completedRpe = newRpe)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            }
        )
    }

    private fun completeLogEntryItem(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int?, copySet: (set: GenericLoggingSet) -> GenericLoggingSet) {
        _state.update { currentState ->
            currentState.copy(
                workout = currentState.workout!!.copy(
                    lifts = currentState.workout.lifts.fastMap { workoutLift ->
                        if (workoutLift.id == workoutLiftId) {
                            workoutLift.copy(
                                sets = workoutLift.sets.fastMap { set ->
                                    if (set.setPosition == setPosition &&
                                        (set as? LoggingMyoRepSetDto)?.myoRepSetPosition == myoRepSetPosition
                                    ) {
                                        copySet(set)
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
        completedWeight: Float,
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
                        weightRecommendation = completedWeight,
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
        completedWeight: Float,
        dropSet: LoggingDropSetDto,
        increment: Float,
    ): LoggingDropSetDto {
        return dropSet.copy(
            weightRecommendation = (completedWeight * (1 - dropSet.dropPercentage))
                .roundToNearestFactor(increment)
        )
    }

    private fun copySetsOnCompletion(
        setPosition: Int,
        myoRepSetPosition: Int?,
        currentSets: List<GenericLoggingSet>,
        increment: Float,
    ): List<GenericLoggingSet> {
        var completedSet: GenericLoggingSet? = null
        val mutableSetCopy = currentSets.fastMap { set ->
            if (setPosition == set.setPosition &&
                myoRepSetPosition == (set as? LoggingMyoRepSetDto)?.myoRepSetPosition) {
                completedSet = when (set) {
                    is LoggingStandardSetDto -> set.copy(complete = true)
                    is LoggingDropSetDto -> set.copy(complete = true)
                    is LoggingMyoRepSetDto -> set.copy(complete = true)
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
                completedSet!!
            } else if (set.setPosition == (setPosition + 1)) {
                when (set) {
                    is LoggingStandardSetDto -> set.copy(weightRecommendation = completedSet!!.completedWeight)
                    is LoggingMyoRepSetDto -> set.copy(weightRecommendation = completedSet!!.completedWeight)
                    is LoggingDropSetDto -> copyDropSetWithUpdatedWeightRecommendation(
                        completedWeight = currentSets[setPosition].completedWeight!!,
                        dropSet = set,
                        increment = increment,
                    )
                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            } else set
        }.toMutableList()

        return if (completedSet is LoggingMyoRepSetDto) {
            updateLoggingSetsOnMyoRepSetCompletion(
                mutableLoggingSets = mutableSetCopy,
                thisMyoRepSet = completedSet as LoggingMyoRepSetDto,
                completedWeight = completedSet!!.completedWeight!!,
            )
        } else mutableSetCopy
    }

    fun buildSetResult(
        liftId: Long,
        setType: SetType,
        progressionScheme: ProgressionScheme,
        setPosition: Int,
        myoRepSetPosition: Int?,
        weight: Float,
        reps: Int,
        rpe: Float,
    ): SetResult {
        val workoutId = _state.value.workout!!.id
        val currentMesocycle = _state.value.programMetadata!!.currentMesocycle
        val currentMicrocycle = _state.value.programMetadata!!.currentMicrocycle

        return when (setType) {
            SetType.STANDARD,
            SetType.DROP_SET -> {
                if (progressionScheme != ProgressionScheme.LINEAR_PROGRESSION) {
                    StandardSetResultDto(
                        workoutId = workoutId,
                        setType = setType,
                        liftId = liftId,
                        mesoCycle = currentMesocycle,
                        microCycle = currentMicrocycle,
                        setPosition = setPosition,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                    )
                } else {
                    // LP can only be standard lift, so no myo
                    LinearProgressionSetResultDto(
                        workoutId = workoutId,
                        liftId = liftId,
                        mesoCycle = currentMesocycle,
                        microCycle = currentMicrocycle,
                        setPosition = setPosition,
                        weight = weight,
                        reps = reps,
                        rpe = rpe,
                        missedLpGoals = 0, // assigned on completion
                    )
                }
            }

            SetType.MYOREP ->
                MyoRepSetResultDto(
                    workoutId = workoutId,
                    liftId = liftId,
                    mesoCycle = currentMesocycle,
                    microCycle = currentMicrocycle,
                    setPosition = setPosition,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    myoRepSetPosition = myoRepSetPosition,
                )
        }
    }

    fun completeSet(restTime: Long, restTimerEnabled: Boolean, result: SetResult) {
        executeInTransactionScope {
            if (restTimerEnabled) {
                restTimerInProgressRepository.insert(restTime)
            }
            _state.update { currentState ->
                currentState.copy(
                    restTime = if (restTimerEnabled) restTime else currentState.restTime,
                    restTimerStartedAt = if(restTimerEnabled) Utils.getCurrentDate() else currentState.restTimerStartedAt,
                    inProgressWorkout = currentState.inProgressWorkout?.copy(
                        completedSets = currentState.inProgressWorkout.completedSets.toMutableList().apply {
                            val existingResult = find { existing ->
                                existing.liftId == result.liftId &&
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
                                        setPosition = result.setPosition,
                                        myoRepSetPosition = (result as? MyoRepSetResultDto)?.myoRepSetPosition,
                                        currentSets = workoutLift.sets,
                                        increment = workoutLift.incrementOverride
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
                                            set.setPosition == (setPosition + 1)) {
                                            // undo the set's weight recommendation when its main
                                            // set is set as uncompleted and it had no recommendation
                                            when (set) {
                                                is LoggingStandardSetDto -> set.copy(weightRecommendation = null)
                                                is LoggingDropSetDto -> set.copy(weightRecommendation = null)
                                                is LoggingMyoRepSetDto -> set.copy(weightRecommendation = null)
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
    }

    fun finishWorkout() {
        executeInTransactionScope {
            val startTimeInMillis = _state.value.inProgressWorkout!!.startTime.time
            val durationInMillis = (Utils.getCurrentDate().time - startTimeInMillis)
            val programMetadata = _state.value.programMetadata!!
            val workout = _state.value.workout!!

            // Remove the workout from in progress
            workoutInProgressRepository.delete()
            restTimerInProgressRepository.deleteAll()

            // Increment the mesocycle and microcycle
            val microCycleComplete =  (programMetadata.workoutCount - 1) == programMetadata.currentMicrocyclePosition
            val deloadWeekComplete = microCycleComplete && (programMetadata.deloadWeek - 1) == programMetadata.currentMicrocycle
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
            setResultsRepository.deleteAllNotForWorkout(
                workoutId = workout.id,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
            )

            // Copy all of the set results from this workout into the set history table
            loggingRepository.insertFromPreviousSetResults(workoutLogEntryId)

            // Update any Linear Progression failures
            // The reason this is done when the workout is completed is because if it were done on the fly
            // you'd have no easy way of knowing if someone failed (increment), changed result (still failure)
            // and then you get double increment. Or any variation of them going between success/failure by
            // modifying results.
            updateLinearProgressionFailures()

            // TODO: have summary pop up as dialog and close this on completion instead
            _state.update {
                it.copy(workoutLogVisible = false)
            }

            withContext(Dispatchers.Main) {
                initialize()
            }
        }
    }

    private suspend fun updateLinearProgressionFailures() {
        val resultsByLift = _state.value.inProgressWorkout!!.completedSets.associateBy {
            "${it.liftId}-${it.setPosition}"
        }
        val setResultsToUpdate = mutableListOf<SetResult>()
        _state.value.workout!!.lifts
            .filter { workoutLift -> workoutLift.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION }
            .fastForEach { workoutLift ->
                workoutLift.sets.fastForEach { set ->
                    val result = resultsByLift["${workoutLift.liftId}-${set.setPosition}"]
                    if (result != null &&
                        ((set.completedReps ?: -1) < set.repRangeBottom ||
                                (set.completedRpe ?: -1f) > set.rpeTarget)) {
                        val lpResults = result as LinearProgressionSetResultDto
                        setResultsToUpdate.add(
                            lpResults.copy(
                                missedLpGoals = lpResults.missedLpGoals + 1
                            )
                        )
                    } else if (result != null && (result as LinearProgressionSetResultDto).missedLpGoals > 0) {
                        setResultsToUpdate.add(
                            result.copy(
                                missedLpGoals = 0
                            )
                        )
                    }
                }
            }

        if (setResultsToUpdate.isNotEmpty()) {
            setResultsRepository.upsertMany(setResultsToUpdate)
        }
    }

    fun updateRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) {
        executeInTransactionScope {
            _state.update { currentState ->
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

    fun deleteMyoRepSet(workoutLiftId: Long, setPosition: Int, myoRepSetPosition: Int) {
        executeInTransactionScope {
            val workoutLift = _state.value.workout!!.lifts.find { it.id == workoutLiftId }!!
            val liftId = workoutLift.liftId
            val isComplete = workoutLift.sets.find {
                it is LoggingMyoRepSetDto &&
                    it.setPosition == setPosition &&
                    it.myoRepSetPosition == myoRepSetPosition
            }?.complete

            if (isComplete == true) {
                setResultsRepository.delete(
                    workoutId = _state.value.workout!!.id,
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