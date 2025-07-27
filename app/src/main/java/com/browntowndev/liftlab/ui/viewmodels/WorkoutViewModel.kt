package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.fastFirst
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
import com.browntowndev.liftlab.core.domain.useCase.workout.GetNewestSetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetPersonalRecordsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.HydrateLoggingWorkoutWithCompletedSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.ReorderLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.SkipDeloadAndStartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.StartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.progression.CalculateLoggingWorkoutUseCase
import com.browntowndev.liftlab.ui.mapping.WorkoutCompletionSummaryUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutInProgressUiMappingExtensions.toDomainModel
import com.browntowndev.liftlab.ui.mapping.WorkoutInProgressUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutStateMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val getWorkoutCompletionSummaryUseCase: GetWorkoutCompletionSummaryUseCase,
    private val reorderLiftsUseCase: ReorderLiftsUseCase,
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val skipDeloadAndStartWorkoutUseCase: SkipDeloadAndStartWorkoutUseCase,
    private val getWorkoutStateFlowUseCase: GetWorkoutStateFlowUseCase,
    private val programsRepository: ProgramsRepository,
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
                    combine(
                        workoutInProgressFlow,
                        getWorkoutStateFlowUseCase(programMetadata),
                    ) { inProgressWorkout, calculatedWorkoutData ->
                        val workoutStateFromCalculatedData = calculatedWorkoutData.toUiModel()
                        WorkoutState(
                            programMetadata = programMetadata,
                            inProgressWorkout = inProgressWorkout?.toUiModel(),
                            completedSets = workoutStateFromCalculatedData.completedSets,
                            workout = workoutStateFromCalculatedData.workout,
                            personalRecords = calculatedWorkoutData.personalRecords,
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

    fun toggleReorderLifts() {
        mutableWorkoutState.update {
            it.copy(isReordering = !it.isReordering)
        }
    }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) =
        executeWithErrorHandling("Failed to reorder lifts") {
            executeInTransactionScope {
                val newWorkoutLiftIndices = newLiftOrder
                    .mapIndexed { index, item -> item.key to index }
                    .associate { it.first to it.second }

                reorderLiftsUseCase.reorder(
                    workout = mutableWorkoutState.value.workout!!,
                    completedSets = mutableWorkoutState.value.completedSets,
                    newWorkoutLiftIndices = newWorkoutLiftIndices
                )
                mutableWorkoutState.update { it.copy(isReordering = false) }
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

    fun skipDeloadMicrocycleAndStartWorkout() =
        executeWithErrorHandling("Failed to skip deload microcycle and start") {
            executeInTransactionScope {
                skipDeloadAndStartWorkoutUseCase.skipAndStart(
                    programMetadata = mutableWorkoutState.value.programMetadata!!,
                    workoutId = mutableWorkoutState.value.workout!!.id,
                )
                mutableWorkoutState.update {
                    it.copy(isDeloadPromptDialogShown = false)
                }
                updateStateForStartedWorkout()
            }
        }

    fun showDeloadPromptOrStartWorkout() =
        executeWithErrorHandling("Failed to start workout") {
            if (mutableWorkoutState.value.isDeloadWeek &&
                mutableWorkoutState.value.programMetadata!!.currentMicrocyclePosition == 0
            ) {
                mutableWorkoutState.update {
                    it.copy(isDeloadPromptDialogShown = true)
                }
            } else {
                startWorkout()
            }
        }

    fun startWorkout() = executeWithErrorHandling("Failed to start workout") {
        executeInTransactionScope {
            startWorkoutUseCase.start(mutableWorkoutState.value.workout!!.id)
            updateStateForStartedWorkout()
        }
    }

    private fun updateStateForStartedWorkout() {
        mutableWorkoutState.update {
            it.copy(
                workoutLogVisible = true,
                isDeloadPromptDialogShown = false, // If it was shown, hide it now
            )
        }
    }

    fun shareWorkoutSummary(context: Context, workoutSummaryBitmap: Bitmap) =
        executeWithErrorHandling("Failed to share workout summary") {
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
        }

    private fun getTempFileFromBitmap(context: Context, bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        val fileOutputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        return file
    }

    fun toggleCompletionSummary() =
        executeWithErrorHandling("Failed to generate workout summary") {
            mutableWorkoutState.update {
                it.copy(
                    isCompletionSummaryVisible = !it.isCompletionSummaryVisible,
                    workoutCompletionSummary = if (!it.isCompletionSummaryVisible) {
                        getWorkoutCompletionSummaryUseCase.get(
                            loggingWorkout = it.workout!!,
                            personalRecords = it.personalRecords.values.toList(),
                            completedSets = it.completedSets,
                        ).toUiModel()
                    } else null,
                    isReordering = if (it.isCompletionSummaryVisible) false else it.isReordering
                )
            }
        }

    fun finishWorkout() =
        executeWithErrorHandling("Failed to complete workout") {
            toggleCompletionSummary()

            executeInTransactionScope {
                // Remove the workoutEntity from in progress
                workoutInProgressRepository.deleteAll()
                restTimerInProgressRepository.deleteAll()

                val startTimeInMillis = mutableWorkoutState.value.inProgressWorkout!!.startTime.time
                val durationInMillis = (getCurrentDate().time - startTimeInMillis)
                val programMetadata = mutableWorkoutState.value.programMetadata!!
                val workout = mutableWorkoutState.value.workout!!

                // Increment the mesocycle and microcycle
                val microCycleComplete =
                    (programMetadata.workoutCount - 1) == programMetadata.currentMicrocyclePosition
                val liftLevelDeloadsEnabled =
                    SettingsManager.getSetting(
                        LIFT_SPECIFIC_DELOADING,
                        DEFAULT_LIFT_SPECIFIC_DELOADING
                    )
                val deloadWeekComplete =
                    !liftLevelDeloadsEnabled && microCycleComplete && mutableWorkoutState.value.isDeloadWeek
                val newMesoCycle =
                    if (deloadWeekComplete) programMetadata.currentMesocycle + 1 else programMetadata.currentMesocycle
                val newMicroCycle =
                    if (deloadWeekComplete) 0 else if (microCycleComplete) programMetadata.currentMicrocycle + 1 else programMetadata.currentMicrocycle
                val newMicroCyclePosition =
                    if (microCycleComplete) 0 else programMetadata.currentMicrocyclePosition + 1
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

    fun cancelWorkout() = executeWithErrorHandling("Failed to cancel workout") {
        if (mutableWorkoutState.value.isConfirmCancelWorkoutDialogShown)
            toggleConfirmCancelWorkoutModal()

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

            mutableWorkoutState.update {
                it.copy(
                    workoutLogVisible = false,
                )
            }
        }
    }

    fun updateRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) =
        executeWithErrorHandling("Failed to update rest timer") {
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
        }

    fun updateNote(workoutLiftId: Long, note: String) =
        executeWithErrorHandling("Failed to update note") {
            executeInTransactionScope {
                liftsRepository.updateNote(workoutLiftId, note.ifEmpty { null })
            }
        }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> =
        setResultsRepository.upsertMany(updatedResults)

    override suspend fun upsertSetResult(updatedResult: SetResult): Long =
        setResultsRepository.upsert(updatedResult)

    override suspend fun deleteSetResult(id: Long) {
        setResultsRepository.deleteById(id)
    }

    override suspend fun insertRestTimerInProgress(restTime: Long) {
        restTimerInProgressRepository.insert(restTime)
    }

    override fun stopRestTimer() {
        cancelRestTimer()
    }
}
