package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.copyGeneric
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toTimeString
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.ui.viewmodels.states.EditWorkoutState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.Integer.max

class EditWorkoutViewModel(
    private val workoutLogEntryId: Long,
    private val loggingRepository: LoggingRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
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
                        lift.sets.associateBy { set -> "${set.position}-${(set as? LoggingMyoRepSetDto)?.myoRepSetPosition}" }
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
            val workoutLog = loggingRepository.get(workoutLogEntryId = workoutLogEntryId)
            val previousResults = loggingRepository.getMostRecentSetResultsForLiftIdsPriorToDate(
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

            _editWorkoutState.update {
                it.copy(
                    duration = workoutLog.durationInMillis.toTimeString(),
                    setResults = setResultsRepository.getForWorkout(
                        workoutId = workoutLog.workoutId,
                        mesoCycle = workoutLog.mesocycle,
                        microCycle = workoutLog.microcycle
                    )
                )
            }
            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    programMetadata = ActiveProgramMetadataDto(
                        programId = 0L,
                        name = "",
                        deloadWeek = workoutLog.programDeloadWeek,
                        workoutCount = workoutLog.programWorkoutCount,
                        currentMesocycle = workoutLog.mesocycle,
                        currentMicrocycle = workoutLog.microcycle,
                        currentMicrocyclePosition = workoutLog.microcyclePosition,
                    ),
                    inProgressWorkout = WorkoutInProgressDto(
                        workoutId = workoutLogEntryId,
                        startTime = getCurrentDate(),
                        completedSets = completedSetResults,
                    )
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

    private fun buildLoggingWorkoutFromWorkoutLogs(workoutLog: WorkoutLogEntryDto, previousResults: List<SetResult>): LoggingWorkoutDto {
        val previousSetResults = previousResults
            .groupBy { it.liftId }
            .entries
            .associate {  lift ->
                lift.key to
                        lift.value.associateBy { "${it.setPosition}-${(it as? MyoRepSetResultDto)?.myoRepSetPosition}" }
            }
        var fauxWorkoutLiftId = 0L
        return LoggingWorkoutDto(
            id = workoutLog.workoutId,
            name = workoutLog.workoutName,
            lifts = workoutLog.setResults.groupBy { it.liftPosition }.map { groupedResults ->
                fauxWorkoutLiftId++
                val lift = groupedResults.value[0]
                LoggingWorkoutLiftDto(
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
                            compareBy<SetLogEntryDto> { it.setPosition }
                                .thenBy { it.myoRepSetPosition ?: -1 }
                        ).fastMap { setLogEntry ->
                        when (setLogEntry.setType) {
                            SetType.STANDARD ->
                                LoggingStandardSetDto(
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
                                LoggingMyoRepSetDto(
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
                                LoggingDropSetDto(
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

    private fun buildCompletedSetResultsFromLog(workoutLog: WorkoutLogEntryDto): List<SetResult> {
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

        return loggingRepository.upsertMany(
            workoutLogEntryId = workoutLogEntryId,
            updatedResults.fastMap { setResult ->
                getSetLogEntryFromSetResult(setResult = setResult)
            }
        )
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        if (_editWorkoutState.value.setResults.isNotEmpty()) {
            updateSetResult(updatedResult = updatedResult)
        }
        return loggingRepository.upsert(
            workoutLogEntryId = workoutLogEntryId,
            getSetLogEntryFromSetResult(setResult = updatedResult),
        )
    }

    override suspend fun deleteSetResult(id: Long) {
        loggingRepository.deleteSetLogEntryById(id)
    }

    private suspend fun updateSetResult(updatedResult: SetResult) {
        // The id on updatedResult is for setLogEntry so you can't just call upsert
        val resultToUpsert = _setResultsByPosition["${updatedResult.liftPosition}-${updatedResult.setPosition}"]
            ?.let { prevSetResult ->
                when (prevSetResult) {
                    is StandardSetResultDto -> prevSetResult.copy(
                        reps = updatedResult.reps,
                        weight = updatedResult.weight,
                        rpe = updatedResult.rpe,
                    )

                    is MyoRepSetResultDto -> prevSetResult.copy(
                        reps = updatedResult.reps,
                        weight = updatedResult.weight,
                        rpe = updatedResult.rpe,
                    )

                    is LinearProgressionSetResultDto -> prevSetResult.copy(
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
                is MyoRepSetResultDto -> updatedResult.copy(id = id)
                is StandardSetResultDto -> updatedResult.copy(id = id)
                is LinearProgressionSetResultDto -> updatedResult.copy(id = id)
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
                            newSet = lastSet.copyGeneric(position = lastSet.position + 1)
                            add(newSet!!)
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
                    restTime = 0L, restTimerEnabled = false, result = buildSetResult(
                        setType = when (newSet) {
                            is LoggingStandardSetDto -> SetType.STANDARD
                            is LoggingDropSetDto -> SetType.DROP_SET
                            is LoggingMyoRepSetDto -> SetType.MYOREP
                            else -> throw Exception("${newSet!!::class.simpleName} is not defined")
                        },
                        liftId = workoutLiftWithNewSet.liftId,
                        progressionScheme = workoutLiftWithNewSet.progressionScheme,
                        liftPosition = workoutLiftWithNewSet.position,
                        setPosition = newSet!!.position,
                        myoRepSetPosition = (newSet as? LoggingMyoRepSetDto)?.myoRepSetPosition,
                        weight = newSet!!.completedWeight ?: 0f,
                        reps = newSet!!.completedReps ?: 0,
                        rpe = newSet!!.completedRpe ?: 8f,
                    )
                )
            }
        }
    }

    private fun getSet(liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?): GenericLoggingSet {
        val setsForLift = _setsByPosition[liftPosition]!!
        return setsForLift["$setPosition-$myoRepSetPosition"]!!
    }

    private fun getSetLogEntryFromSetResult(setResult: SetResult): SetLogEntryDto {
        val lift = _liftsById[setResult.liftId]!!
        val set = getSet(
            liftPosition = setResult.liftPosition,
            setPosition = setResult.setPosition,
            myoRepSetPosition = (setResult as? MyoRepSetResultDto)?.myoRepSetPosition
        )

        return SetLogEntryDto(
            id = setResult.id,
            liftId = setResult.liftId,
            liftName = lift.liftName,
            liftMovementPattern = lift.liftMovementPattern,
            progressionScheme = lift.progressionScheme,
            setType = setResult.setType,
            liftPosition = setResult.liftPosition,
            setPosition = setResult.setPosition,
            myoRepSetPosition = (setResult as? MyoRepSetResultDto)?.myoRepSetPosition,
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