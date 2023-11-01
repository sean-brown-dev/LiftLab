package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.repositories.LoggingRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.Integer.max

class EditWorkoutViewModel(
    private val loggingRepository: LoggingRepository,
    private val workoutLogEntryId: Long,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    transactionScope = transactionScope,
    eventBus = eventBus,
) {
    private val _liftsById by lazy {
        mutableState.value.workout!!.lifts.associateBy { it.liftId }
    }

    private val _setsByPosition by lazy {
        mutableState.value.workout!!.lifts
            .associate { lift ->
                lift.position to
                        lift.sets.associateBy { set -> set.position }
            }
    }

    init {
        viewModelScope.launch {
            mutableState.update { currentState ->
                val workoutLog = loggingRepository.get(workoutLogEntryId = workoutLogEntryId)
                val previousWorkoutLog = loggingRepository.getFirstPriorToDate(
                    historicalWorkoutNameId = workoutLog!!.historicalWorkoutNameId,
                    date = workoutLog.date
                )
                val workout = buildLoggingWorkoutFromWorkoutLogs(workoutLog = workoutLog, previousWorkoutLog = previousWorkoutLog)
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
                        completedSets = listOf(),
                    )
                )
            }
        }
    }

    private fun buildLoggingWorkoutFromWorkoutLogs(workoutLog: WorkoutLogEntryDto, previousWorkoutLog: Any): LoggingWorkoutDto {
        return LoggingWorkoutDto(
            id = workoutLog.workoutId,
            name = workoutLog.workoutName,
            lifts = workoutLog.setResults.groupBy { it.liftPosition }.map { groupedResults ->
                val lift = groupedResults.value[0]
                LoggingWorkoutLiftDto(
                    id = -1L,
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
                                    previousSetResultLabel = "",
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
                                    weightRecommendation = null,
                                    previousSetResultLabel = "",
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
                                    weightRecommendation = null,
                                    previousSetResultLabel = "",
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

    private fun buildInProgressWorkoutFromWorkoutLog(workoutLog: WorkoutLogEntryDto): WorkoutInProgressDto {
        return WorkoutInProgressDto(
            workoutId = workoutLogEntryId,
            startTime = Utils.getCurrentDate(),
            completedSets = workoutLog.setResults.fastMap { setLogEntry ->
                super.buildSetResult(
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
        )
    }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> {
        return loggingRepository.upsertMany(
            workoutLogEntryId = workoutLogEntryId,
            updatedResults.fastMap { setResult ->
                getSetLogEntryFromSetResult(setResult = setResult)
            }
        )
    }
    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
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

    private fun getSet(liftPosition: Int, setPosition: Int): GenericLoggingSet {
        val setsForLift = _setsByPosition[liftPosition]!!
        return setsForLift[setPosition]!!
    }

    private fun getSetLogEntryFromSetResult(setResult: SetResult): SetLogEntryDto {
        val lift = _liftsById[setResult.liftId]!!
        val set = getSet(setResult.liftPosition, setResult.liftPosition)

        return SetLogEntryDto(
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
            mesoCycle = mutableState.value.programMetadata!!.currentMesocycle,
            microCycle = mutableState.value.programMetadata!!.currentMesocycle!!,
        )
    }

}