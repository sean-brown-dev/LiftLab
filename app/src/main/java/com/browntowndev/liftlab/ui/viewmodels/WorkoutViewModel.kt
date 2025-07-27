package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.HistoricalWorkoutName
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.PersonalRecord
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.domain.useCase.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.HydrateLoggingWorkoutWithCompletedSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.progression.CalculateLoggingWorkoutUseCase
import com.browntowndev.liftlab.ui.mapping.WorkoutCompletionSummaryUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutInProgressUiMappingExtensions.toDomainModel
import com.browntowndev.liftlab.ui.mapping.WorkoutInProgressUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration

class WorkoutViewModel(
    private val calculateLoggingWorkoutUseCase: CalculateLoggingWorkoutUseCase,
    private val hydrateLoggingWorkoutWithCompletedSetsUseCase: HydrateLoggingWorkoutWithCompletedSetsUseCase,
    private val hydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase: HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase,
    private val getWorkoutCompletionSummaryUseCase: GetWorkoutCompletionSummaryUseCase,
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val setLogEntryRepository: SetLogEntryRepository,
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
    @Subscribe
    fun handleActionBarEvents(actionEvent: TopAppBarEvent.ActionEvent) {
        when (actionEvent.action) {
            TopAppBarAction.NavigatedBack -> mutableWorkoutState.update {
                if (mutableWorkoutState.value.isCompletionSummaryVisible) {
                    it.copy(isCompletionSummaryVisible = false)
                } else {
                    it.copy(
                        workoutLogVisible = false,
                        isReordering = false,
                    )
                }
            }
            TopAppBarAction.RestTimerCompleted -> {
                try {
                    executeInTransactionScope {
                        restTimerInProgressRepository.deleteAll()
                        mutableWorkoutState.update {
                            it.copy(restTimerStartedAt = null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WorkoutViewModel", "Error handling rest timer completion", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    viewModelScope.launch {
                        showToast("Failed to complete rest timer. Please try again.")
                    }
                }
            }
            TopAppBarAction.FinishWorkout -> if (mutableWorkoutState.value.isCompletionSummaryVisible) {
                finishWorkout()
            } else {
                toggleCompletionSummary()
            }
            TopAppBarAction.OpenWorkoutHistory -> navigateToWorkoutHistory()
            else -> {}
        }
    }

    init {
        initialize()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initialize() {
        val restTimerFlow = restTimerInProgressRepository.getFlow()
        programsRepository.getActiveProgramMetadataFlow()
            .flatMapLatest { programMetadata ->
                if (programMetadata == null) flowOf(WorkoutState())
                else {
                    val workoutInProgressFlow = workoutInProgressRepository.getFlow(
                        programMetadata.currentMesocycle,
                        programMetadata.currentMicrocycle
                    )

                    val loggingWorkoutStateFlow = getNextToPerformFlow(programMetadata)

                    combine(
                        workoutInProgressFlow,
                        loggingWorkoutStateFlow
                    ) { inProgressWorkout, loggingWorkoutState ->
                        WorkoutState(
                            programMetadata = programMetadata,
                            inProgressWorkout = inProgressWorkout?.toUiModel(),
                            completedSets = loggingWorkoutState.completedSets,
                            workout = loggingWorkoutState.workout,
                            personalRecords = loggingWorkoutState.personalRecords,
                        )
                    }
                }
            }.combine(restTimerFlow) { newState, restTimerInProgress ->
                newState.copy(
                    restTimerStartedAt = restTimerInProgress?.timeStartedInMillis?.toDate(),
                    restTime = restTimerInProgress?.restTime ?: 0L,
                )
            }
            .onEach { newState ->
                mutableWorkoutState.update { currentState ->
                    currentState.copy(
                        inProgressWorkout = newState.inProgressWorkout,
                        completedSets = newState.completedSets,
                        programMetadata = newState.programMetadata,
                        workout = newState.workout,
                        personalRecords = newState.personalRecords,
                        restTimerStartedAt = newState.restTimerStartedAt,
                        restTime = newState.restTime,
                        initialized = true,
                    )
                }
            }
            .catch { e ->
                Log.e("WorkoutViewModel", "Error in initialize", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                showToast("An unexpected error occurred during initialization. Please restart the app.")
            }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNextToPerformFlow(
        programMetadata: ActiveProgramMetadata,
    ): Flow<WorkoutState> {
        val useAllWorkoutDataFlow = SettingsManager.getSettingFlow(
            USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS,
            DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
        )
        val useOnlySamePositionFlow = SettingsManager.getSettingFlow(
            ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION,
            DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
        )
        val useLiftSpecificDeloadingFlow = SettingsManager.getSettingFlow(
            LIFT_SPECIFIC_DELOADING,
            DEFAULT_LIFT_SPECIFIC_DELOADING
        )

        return workoutsRepository.getByMicrocyclePosition(
            programId = programMetadata.programId,
            microcyclePosition = programMetadata.currentMicrocyclePosition
        ).flatMapLatest { nullableWorkout ->
            Log.d("WorkoutViewModel", "Workout: $nullableWorkout")
            if (nullableWorkout == null) {
                flowOf(WorkoutState())  // no workoutEntity, short‐circuit
            } else {
                val personalRecords = getPersonalRecords(
                    workoutId = nullableWorkout.id,
                    liftIds = nullableWorkout.lifts.map { it.liftId },
                    mesoCycle = programMetadata.currentMesocycle,
                    microCycle = programMetadata.currentMicrocycle,
                )
                val previousResultsFlow = useAllWorkoutDataFlow.flatMapLatest { useAllData ->
                    getSetResultsFlow(
                        workout = nullableWorkout,
                        programMetadata = programMetadata,
                        useAllData = useAllData,
                    ).distinctUntilChanged()
                }
                val inProgressResultsFlow = setResultsRepository.getForWorkoutFlow(
                    workoutId = nullableWorkout.id,
                    mesoCycle = programMetadata.currentMesocycle,
                    microCycle = programMetadata.currentMicrocycle,
                ).distinctUntilChanged()
                val previousResultsForDisplay = getNewestResultsFromOtherWorkouts(
                    liftIdsToSearchFor = nullableWorkout.lifts.map { it.liftId },
                    workout = nullableWorkout,
                    existingResultsForOtherLifts = emptyList(),
                    includeDeload = true
                )

                // Only do expensive calc when necessary
                combine(
                    useOnlySamePositionFlow,
                    useLiftSpecificDeloadingFlow,
                    previousResultsFlow
                ) { onlySamePos, liftDeloadEnabled, previousResults ->
                    Log.d("WorkoutViewModel", "Calculating workout")
                    calculateLoggingWorkoutUseCase.calculate(
                        workout = nullableWorkout,
                        previousSetResults = previousResults,
                        previousResultsForDisplay = previousResultsForDisplay,
                        programDeloadWeek = programMetadata.deloadWeek,
                        useLiftSpecificDeloading = liftDeloadEnabled,
                        microCycle = programMetadata.currentMicrocycle,
                        onlyUseResultsForLiftsInSamePosition = onlySamePos
                    )
                }.flatMapLatest { calculatedWorkout ->
                    // When expensiveCalcFlow emits, flatMapLatest creates a new flow,
                    // starting the accumulation process from the new `calculatedWorkout`.
                    inProgressResultsFlow.scan(calculatedWorkout to emptyList<SetResult>()) { (accWorkout, _), inProgressResults ->
                        // `scan` accumulates changes. `accWorkout` is the result from the previous run.
                        Log.d("WorkoutViewModel", "Updating workout with in progress results.")

                        val updatedWorkout =
                            hydrateLoggingWorkoutWithCompletedSetsUseCase.hydrateWithInProgressSetResults(
                                loggingWorkout = accWorkout, // Use accumulated workout as base
                                inProgressSetResults = inProgressResults,
                                microCycle = programMetadata.currentMicrocycle,
                            )

                        val finalWorkout = hydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase
                            .hydrateWithPartiallyCompletedSets(loggingWorkout = updatedWorkout)

                        finalWorkout to inProgressResults
                    }.map { (loggingWorkout, inProgressResults) ->
                        WorkoutState(
                            completedSets = inProgressResults,
                            personalRecords = personalRecords,
                            workout = loggingWorkout,
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getSetResultsFlow(
        workout: Workout?,
        programMetadata: ActiveProgramMetadata,
        useAllData: Boolean,
    ): Flow<List<SetResult>> {
        if (workout == null) return flowOf(emptyList())
        return setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(
            workoutId = workout.id,
            mesoCycle = programMetadata.currentMesocycle,
            microCycle = programMetadata.currentMicrocycle,
        ).mapLatest { resultsFromLastWorkout ->
            if (useAllData) {
                getResultsWithAllWorkoutDataAppended(
                    workout = workout,
                    resultsFromLastWorkout = resultsFromLastWorkout
                )
            } else resultsFromLastWorkout
        }
    }

    private suspend fun getResultsWithAllWorkoutDataAppended(
        workout: Workout,
        resultsFromLastWorkout: List<SetResult>,
    ): List<SetResult> {
        val liftIdsOfResults = resultsFromLastWorkout.map { it.liftId }.toHashSet()
        val liftIdsToSearchFor = workout.lifts
            .filter { !liftIdsOfResults.contains(it.liftId) }
            .map { workoutLift -> workoutLift.liftId }

        return getNewestResultsFromOtherWorkouts(
            workout = workout,
            liftIdsToSearchFor = liftIdsToSearchFor,
            existingResultsForOtherLifts = resultsFromLastWorkout,
            includeDeload = false,
        )
    }

    private suspend fun getNewestResultsFromOtherWorkouts(
        workout: Workout,
        liftIdsToSearchFor: List<Long>,
        existingResultsForOtherLifts: List<SetResult>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return if (liftIdsToSearchFor.isNotEmpty()) {
            val linearProgressionLiftIds = workout.lifts
                .filter {
                    it.progressionScheme == ProgressionScheme.LINEAR_PROGRESSION
                }.map { it.liftId }
                .toHashSet()

            existingResultsForOtherLifts.toMutableList().apply {
                val resultsFromOtherWorkouts =
                    workoutLogRepository.getMostRecentSetResultsForLiftIds(
                        liftIds = liftIdsToSearchFor,
                        linearProgressionLiftIds = linearProgressionLiftIds,
                        includeDeload = includeDeload,
                    )

                addAll(resultsFromOtherWorkouts)
            }
        } else existingResultsForOtherLifts
    }

    private suspend fun getPersonalRecords(
        workoutId: Long,
        mesoCycle: Int,
        microCycle: Int,
        liftIds: List<Long>
    ): Map<Long, PersonalRecord> {
        val prevWorkoutPersonalRecords = setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(
            workoutId = workoutId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftIds = liftIds,
        )
        return setLogEntryRepository.getPersonalRecordsForLifts(liftIds)
            .associateBy { it.liftId }
            .toMutableMap()
            .apply {
                prevWorkoutPersonalRecords.fastForEach { prevWorkoutPr ->
                    get(prevWorkoutPr.liftId)?.let { allWorkoutsPr ->
                        if (allWorkoutsPr.personalRecord < prevWorkoutPr.personalRecord) {
                            put(prevWorkoutPr.liftId, prevWorkoutPr)
                        }
                    } ?: put(prevWorkoutPr.liftId, prevWorkoutPr)
                }
            }
    }

    fun toggleReorderLifts() {
        mutableWorkoutState.update {
            it.copy(isReordering = !it.isReordering)
        }
    }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) {
        mutableWorkoutState.update { it.copy(isReordering = false) }

        try {
            executeInTransactionScope {
                val newWorkoutLiftIndices = newLiftOrder
                    .mapIndexed { index, item -> item.key to index }
                    .associate { it.first to it.second }

                val updatedLifts = workoutLiftsRepository.getForWorkout(mutableWorkoutState.value.workout!!.id)
                    .map {
                        when (it) {
                            is StandardWorkoutLift -> it.copy(position = newWorkoutLiftIndices[it.id]!!)
                            is CustomWorkoutLift -> it.copy(position = newWorkoutLiftIndices[it.id]!!)
                            else -> throw Exception("${it::class.simpleName} is not defined.")
                        }
                    }
                workoutLiftsRepository.updateMany(updatedLifts)

                val workoutLiftIdByLiftId = mutableWorkoutState.value.workout!!.lifts.associate { it.liftId to it.id }
                val updatedInProgressSetResults = mutableWorkoutState.value.completedSets.map { completedSet ->
                    val workoutLiftIdOfCompletedSet = workoutLiftIdByLiftId[completedSet.liftId]
                    when (completedSet) {
                        is StandardSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                        is MyoRepSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                        is LinearProgressionSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                        else -> throw Exception("${completedSet::class.simpleName} is not defined.")
                    }
                }
                setResultsRepository.upsertMany(updatedInProgressSetResults)
            }
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error reordering lifts", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to reorder lifts. Please try again.")
            }
        }
    }

    fun setWorkoutLogVisibility(visible: Boolean) {
        mutableWorkoutState.update {
            it.copy(
                workoutLogVisible = visible,
                isReordering = if (visible) false else it.isReordering,
            )
        }
    }

    fun toggleDeloadPrompt() {
        mutableWorkoutState.update {
            it.copy(isDeloadPromptDialogShown = !it.isDeloadPromptDialogShown)
        }
    }

    fun skipDeloadMicrocycle(): Job {
        mutableWorkoutState.update {
            it.copy(isDeloadPromptDialogShown = false)
        }
        return viewModelScope.launch {
            try {
                executeInTransactionScope {
                    val programMetadata = mutableWorkoutState.value.programMetadata!!
                    programsRepository.updateMesoAndMicroCycle(
                        id = programMetadata.programId,
                        mesoCycle = programMetadata.currentMesocycle + 1,
                        microCycle = 0,
                        microCyclePosition = 0,
                    )
                }
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Error skipping deload microcycle", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                showToast("Failed to skip deload microcycle. Please try again.")
            }
        }
    }

    fun showDeloadPromptOrStartWorkout() {
        if (mutableWorkoutState.value.isDeloadWeek &&
            mutableWorkoutState.value.programMetadata!!.currentMicrocyclePosition == 0) {
            mutableWorkoutState.update {
                it.copy(isDeloadPromptDialogShown = true)
            }
        } else {
            startWorkout()
        }
    }

    fun startWorkout() {
        mutableWorkoutState.update {
            it.copy(
                workoutLogVisible = true,
                isDeloadPromptDialogShown = false,
            )
        }

        try {
            executeInTransactionScope {
                val inProgressWorkout = WorkoutInProgressUiModel(
                    startTime = getCurrentDate(),
                )
                workoutInProgressRepository.insert(inProgressWorkout.toDomainModel(mutableWorkoutState.value.workout!!.id))
            }
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error starting workout", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to start workout. Please try again.")
            }
        }
    }

    private fun getTempFileFromBitmap(context: Context, bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        val fileOutputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        return file
    }

    fun shareWorkoutSummary(context: Context, workoutSummaryBitmap: Bitmap) {
        try {
            val shareUri: Uri = FileProvider.getUriForFile(
                context,
                "com.browntowndev.liftlab.fileprovider",
                getTempFileFromBitmap(
                    context = context,
                    bitmap = workoutSummaryBitmap,
                    fileName = "workoutSummary.png"
                )
            )
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share WorkoutEntity Results"))
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error sharing workout summary", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to share workout summary. Please try again.")
            }
        }
    }

    fun toggleCompletionSummary() {
        mutableWorkoutState.update {
            it.copy(
                isCompletionSummaryVisible = !it.isCompletionSummaryVisible,
                workoutCompletionSummary = if (!it.isCompletionSummaryVisible) {
                    try {
                        getWorkoutCompletionSummaryUseCase.get(
                            loggingWorkout = it.workout!!,
                            personalRecords = it.personalRecords.values.toList(),
                            completedSets = it.completedSets,
                        ).toUiModel()
                    } catch (e: Exception) {
                        Log.e("WorkoutViewModel", "Error getting workout completion summary", e)
                        FirebaseCrashlytics.getInstance().recordException(e)
                        viewModelScope.launch {
                            showToast("Failed to generate workout summary. Please try again.")
                        }
                        null
                    }
                } else null,
                isReordering = if (it.isCompletionSummaryVisible) false else it.isReordering
            )
        }
    }

    fun finishWorkout() {
        toggleCompletionSummary()

        try {
            executeInTransactionScope {
                // Remove the workoutEntity from in progress
                workoutInProgressRepository.deleteAll()
                restTimerInProgressRepository.deleteAll()

                val startTimeInMillis = mutableWorkoutState.value.inProgressWorkout!!.startTime.time
                val durationInMillis = (getCurrentDate().time - startTimeInMillis)
                val programMetadata = mutableWorkoutState.value.programMetadata!!
                val workout = mutableWorkoutState.value.workout!!

                // Increment the mesocycle and microcycle
                val microCycleComplete =  (programMetadata.workoutCount - 1) == programMetadata.currentMicrocyclePosition
                val liftLevelDeloadsEnabled = SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
                val deloadWeekComplete = !liftLevelDeloadsEnabled && microCycleComplete && mutableWorkoutState.value.isDeloadWeek
                val newMesoCycle = if (deloadWeekComplete) programMetadata.currentMesocycle + 1 else programMetadata.currentMesocycle
                val newMicroCycle = if (deloadWeekComplete) 0 else if (microCycleComplete) programMetadata.currentMicrocycle + 1 else programMetadata.currentMicrocycle
                val newMicroCyclePosition = if (microCycleComplete) 0 else programMetadata.currentMicrocyclePosition + 1
                programsRepository.updateMesoAndMicroCycle(
                    id = programMetadata.programId,
                    mesoCycle = newMesoCycle,
                    microCycle = newMicroCycle,
                    microCyclePosition = newMicroCyclePosition,
                )

                // Get/create the historical workoutEntity name entry then use it to insert a workoutEntity log entry
                var historicalWorkoutNameId =
                    historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(
                        programId = programMetadata.programId,
                        workoutId = workout.id,
                    )
                if (historicalWorkoutNameId == null) {
                    historicalWorkoutNameId = historicalWorkoutNamesRepository.insert(
                        HistoricalWorkoutName(
                            programId = programMetadata.programId,
                            workoutId = workout.id,
                            programName = programMetadata.name,
                            workoutName = workout.name,
                        )
                    )
                }
                val workoutLogEntryId = workoutLogRepository.insertWorkoutLogEntry(
                    historicalWorkoutNameId = historicalWorkoutNameId,
                    programDeloadWeek = programMetadata.deloadWeek,
                    programWorkoutCount = programMetadata.workoutCount,
                    mesoCycle = programMetadata.currentMesocycle,
                    microCycle = programMetadata.currentMicrocycle,
                    microcyclePosition = programMetadata.currentMicrocyclePosition,
                    date = getCurrentDate(),
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
            }
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error finishing workout", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to finish workout. Please try again.")
            }
        }
    }

    private suspend fun moveSetResultsToLogHistory(
        workoutLogEntryId: Long,
        programMetadata: ActiveProgramMetadata,
        workout: LoggingWorkout
    ) {
        val liftsAndPositions = mutableWorkoutState.value.workout!!.lifts.associate {
            it.liftId to it.position
        }

        // If any lifts were changed and had completed results do not copy them
        val excludeFromCopy = mutableWorkoutState.value.completedSets.filter { result ->
                val liftPosition = liftsAndPositions[result.liftId]
                liftPosition != result.liftPosition
            }.map {
                it.id
            }.toHashSet()

        // If someone marked a myorep set incomplete in the middle of a sequence and didn't
        // finish it, fix the myorep set positions to fill the gap
        val myoRepSetsToSynchronizePositonsFor = mutableWorkoutState.value.completedSets
            .filterIsInstance<MyoRepSetResult>()
            .filter { it.id !in excludeFromCopy }
            .groupBy { "${it.liftId}-${it.liftPosition}" }
            .values
            .flatMap { resultsForLift ->
                resultsForLift.sortedBy { it.id }.mapIndexedNotNull { index, result ->
                    val expectedMyoRepSetPosition: Int? = if (index == 0) null else index - 1
                    if (result.myoRepSetPosition != expectedMyoRepSetPosition) {
                        result.copy(myoRepSetPosition = expectedMyoRepSetPosition)
                    } else null
                }
            }

        if (myoRepSetsToSynchronizePositonsFor.isNotEmpty()) {
            setResultsRepository.upsertMany(myoRepSetsToSynchronizePositonsFor)
        }

        // Copy all of the set results from this workout into the set history table
        setLogEntryRepository.insertFromPreviousSetResults(
            workoutLogEntryId = workoutLogEntryId,
            workoutId = mutableWorkoutState.value.workout!!.id,
            mesocycle = programMetadata.currentMesocycle,
            microcycle = programMetadata.currentMicrocycle,
            excludeFromCopy = excludeFromCopy.toList(),
        )

        // Get all the set results for deloaded lifts
        val deloadSetResults = mutableWorkoutState.value.workout!!.lifts
            .filter { workoutLift ->
                // workoutEntity lifts whose deload week it is
                val deloadWeek = (workoutLift.deloadWeek ?: programMetadata.deloadWeek) - 1
                deloadWeek == programMetadata.currentMicrocycle
            }.fastMap {
                // key that can be used to match set results
                "${it.liftId}-${it.position}"
            }.toHashSet().let { deloadedWorkoutLiftIds ->
                // set results for deloaded workoutEntity lifts
                mutableWorkoutState.value.completedSets
                    .filter { deloadedWorkoutLiftIds.contains("${it.liftId}-${it.liftPosition}") }
                    .fastMap { it.id }
            }

        // Delete all set results from the previous workoutEntity OR ones that were deloaded. Deloaded
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
                isConfirmCancelWorkoutDialogShown = !it.isConfirmCancelWorkoutDialogShown
            )
        }
    }

    fun cancelWorkout() {
        if (mutableWorkoutState.value.isConfirmCancelWorkoutDialogShown)
            toggleConfirmCancelWorkoutModal()

        mutableWorkoutState.update {
            it.copy(
                workoutLogVisible = false,
                inProgressWorkout = null, // do optimistically in case updates take enough to notice
            )
        }

        try {
            executeInTransactionScope {
                // Remove the workoutEntity from in progress
                workoutInProgressRepository.deleteAll()

                // Delete all set results from the workoutEntity
                val programMetadata = mutableWorkoutState.value.programMetadata!!
                setResultsRepository.deleteAllForWorkout(
                    workoutId = mutableWorkoutState.value.workout!!.id,
                    mesoCycle = programMetadata.currentMesocycle,
                    microCycle = programMetadata.currentMicrocycle,
                )
            }
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error canceling workout", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to cancel workout. Please try again.")
            }
        }
    }

    fun updateRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) {
        try {
            executeInTransactionScope {
                mutableWorkoutState.value.workout?.lifts?.fastFirst { workoutLift ->
                    workoutLift.id == workoutLiftId
                }?.liftId?.let { liftId ->
                    liftsRepository.updateRestTime(
                        id = liftId,
                        enabled = enabled,
                        newRestTime = newRestTime
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error updating rest time", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to update rest time. Please try again.")
            }
        }
    }

    fun updateNote(workoutLiftId: Long, note: String) {
        try {
            executeInTransactionScope {
                liftsRepository.updateNote(workoutLiftId, note.ifEmpty { null })
            }
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error updating note", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to update note. Please try again.")
            }
        }
    }

    fun saveRestTimerInProgress(restTime: Long) {
        try {
            executeInTransactionScope {
                insertRestTimerInProgress(restTime)
            }
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error saving rest timer in progress", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to save rest timer. Please try again.")
            }
        }
    }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> {
        return try {
            setResultsRepository.upsertMany(updatedResults)
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error upserting many set results", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to save set results. Please try again.")
            }
            emptyList()
        }
    }

    override suspend fun upsertSetResult(updatedResult: SetResult): Long {
        return try {
            Log.d("WorkoutViewModel", "upsertSetResult: $updatedResult")
            setResultsRepository.upsert(updatedResult)
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error upserting set result", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to save set result. Please try again.")
            }
            -1L // Indicate failure with a negative ID or throw a custom exception
        }
    }

    override suspend fun deleteSetResult(id: Long) {
        try {
            setResultsRepository.deleteById(id)
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error deleting set result", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to delete set result. Please try again.")
            }
        }
    }

    override suspend fun insertRestTimerInProgress(restTime: Long) {
        try {
            restTimerInProgressRepository.insert(restTime)
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error inserting rest timer in progress", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            viewModelScope.launch {
                showToast("Failed to save rest timer progress. Please try again.")
            }
        }
    }

    override fun stopRestTimer() {
        cancelRestTimer()
    }
}
