package com.browntowndev.liftlab.ui.viewmodels

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.RestTimerInProgressDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
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
import com.browntowndev.liftlab.core.progression.ProgressionFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.Integer.max
import kotlin.time.Duration

class WorkoutViewModel(
    private val progressionFactory: ProgressionFactory,
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository,
    private val loggingRepository: LoggingRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val liftsRepository: LiftsRepository,
    private val navigateToWorkoutHistory: () -> Unit,
    private val cancelRestTimer: () -> Unit,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    transactionScope = transactionScope,
    eventBus = eventBus,
) {
    private var _restTimerLiveData: LiveData<RestTimerInProgressDto?>? = null
    private var _restTimerObserver: Observer<RestTimerInProgressDto?>? = null
    private var _programLiveData: LiveData<ActiveProgramMetadataDto?>? = null
    private var _programObserver: Observer<ActiveProgramMetadataDto?>? = null
    private var _workoutLiveData: LiveData<LoggingWorkoutDto?>? = null
    private var _workoutObserver: Observer<LoggingWorkoutDto?>? = null

    init {
        initialize()
    }

    private fun initialize() {
        _restTimerObserver = Observer { restTimerInProgress ->
            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    restTimerStartedAt = restTimerInProgress?.timeStartedInMillis?.toDate(),
                    restTime = restTimerInProgress?.restTime ?: 0L,
                )
            }
        }
        _restTimerLiveData = restTimerInProgressRepository.getLive()
        _restTimerLiveData!!.observeForever(_restTimerObserver!!)

        _programObserver = Observer { programMetadata ->
            if (programMetadata != null) {
                viewModelScope.launch {
                    _workoutLiveData?.removeObserver(_workoutObserver!!)
                    _workoutObserver = Observer { workout ->
                        executeInTransactionScope {
                            val inProgressWorkout = workoutInProgressRepository.get(
                                programMetadata.currentMesocycle,
                                programMetadata.currentMicrocycle
                            )

                            mutableWorkoutState.update { currentState ->
                                currentState.copy(
                                    inProgressWorkout = inProgressWorkout,
                                    programMetadata = programMetadata,
                                    workout = workout,
                                    initialized = true,
                                )
                            }
                        }
                    }

                    _workoutLiveData = getNextToPerform(programMetadata)
                    _workoutLiveData!!.observeForever(_workoutObserver!!)
                }
            } else {
                mutableWorkoutState.update {
                    it.copy(initialized = true)
                }
            }
        }
        _programLiveData = programsRepository.getActiveProgramMetadata()
        _programLiveData!!.observeForever(_programObserver!!)
    }

    override fun onCleared() {
        super.onCleared()

        _restTimerLiveData?.removeObserver(_restTimerObserver!!)
        _programLiveData?.removeObserver(_programObserver!!)
        _workoutLiveData?.removeObserver(_workoutObserver!!)
    }

    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> mutableWorkoutState.update {
                it.copy(workoutLogVisible = false)
            }
            TopAppBarAction.RestTimerCompleted -> {
                executeInTransactionScope {
                    restTimerInProgressRepository.deleteAll()
                    mutableWorkoutState.update {
                        it.copy(restTimerStartedAt = null)
                    }
                }
            }
            TopAppBarAction.FinishWorkout -> toggleConfirmFinishWorkoutModal()
            TopAppBarAction.OpenWorkoutHistory -> navigateToWorkoutHistory()
            else -> {}
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getNextToPerform(
        programMetadata: ActiveProgramMetadataDto,
    ): LiveData<LoggingWorkoutDto?> {
        return workoutsRepository.getByMicrocyclePosition(
            programId = programMetadata.programId,
            microcyclePosition = programMetadata.currentMicrocyclePosition
        ).flatMapLatest { workout ->
            getSetResults(workout, programMetadata)
                .flatMapLatest { previousSetResults ->
                    SettingsManager.getSettingFlow(
                        SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
                        SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
                    ).flatMapLatest { onlyUseResultsForLiftsInSamePosition ->
                        flowOf(
                            if (workout != null) {
                                val inProgressSetResults = setResultsRepository.getForWorkout(
                                    workoutId = workout.id,
                                    mesoCycle = programMetadata.currentMesocycle,
                                    microCycle = programMetadata.currentMicrocycle
                                ).associateBy { result ->
                                    "${result.liftId}-${result.setPosition}-${(result as? MyoRepSetResultDto)?.myoRepSetPosition}"
                                }

                                val previousResultsForDisplay = getNewestResultsFromOtherWorkouts(
                                    liftIdsToSearchFor = workout.lifts.map { it.liftId },
                                    workout,
                                    listOf(),
                                    includeDeload = true,
                                )

                                progressionFactory.calculate(
                                    workout = workout,
                                    previousSetResults = previousSetResults,
                                    previousResultsForDisplay = previousResultsForDisplay,
                                    inProgressSetResults = inProgressSetResults,
                                    programDeloadWeek = programMetadata.deloadWeek,
                                    microCycle = programMetadata.currentMicrocycle,
                                    onlyUseResultsForLiftsInSamePosition = onlyUseResultsForLiftsInSamePosition,
                                )
                            } else null
                        )
                    }
                }
        }.asLiveData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getSetResults(
        workout: WorkoutDto?,
        programMetadata: ActiveProgramMetadataDto
    ): Flow<List<SetResult>> {
        return SettingsManager.getSettingFlow(
            SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
            SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA
        ).flatMapLatest { useAllData ->
            flowOf(
                if (workout != null) {
                    val resultsFromLastWorkout =
                        setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicro(
                            workoutId = workout.id,
                            mesoCycle = programMetadata.currentMesocycle,
                            microCycle = programMetadata.currentMicrocycle,
                        )

                    if (useAllData) {
                        getResultsWithAllWorkoutDataAppended(resultsFromLastWorkout, workout)
                    } else resultsFromLastWorkout
                } else listOf()
            )
        }
    }

    private suspend fun getResultsWithAllWorkoutDataAppended(
        resultsFromLastWorkout: List<SetResult>,
        workout: WorkoutDto,
    ): List<SetResult> {
        val liftIdsOfResults = resultsFromLastWorkout.map { it.liftId }.toHashSet()
        val liftIdsToSearchFor = workout.lifts
            .filter { !liftIdsOfResults.contains(it.liftId) }
            .map { workoutLift -> workoutLift.liftId }

        return getNewestResultsFromOtherWorkouts(
            liftIdsToSearchFor,
            workout,
            resultsFromLastWorkout,
            includeDeload = false,
        )
    }

    private suspend fun getNewestResultsFromOtherWorkouts(
        liftIdsToSearchFor: List<Long>,
        workout: WorkoutDto,
        existingResults: List<SetResult>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return if (liftIdsToSearchFor.isNotEmpty()) {
            val linearProgressionLiftIds = workout.lifts
                .filter {
                    it.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION
                }.map { it.liftId }
                .toHashSet()

            existingResults.toMutableList().apply {
                val resultsFromOtherWorkouts =
                    loggingRepository.getMostRecentSetResultsForLiftIds(
                        liftIds = liftIdsToSearchFor,
                        linearProgressionLiftIds = linearProgressionLiftIds,
                        includeDeload = includeDeload,
                    )

                addAll(resultsFromOtherWorkouts)
            }
        } else existingResults
    }

    fun toggleReorderLifts() {
        mutableWorkoutState.update {
            it.copy(isReordering = !it.isReordering)
        }
    }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) {
        executeInTransactionScope {
            val newLiftIndices = newLiftOrder
                .mapIndexed { index, item -> item.key to index }
                .associate { it.first to it.second }

            val updatedWorkoutCopy = mutableWorkoutState.value.workout!!.copy(
                lifts = mutableWorkoutState.value.workout!!.lifts.map { lift ->
                    lift.copy(position = newLiftIndices[lift.id]!!)
                }
            )

            val updatedLifts = workoutLiftsRepository.getForWorkout(mutableWorkoutState.value.workout!!.id)
                .map {
                    when (it) {
                        is StandardWorkoutLiftDto -> it.copy(position = newLiftIndices[it.id]!!)
                        is CustomWorkoutLiftDto -> it.copy(position = newLiftIndices[it.id]!!)
                        else -> throw Exception("${it::class.simpleName} is not defined.")
                    }
                }

            workoutLiftsRepository.updateMany(updatedLifts)
            mutableWorkoutState.update { it.copy(workout = updatedWorkoutCopy, isReordering = false) }
        }
    }

    fun setWorkoutLogVisibility(visible: Boolean) {
        mutableWorkoutState.update {
            it.copy(
                workoutLogVisible = visible
            )
        }
    }

    fun startWorkout() {
        executeInTransactionScope {
            val inProgressWorkout = WorkoutInProgressDto(
                startTime = Utils.getCurrentDate(),
                workoutId = mutableWorkoutState.value.workout!!.id,
                completedSets = listOf(),
            )
            workoutInProgressRepository.insert(inProgressWorkout)
            mutableWorkoutState.update {
                it.copy(
                    inProgressWorkout = inProgressWorkout,
                    workoutLogVisible = true,
                )
            }
        }
    }

    fun toggleConfirmFinishWorkoutModal() {
        mutableWorkoutState.update {
            it.copy(
                isConfirmFinishWorkoutModalShown = !it.isConfirmFinishWorkoutModalShown
            )
        }
    }

    fun finishWorkout() {
        toggleConfirmFinishWorkoutModal()

        executeInTransactionScope {
            val startTimeInMillis = mutableWorkoutState.value.inProgressWorkout!!.startTime.time
            val durationInMillis = (Utils.getCurrentDate().time - startTimeInMillis)
            val programMetadata = mutableWorkoutState.value.programMetadata!!
            val workout = mutableWorkoutState.value.workout!!

            // Remove the workout from in progress
            workoutInProgressRepository.delete()
            restTimerInProgressRepository.deleteAll()

            // Increment the mesocycle and microcycle
            val microCycleComplete =  (programMetadata.workoutCount - 1) == programMetadata.currentMicrocyclePosition
            val lastDeloadWeek = max(programMetadata.deloadWeek, workout.lifts.maxOfOrNull { it.deloadWeek ?: 0 } ?: 0)
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
                programDeloadWeek = programMetadata.deloadWeek,
                programWorkoutCount = programMetadata.workoutCount,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
                microcyclePosition = programMetadata.currentMicrocyclePosition,
                date = Utils.getCurrentDate(),
                durationInMillis = durationInMillis,
            )

            moveSetResultsToLogHistory(
                workoutLogEntryId = workoutLogEntryId,
                programMetadata = programMetadata,
                workout = workout
            )

            // Update any Linear Progression failures
            // The reason this is done when the workout is completed is because if it were done on the fly
            // you'd have no easy way of knowing if someone failed (increment), changed result (still failure)
            // and then you get double increment. Or any variation of them going between success/failure by
            // modifying results.
            updateLinearProgressionFailures()

            stopRestTimer()

            // TODO: have summary pop up as dialog and close this on completion instead
            mutableWorkoutState.update {
                it.copy(workoutLogVisible = false)
            }
        }
    }

    private suspend fun moveSetResultsToLogHistory(
        workoutLogEntryId: Long,
        programMetadata: ActiveProgramMetadataDto,
        workout: LoggingWorkoutDto
    ) {
        val liftsAndPositions = mutableWorkoutState.value.workout!!.lifts.associate {
            it.liftId to it.position
        }

        // If any lifts were changed and had completed results do not copy them
        val excludeFromCopy =
            mutableWorkoutState.value.inProgressWorkout!!.completedSets.filter { result ->
                val liftPosition = liftsAndPositions[result.liftId]
                liftPosition != result.liftPosition
            }.map {
                it.id
            }

        // Copy all of the set results from this workout into the set history table
        loggingRepository.insertFromPreviousSetResults(
            workoutLogEntryId = workoutLogEntryId,
            workoutId = mutableWorkoutState.value.workout!!.id,
            mesocycle = programMetadata.currentMesocycle,
            microcycle = programMetadata.currentMicrocycle,
            excludeFromCopy = excludeFromCopy,
        )

        // Get all the set results for deloaded lifts
        val deloadSetResults = mutableWorkoutState.value.workout!!.lifts
            .filter { workoutLift ->
                // workout lifts whose deload week it is
                val deloadWeek = (workoutLift.deloadWeek ?: programMetadata.deloadWeek) - 1
                deloadWeek == programMetadata.currentMicrocycle
            }.fastMap {
                // key that can be used to match set results
                "${it.liftId}-${it.position}"
            }.toHashSet().let { deloadedWorkoutLiftIds ->
                // set results for deloaded workout lifts
                mutableWorkoutState.value.inProgressWorkout!!.completedSets
                    .filter { deloadedWorkoutLiftIds.contains("${it.liftId}-${it.liftPosition}") }
                    .fastMap { it.id }
            }

        // Delete all set results from the previous workout OR ones that were deloaded. Deloaded
        // ones are deleted so next progressions are calculated using most recent non-deload results
        setResultsRepository.deleteAllForPreviousWorkout(
            workoutId = workout.id,
            currentMesocycle = programMetadata.currentMesocycle,
            currentMicrocycle = programMetadata.currentMicrocycle,
            currentResultsToDeleteInstead = deloadSetResults,
        )
    }

    fun toggleConfirmCancelWorkoutModal() {
        mutableWorkoutState.update {
            it.copy(
                isConfirmCancelWorkoutModalShown = !it.isConfirmCancelWorkoutModalShown
            )
        }
    }

    fun cancelWorkout() {
        if (mutableWorkoutState.value.isConfirmCancelWorkoutModalShown)
            toggleConfirmCancelWorkoutModal()

        executeInTransactionScope {
            // Remove the workout from in progress
            workoutInProgressRepository.delete()

            // Delete all set results from the workout
            val programMetadata = mutableWorkoutState.value.programMetadata!!
            setResultsRepository.deleteAllForWorkout(
                workoutId = mutableWorkoutState.value.workout!!.id,
                mesoCycle = programMetadata.currentMesocycle,
                microCycle = programMetadata.currentMicrocycle,
            )

            mutableWorkoutState.update {
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
                                                complete = false,
                                            )

                                            is LoggingDropSetDto -> set.copy(
                                                completedWeight = null,
                                                completedReps = null,
                                                completedRpe = null,
                                                complete = false,
                                            )

                                            is LoggingMyoRepSetDto -> set.copy(
                                                completedWeight = null,
                                                completedReps = null,
                                                completedRpe = null,
                                                complete = false,
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
            mutableWorkoutState.update { currentState ->
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
                            } else lift
                        }
                    )
                )
            }
        }
    }

    fun updateNote(workoutLiftId: Long, note: String) {
        executeInTransactionScope {
            workoutLiftsRepository.updateNote(workoutLiftId, note.ifEmpty { null })

            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    workout = currentState.workout!!.copy(
                        lifts = currentState.workout.lifts.fastMap { workoutLift ->
                            if (workoutLift.id == workoutLiftId) {
                                workoutLift.copy(
                                    note = note.ifEmpty { null },
                                )
                            } else workoutLift
                        }
                    )
                )
            }
        }
    }

    fun saveRestTimerInProgress(restTime: Long) {
        executeInTransactionScope {
            insertRestTimerInProgress(restTime)

            mutableWorkoutState.update {
                it.copy(
                    restTime = restTime,
                    restTimerStartedAt = Utils.getCurrentDate(),
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