package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.ui.models.ReorderableListItem
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.useCase.shared.UpdateRestTimeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CancelWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetResultByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetActiveWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.InsertRestTimerInProgressUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.ReorderWorkoutLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.RestTimerCompletedUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.SkipDeloadAndStartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.StartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpdateLiftNoteUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertManySetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetResultUseCase
import com.browntowndev.liftlab.ui.mapping.WorkoutCompletionSummaryUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutInProgressUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val getWorkoutCompletionSummaryUseCase: GetWorkoutCompletionSummaryUseCase,
    private val reorderWorkoutLiftsUseCase: ReorderWorkoutLiftsUseCase,
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val skipDeloadAndStartWorkoutUseCase: SkipDeloadAndStartWorkoutUseCase,
    getActiveWorkoutStateFlowUseCase: GetActiveWorkoutStateFlowUseCase,
    private val completeWorkoutUseCase: CompleteWorkoutUseCase,
    private val cancelWorkoutUseCase: CancelWorkoutUseCase,
    private val upsertManySetResultsUseCase: UpsertManySetResultsUseCase,
    private val upsertSetResultUseCase: UpsertSetResultUseCase,
    private val deleteSetResultByIdUseCase: DeleteSetResultByIdUseCase,
    private val insertRestTimerInProgressUseCase: InsertRestTimerInProgressUseCase,
    private val updateRestTimeUseCase: UpdateRestTimeUseCase,
    private val restTimerCompletedUseCase: RestTimerCompletedUseCase,
    private val updateLiftNoteUseCase: UpdateLiftNoteUseCase,
    private val navigateToWorkoutHistory: () -> Unit,
    private val cancelRestTimer: () -> Unit,
    transactionScope: TransactionScope,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    transactionScope = transactionScope,
    eventBus = eventBus,
) {
    companion object {
        private const val TAG = "WorkoutViewModel"
    }

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
            TopAppBarAction.RestTimerCompleted -> viewModelScope.launch {
                try {
                    restTimerCompletedUseCase()
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
        getActiveWorkoutStateFlowUseCase().map { activeWorkoutState ->
            WorkoutState(
                inProgressWorkout = activeWorkoutState.inProgressWorkout?.toUiModel(),
                completedSets = activeWorkoutState.completedSets,
                programMetadata = activeWorkoutState.programMetadata,
                workout = activeWorkoutState.workout,
                personalRecords = activeWorkoutState.personalRecords,
                restTimerStartedAt = activeWorkoutState.restTimerStartedAt,
                restTime = activeWorkoutState.restTime,
                initialized = true,
            )
        }.onEach { newUiState ->
            mutableWorkoutState.update { currentState ->
                currentState.copy(
                    inProgressWorkout = newUiState.inProgressWorkout,
                    completedSets = newUiState.completedSets,
                    programMetadata = newUiState.programMetadata,
                    workout = newUiState.workout,
                    personalRecords = newUiState.personalRecords,
                    restTimerStartedAt = newUiState.restTimerStartedAt,
                    restTime = newUiState.restTime,
                    initialized = true,
                    workoutLogVisible = if (newUiState.inProgressWorkout == null) false else currentState.workoutLogVisible,
                    isCompletionSummaryVisible = false,
                    isDeloadPromptDialogShown = false,
                    isReordering = false,
                )
            }
        }.catch { e ->
            Log.e("WorkoutViewModel", "Error in initialize", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            emitUserMessage("An unexpected error occurred during initialization. Please restart the app.")
        }.launchIn(viewModelScope)
    }

    fun toggleReorderLifts() {
        mutableWorkoutState.update {
            it.copy(isReordering = !it.isReordering)
        }
    }

    fun reorderLifts(newLiftOrder: List<ReorderableListItem>) =
        executeWithErrorHandling("Failed to reorder lifts") {
            val newWorkoutLiftIndices = newLiftOrder
                .mapIndexed { index, item -> item.key to index }
                .associate { it.first to it.second }

            reorderWorkoutLiftsUseCase(
                workout = mutableWorkoutState.value.workout!!,
                completedSets = mutableWorkoutState.value.completedSets,
                newWorkoutLiftIndices = newWorkoutLiftIndices
            )
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

    fun skipDeloadMicrocycleAndStartWorkout() = executeWithErrorHandling("Failed to skip deload microcycle and start") {
        skipDeloadAndStartWorkoutUseCase(
            programMetadata = mutableWorkoutState.value.programMetadata!!,
            workoutId = mutableWorkoutState.value.workout!!.id,
        )
        updateStateForStartedWorkout()
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
        startWorkoutUseCase(mutableWorkoutState.value.workout!!.id)
        updateStateForStartedWorkout()
    }

    private fun updateStateForStartedWorkout() {
        mutableWorkoutState.update {
            it.copy(
                workoutLogVisible = true,
                isDeloadPromptDialogShown = false, // If it was shown, hide it now
            )
        }
    }

    fun shareWorkoutSummary(context: Context, workoutSummaryBitmap: Bitmap) = executeWithErrorHandling("Failed to share workout summary") {
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

    fun toggleCompletionSummary() = executeWithErrorHandling("Failed to generate workout summary") {
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

    fun finishWorkout() = executeWithErrorHandling("Failed to complete workout") {
        stopRestTimer()
        completeWorkoutUseCase(
            inProgressWorkout = mutableWorkoutState.value.inProgressWorkout!!,
            programMetadata = mutableWorkoutState.value.programMetadata!!,
            workout = mutableWorkoutState.value.workout!!,
            completedSets = mutableWorkoutState.value.completedSets,
            isDeloadWeek = mutableWorkoutState.value.isDeloadWeek
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

        cancelWorkoutUseCase(
            programMetadata = mutableWorkoutState.value.programMetadata!!,
            workout = mutableWorkoutState.value.workout!!
        )
    }

    fun updateRestTime(workoutLiftId: Long, newRestTime: Duration, enabled: Boolean) = executeWithErrorHandling("Failed to update rest timer") {
        val workoutLift = getWorkoutLiftAndLogIfNull(workoutLiftId) ?: return@executeWithErrorHandling
        updateRestTimeUseCase(
            liftId = workoutLift.liftId,
            enabled = enabled,
            restTime = newRestTime
        )
    }

    fun updateNote(liftId: Long, note: String) = executeWithErrorHandling("Failed to update note") {
        updateLiftNoteUseCase(liftId, note)
    }

    override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> =
        upsertManySetResultsUseCase(updatedResults)

    override suspend fun upsertSetResult(updatedResult: SetResult): Long =
        upsertSetResultUseCase(updatedResult)

    override suspend fun deleteSetResult(id: Long) =
        deleteSetResultByIdUseCase(id)

    override suspend fun insertRestTimerInProgress(restTime: Long) {
        insertRestTimerInProgressUseCase(restTime)
    }

    override fun stopRestTimer() {
        cancelRestTimer()
    }

    private fun getWorkoutLiftAndLogIfNull(workoutLiftId: Long): LoggingWorkoutLift? {
        val workoutLift = mutableWorkoutState.value.workout?.lifts
            ?.find { it.id == workoutLiftId }

        if (workoutLift == null) {
            emitUserMessage("Must be standard workoutEntity liftEntity.")
            val exception = Exception("Must be standard workoutEntity liftEntity.")
            Log.e(TAG, "Must be standard workoutEntity liftEntity.", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
        }

        return workoutLift
    }
}
