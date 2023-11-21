package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.Utils
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
            val previousWorkoutLog = loggingRepository.getFirstPriorToDate(
                historicalWorkoutNameId = workoutLog!!.historicalWorkoutNameId,
                date = workoutLog.date
            )
            val workout = buildLoggingWorkoutFromWorkoutLogs(workoutLog = workoutLog, previousWorkoutLog = previousWorkoutLog)
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
                    workout = workout,
                    inProgressWorkout = WorkoutInProgressDto(
                        workoutId = workoutLogEntryId,
                        startTime = Utils.getCurrentDate(),
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
        previousSetResults: Map<String, Map<Int, SetLogEntryDto>>?,
        liftId: Long,
        liftPosition: Int,
        setPosition: Int,
    ): String {
        val result = previousSetResults?.get("${liftId}-${liftPosition}")?.get(setPosition)
        return if (result != null) {
            "${result.weight.toString().removeSuffix(".0")}x${result.reps} @${result.rpe}"
        } else {
            "—"
        }
    }

    private fun buildLoggingWorkoutFromWorkoutLogs(workoutLog: WorkoutLogEntryDto, previousWorkoutLog: WorkoutLogEntryDto?): LoggingWorkoutDto {
        val previousSetResults = previousWorkoutLog?.setResults
            ?.groupBy { "${it.liftId}-${it.liftPosition}" }
            ?.entries
            ?.associate {  lift ->
                lift.key to
                        lift.value.associateBy { it.setPosition }
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
                    sets = groupedResults.value.fastMap { setLogEntry ->
                        when (setLogEntry.setType) {
                            SetType.STANDARD ->
                                LoggingStandardSetDto(
                                    position = setLogEntry.setPosition,
                                    repRangeTop = setLogEntry.repRangeTop,
                                    repRangeBottom = setLogEntry.repRangeBottom,
                                    rpeTarget = setLogEntry.rpeTarget,
                                    weightRecommendation = setLogEntry.weightRecommendation,
                                    hadInitialWeightRecommendation = setLogEntry.weightRecommendation != null,
                                    previousSetResultLabel = getPreviousSetResultLabel(
                                        previousSetResults = previousSetResults,
                                        liftId = lift.liftId,
                                        liftPosition = lift.liftPosition,
                                        setPosition = setLogEntry.setPosition,
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
                                        liftPosition = lift.liftPosition,
                                        setPosition = setLogEntry.setPosition,
                                    ),
                                    repRangePlaceholder = "${setLogEntry.repRangeBottom}-${setLogEntry.repRangeTop}",
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
                                    repRangeTop = setLogEntry.repRangeTop,
                                    repRangeBottom = setLogEntry.repRangeBottom,
                                    rpeTarget = setLogEntry.rpeTarget,
                                    weightRecommendation = setLogEntry.weightRecommendation,
                                    hadInitialWeightRecommendation = setLogEntry.weightRecommendation != null,
                                    previousSetResultLabel = getPreviousSetResultLabel(
                                        previousSetResults = previousSetResults,
                                        liftId = lift.liftId,
                                        liftPosition = lift.liftPosition,
                                        setPosition = setLogEntry.setPosition,
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
        )
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

    private suspend fun updateSetResult(updatedResult: SetResult) {
        // The id on updatedResult is for setLogEntry so you can't just call upsert
        val exists = _setResultsByPosition["${updatedResult.liftPosition}-${updatedResult.setPosition}"] != null
        if (exists) {
            setResultsRepository.update(
                liftId = updatedResult.liftId,
                liftPosition = updatedResult.liftPosition,
                setPosition = updatedResult.setPosition,
                myoRepSetPosition = (updatedResult as? MyoRepSetResultDto)?.myoRepSetPosition,
                weight = updatedResult.weight,
                reps = updatedResult.reps,
                rpe = updatedResult.rpe,
            )
            updatedResult.id
        } else {
            val newId = setResultsRepository.upsert(updatedResult)
            val resultWithId = when (updatedResult) {
                is MyoRepSetResultDto -> updatedResult.copy(id = newId)
                is StandardSetResultDto -> updatedResult.copy(id = newId)
                is LinearProgressionSetResultDto -> updatedResult.copy(id = newId)
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
        )
    }

}