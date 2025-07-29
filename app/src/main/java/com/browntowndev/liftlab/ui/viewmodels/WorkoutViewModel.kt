package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.fastFirst
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.common.toDate
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.useCase.workout.CancelWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.CompleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.GetWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.ReorderLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.SkipDeloadAndStartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workout.StartWorkoutUseCase
import com.browntowndev.liftlab.ui.mapping.WorkoutCompletionSummaryUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutInProgressUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutStateMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val completeWorkoutUseCase: CompleteWorkoutUseCase,
    private val cancelWorkoutUseCase: CancelWorkoutUseCase,
    private val programsRepository: ProgramsRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
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
                        emitUserMessage("Failed to complete rest timer. Please try again.")
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
                emitUserMessage("An unexpected error occurred during initialization. Please restart the app.")
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

                reorderLiftsUseCase(
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
                skipDeloadAndStartWorkoutUseCase(
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
            startWorkoutUseCase(mutableWorkoutState.value.workout!!.id)
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
                        getWorkoutCompletionSummaryUseCase(
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
            stopRestTimer()
            executeInTransactionScope {
                completeWorkoutUseCase(
                    inProgressWorkout = mutableWorkoutState.value.inProgressWorkout!!,
                    programMetadata = mutableWorkoutState.value.programMetadata!!,
                    workout = mutableWorkoutState.value.workout!!,
                    completedSets = mutableWorkoutState.value.completedSets,
                    isDeloadWeek = mutableWorkoutState.value.isDeloadWeek
                )

                mutableWorkoutState.update {
                    it.copy(workoutLogVisible = false)
                }
                toggleCompletionSummary()
            }
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
            cancelWorkoutUseCase(
                programMetadata = mutableWorkoutState.value.programMetadata!!,
                workout = mutableWorkoutState.value.workout!!
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
