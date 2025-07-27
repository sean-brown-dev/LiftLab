package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.copyGeneric
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.EditWorkoutState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.Integer.max

class EditWorkoutViewModel(
    private val workoutLogEntryId: Long,
    private val workoutLogRepository: WorkoutLogRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val setLogEntryRepository: SetLogEntryRepository,
    private val onNavigateBack: () -> Unit,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    transactionScope = transactionScope,
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
        executeInTransactionScope {
            val workoutLog = workoutLogRepository.get(workoutLogEntryId = workoutLogEntryId)
            val previousResults = workoutLogRepository.getMostRecentSetResultsForLiftIdsPriorToDate(
                liftIds = workoutLog!!.setResults.map { it.liftId },
                linearProgressionLiftIds = workoutLog.setResults
                    .filter { it.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION }
                    .map { it.liftId }
                    .toSet(),
                date = workoutLog.date,
            )
            val workout = buildLoggingWorkoutFromWorkoutLogs(workoutLog = workoutLog, previousResults = previousResults)
            mutableWorkoutState.update {
                it.copy(workout = workout)
            }
            val completedSetResults = buildCompletedSetResultsFromLog(workoutLog = workoutLog)
            setResultsRepository.getForWorkoutFlow(
                workoutId = workoutLog.workoutId,
                mesoCycle = workoutLog.mesocycle,
                microCycle = workoutLog.microcycle
            ).collect { setResults ->
                _editWorkoutState.update {
                    it.copy(
                        duration = workoutLog.durationInMillis.toTimeString(),
                        setResults = setResults
                    )
                }
            }

            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    programMetadata = ActiveProgramMetadata(
                        programId = 0L,
                        name = "",
                        deloadWeek = workoutLog.programDeloadWeek,
                        workoutCount = workoutLog.programWorkoutCount,
                        currentMesocycle = workoutLog.mesocycle,
                        currentMicrocycle = workoutLog.microcycle,
                        currentMicrocyclePosition = workoutLog.microcyclePosition,
                    ),
                    inProgressWorkout = WorkoutInProgressUiModel(
                        startTime = getCurrentDate(),
                    ),
                    completedSets = completedSetResults,
                )
            }
        }
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> {
                executeInTransactionScope {
                    updateLinearProgressionFailures()
                }
                onNavigateBack()
            }
            else -> {}
        }
    }

    private fun getPreviousSetResultLabel(
        previousSetResults: Map<Long, Map<String, SetResult>>,
        liftId: Long,
        setPosition: Int,
        myoRepSetPosition: Int?,
    ): String {
        val result = previousSetResults[liftId]?.get("$setPosition-$myoRepSetPosition")
        return if (result != null) {
            "${result.weight.toString().removeSuffix(".0")}x${result.reps} @${result.rpe}"
        } else {
            "—"
        }
    }

    private fun buildLoggingWorkoutFromWorkoutLogs(workoutLog: WorkoutLogEntry, previousResults: List<SetResult>): LoggingWorkout {
        val previousSetResults = previousResults
            .groupBy { it.liftId }
            .entries
            .associate {  lift ->
                lift.key to
                        lift.value.associateBy { "${it.setPosition}-${(it as? MyoRepSetResult)?.myoRepSetPosition}" }
            }
        var fauxWorkoutLiftId = 0L
        return LoggingWorkout(
            id = workoutLog.workoutId,
            name = workoutLog.workoutName,
            lifts = workoutLog.setResults.groupBy { it.liftPosition }.map { groupedResults ->
                fauxWorkoutLiftId++
                val lift = groupedResults.value[0]
                LoggingWorkoutLift(
                    id = fauxWorkoutLiftId,
                    liftId = lift.liftId,
                    liftName = lift.liftName,
                    liftMovementPattern = lift.liftMovementPattern,
                    liftVolumeTypes = 0,
                    liftSecondaryVolumeTypes = null,
                    position = lift.liftPosition,
                    setCount = groupedResults.value.size,
                    progressionScheme = lift.progressionScheme,
                    deloadWeek = max(lift.workoutLiftDeloadWeek ?: 0, workoutLog.programDeloadWeek),
                    incrementOverride = null,
                    restTime = null,
                    restTimerEnabled = false,
                    note = null,
                    sets = groupedResults.value
                        .sortedWith(
                            compareBy<SetLogEntry> { it.setPosition }
                                .thenBy { it.myoRepSetPosition ?: -1 }
                        ).fastMap { setLogEntry ->
                        when (setLogEntry.setType) {
                            SetType.STANDARD ->
                                LoggingStandardSet(
                                    position = setLogEntry.setPosition,
                                    repRangeTop = setLogEntry.repRangeTop!!,
                                    repRangeBottom = setLogEntry.repRangeBottom!!,
                                    rpeTarget = setLogEntry.rpeTarget,
                                    weightRecommendation = setLogEntry.weightRecommendation,
                                    hadInitialWeightRecommendation = setLogEntry.weightRecommendation != null,
                                    previousSetResultLabel = getPreviousSetResultLabel(
                                        previousSetResults = previousSetResults,
                                        liftId = lift.liftId,
                                        setPosition = setLogEntry.setPosition,
                                        myoRepSetPosition = setLogEntry.myoRepSetPosition,
                                    ),
                                    repRangePlaceholder = "${setLogEntry.repRangeBottom}-${setLogEntry.repRangeTop}",
                                    complete = true,
                                    completedWeight = setLogEntry.weight,
                                    completedReps = setLogEntry.reps,
                                    completedRpe = setLogEntry.rpe,
                                )
                            SetType.MYOREP ->
                                LoggingMyoRepSet(
                                    position = setLogEntry.setPosition,
                                    myoRepSetPosition = setLogEntry.myoRepSetPosition,
                                    repRangeTop = setLogEntry.repRangeTop,
                                    repRangeBottom = setLogEntry.repRangeBottom,
                                    rpeTarget = setLogEntry.rpeTarget,
                                    weightRecommendation = setLogEntry.weightRecommendation,
                                    hadInitialWeightRecommendation = setLogEntry.weightRecommendation != null,
                                    previousSetResultLabel = getPreviousSetResultLabel(
                                        previousSetResults = previousSetResults,
                                        liftId = lift.liftId,
                                        setPosition = setLogEntry.setPosition,
                                        myoRepSetPosition = setLogEntry.myoRepSetPosition,
                                    ),
                                    repRangePlaceholder = if (setLogEntry.repRangeBottom != null && setLogEntry.repRangeTop != null) {
                                        "${setLogEntry.repRangeBottom}-${setLogEntry.repRangeTop}"
                                    } else "",
                                    setMatching = setLogEntry.setMatching!!,
                                    maxSets = setLogEntry.maxSets,
                                    repFloor = setLogEntry.repFloor,
                                    complete = true,
                                    completedWeight = setLogEntry.weight,
                                    completedReps = setLogEntry.reps,
                                    completedRpe = setLogEntry.rpe,
                                )
                            SetType.DROP_SET ->
                                LoggingDropSet(
                                    position = setLogEntry.setPosition,
                                    repRangeTop = setLogEntry.repRangeTop!!,
                                    repRangeBottom = setLogEntry.repRangeBottom!!,
                                    rpeTarget = setLogEntry.rpeTarget,
                                    weightRecommendation = setLogEntry.weightRecommendation,
                                    hadInitialWeightRecommendation = setLogEntry.weightRecommendation != null,
                                    previousSetResultLabel = getPreviousSetResultLabel(
                                        previousSetResults = previousSetResults,
                                        liftId = lift.liftId,
                                        setPosition = setLogEntry.setPosition,
                                        myoRepSetPosition = setLogEntry.myoRepSetPosition,
                                    ),
                                    repRangePlaceholder = "${setLogEntry.repRangeBottom}-${setLogEntry.repRangeTop}",
                                    dropPercentage = setLogEntry.dropPercentage!!,
                                    complete = true,
                                    completedWeight = setLogEntry.weight,
                                    completedReps = setLogEntry.reps,
                                    completedRpe = setLogEntry.rpe,
                                )
                        }
                    }
                )
            }
        ).let { unsortedWorkout ->
            // Sort lifts & sets by position
            unsortedWorkout.copy(
                lifts = unsortedWorkout.lifts
                    .sortedBy { unsortedLift -> unsortedLift.position }
                    .map { sortedLift ->
                        sortedLift.copy(
                            sets = sortedLift.sets.sortedBy { unsortedSet -> unsortedSet.position }
                        )
                    }
            )
        }
    }

    private fun buildCompletedSetResultsFromLog(workoutLog: WorkoutLogEntry): List<SetResult> {
        return workoutLog.setResults.fastMap { setLogEntry ->
            super.buildSetResult(
                id = setLogEntry.id,
                workoutId = workoutLog.workoutId,
                currentMesocycle = workoutLog.mesocycle,
                currentMicrocycle = workoutLog.microcycle,
                weightRecommendation = setLogEntry.weightRecommendation,
                liftId = setLogEntry.liftId,
                setType = setLogEntry.setType,
                progressionScheme = setLogEntry.progressionScheme,
                liftPosition = setLogEntry.liftPosition,
                setPosition = setLogEntry.setPosition,
                myoRepSetPosition = setLogEntry.myoRepSetPosition,
                weight = setLogEntry.weight,
                reps = setLogEntry.reps,
                rpe = setLogEntry.rpe,
            )
        }
    }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> {
        if (_editWorkoutState.value.setResults.isNotEmpty()) {
            updatedResults.fastMap { setResult ->
                updateSetResult(updatedResult = setResult)
            }
        }

        return setLogEntryRepository.upsertMany(
            updatedResults.fastMap { setResult ->
                getSetLogEntryFromSetResult(setResult = setResult)
            }
        )
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        if (_editWorkoutState.value.setResults.isNotEmpty()) {
            updateSetResult(updatedResult = updatedResult)
        }
        return setLogEntryRepository.upsert(
            getSetLogEntryFromSetResult(setResult = updatedResult),
        )
    }

    override suspend fun deleteSetResult(id: Long) {
        setLogEntryRepository.deleteById(id)
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

        val id = setResultsRepository.upsert(resultToUpsert)

        // If it was a new result
        if (updatedResult.id != id) {
            val resultWithId = when (updatedResult) {
                is MyoRepSetResult -> updatedResult.copy(id = id)
                is StandardSetResult -> updatedResult.copy(id = id)
                is LinearProgressionSetResult -> updatedResult.copy(id = id)
                else -> throw Exception("${updatedResult::class.simpleName} is not defined.")
            }
            _editWorkoutState.update {
                it.copy(
                    setResults = it.setResults.toMutableList().apply {
                        add(resultWithId)
                    }
                )
            }
        }
    }

    public override suspend fun updateLinearProgressionFailures() {
        super.updateLinearProgressionFailures()
    }

    fun addSet(workoutLiftId: Long) {
        executeInTransactionScope {
            var newSet: GenericLoggingSet? = null
            val workoutLiftWithNewSet = mutableWorkoutState.value.workout?.lifts
                ?.find { it.id == workoutLiftId }
                ?.let { workoutLift ->
                    workoutLift.copy(
                        sets = workoutLift.sets.toMutableList().apply {
                            val lastSet = last()
                            newSet = lastSet.copyGeneric(
                                position = lastSet.position + 1,
                                myoRepSetPosition = (lastSet as? LoggingMyoRepSet)?.myoRepSetPosition?.let {
                                    it + 1
                                },
                            )
                            add(newSet)
                        }.toList()
                    )
                }

            if (newSet != null && workoutLiftWithNewSet != null) {
                mutableWorkoutState.update {
                    it.copy(
                        workout = it.workout?.copy(
                            lifts = it.workout.lifts.fastMap { workoutLift ->
                                if (workoutLift.id == workoutLiftId)
                                    workoutLiftWithNewSet
                                else workoutLift
                            }
                        ))
                }

                completeSet(
                    restTime = 0L, restTimerEnabled = false) {
                    buildSetResult(
                        setType = when (newSet) {
                            is LoggingStandardSet -> SetType.STANDARD
                            is LoggingDropSet -> SetType.DROP_SET
                            is LoggingMyoRepSet -> SetType.MYOREP
                            else -> throw Exception("${newSet!!::class.simpleName} is not defined")
                        },
                        liftId = workoutLiftWithNewSet.liftId,
                        progressionScheme = workoutLiftWithNewSet.progressionScheme,
                        liftPosition = workoutLiftWithNewSet.position,
                        setPosition = newSet.position,
                        myoRepSetPosition = (newSet as? LoggingMyoRepSet)?.myoRepSetPosition,
                        weight = newSet.completedWeight ?: 0f,
                        reps = newSet.completedReps ?: 0,
                        rpe = newSet.completedRpe ?: 8f,
                    )
                }
            }
        }
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

        return SetLogEntry(
            id = setResult.id,
            workoutLogEntryId = workoutLogEntryId,
            liftId = setResult.liftId,
            liftName = lift.liftName,
            liftMovementPattern = lift.liftMovementPattern,
            progressionScheme = lift.progressionScheme,
            setType = setResult.setType,
            liftPosition = setResult.liftPosition,
            setPosition = setResult.setPosition,
            myoRepSetPosition = (setResult as? MyoRepSetResult)?.myoRepSetPosition,
            repRangeTop = set.repRangeTop,
            repRangeBottom = set.repRangeBottom,
            rpeTarget = set.rpeTarget,
            weightRecommendation = set.weightRecommendation,
            weight = setResult.weight,
            reps = setResult.reps,
            rpe = setResult.rpe,
            mesoCycle = mutableWorkoutState.value.programMetadata!!.currentMesocycle,
            microCycle = mutableWorkoutState.value.programMetadata!!.currentMesocycle,
            isDeload = setResult.isDeload,
        )
    }

}