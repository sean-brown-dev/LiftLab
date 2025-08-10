package com.browntowndev.liftlab.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.extensions.hasIncompleteModifiedSets
import com.browntowndev.liftlab.core.domain.extensions.mergeModifiedSets
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateRestTimeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CancelWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetResultByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetActiveWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.HydrateLoggingWorkoutWithExistingLiftDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.InsertRestTimerInProgressUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.ReorderWorkoutLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.SkipDeloadAndStartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.StartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpdateLiftNoteUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertManySetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetResultUseCase
import com.browntowndev.liftlab.ui.mapping.ProgramMappingExtensions.toDomainModel
import com.browntowndev.liftlab.ui.mapping.ProgramMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutCompletionSummaryUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutHistoryMappingExtensions.toDomainModel
import com.browntowndev.liftlab.ui.mapping.WorkoutHistoryMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutInProgressUiMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.mapping.WorkoutLoggingMappingExtensions.toDomainModel
import com.browntowndev.liftlab.ui.mapping.WorkoutLoggingMappingExtensions.toUiModel
import com.browntowndev.liftlab.ui.models.controls.ReorderableListItem
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    getActiveWorkoutStateFlowUseCase: GetActiveWorkoutStateFlowUseCase,
    private val hydrateLoggingWorkoutWithExistingLiftDataUseCase: HydrateLoggingWorkoutWithExistingLiftDataUseCase,
    private val getWorkoutCompletionSummaryUseCase: GetWorkoutCompletionSummaryUseCase,
    private val reorderWorkoutLiftsUseCase: ReorderWorkoutLiftsUseCase,
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val skipDeloadAndStartWorkoutUseCase: SkipDeloadAndStartWorkoutUseCase,
    private val completeWorkoutUseCase: CompleteWorkoutUseCase,
    private val cancelWorkoutUseCase: CancelWorkoutUseCase,
    private val upsertManySetResultsUseCase: UpsertManySetResultsUseCase,
    private val upsertSetResultUseCase: UpsertSetResultUseCase,
    private val deleteSetResultByIdUseCase: DeleteSetResultByIdUseCase,
    private val insertRestTimerInProgressUseCase: InsertRestTimerInProgressUseCase,
    private val updateRestTimeUseCase: UpdateRestTimeUseCase,
    private val updateLiftNoteUseCase: UpdateLiftNoteUseCase,
    private val navigateToWorkoutHistory: () -> Unit,
    completeSetUseCase: CompleteSetUseCase,
    undoSetCompletionUseCase: UndoSetCompletionUseCase,
    eventBus: EventBus,
): BaseWorkoutViewModel(
    completeSetUseCase = completeSetUseCase,
    undoSetCompletionUseCase = undoSetCompletionUseCase,
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
        getActiveWorkoutStateFlowUseCase()
            .map { activeWorkoutState ->
                WorkoutState(
                    inProgressWorkout = activeWorkoutState.inProgressWorkout?.toUiModel(),
                    completedSets = activeWorkoutState.completedSets.fastMap { it.toUiModel() },
                    programMetadata = activeWorkoutState.programMetadata?.toUiModel(),
                    workout = activeWorkoutState.workout?.toUiModel(),
                    personalRecords = activeWorkoutState.personalRecords
                        .map { it.key to it.value.toUiModel() }
                        .toMap(),
                    initialized = true,
                )
            }.onEach { newUiState ->
                val newWorkout = if (mutableWorkoutState.value.workout == null || newUiState.workout == null) {
                    newUiState.workout
                }
                else {
                    // Hydrate workout with any sets that have been started but not marked completed.
                    // These only exist in-memory in our state, so the state flow use case knows nothing
                    // about them and we have to hydrate the updated workout from the state flow with
                    // that data.
                    val updatedLiftsById = newUiState.workout.lifts.associateBy { it.id }
                    hydrateLoggingWorkoutWithExistingLiftDataUseCase(
                        loggingWorkout = newUiState.workout.toDomainModel(),
                        liftsToUpdateFrom = mutableWorkoutState.value.workout!!.lifts
                            .fastMapNotNull {
                                // newUiState has the latest completed/incompleted set data, but it doesn't have
                                // the in-memory modified lifts which were never completed. So, we need to merge
                                // the new lifts into the current in-memory lifts to get the holistic state
                                // of the lift
                                updatedLiftsById[it.id]?.let { updatedLift ->
                                    it.mergeModifiedSets(updatedLift)
                                }
                            }
                            .filter {
                                // Now that we have fully updated the lift, we can filter out any
                                // lifts that have not been modified
                                it.hasIncompleteModifiedSets()
                            }
                            .fastMap { it.toDomainModel() },
                    ).toUiModel()
                }

                mutableWorkoutState.update { currentState ->
                    currentState.copy(
                        workout = newWorkout,
                        inProgressWorkout = newUiState.inProgressWorkout,
                        completedSets = newUiState.completedSets,
                        programMetadata = newUiState.programMetadata,
                        personalRecords = newUiState.personalRecords,
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
                workout = mutableWorkoutState.value.workout!!.toDomainModel(),
                completedSets = mutableWorkoutState.value.completedSets.fastMap { it.toDomainModel() },
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
            programMetadata = mutableWorkoutState.value.programMetadata!!.toDomainModel(),
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
                        loggingWorkout = it.workout!!.toDomainModel(),
                        personalRecords = it.personalRecords.values.map { pr -> pr.toDomainModel() },
                        completedSets = it.completedSets.fastMap { result -> result.toDomainModel() },
                    ).toUiModel()
                } else null,
                isReordering = if (it.isCompletionSummaryVisible) false else it.isReordering
            )
        }
    }

    fun finishWorkout() = executeWithErrorHandling("Failed to complete workout") {
        completeWorkoutUseCase(
            inProgressWorkout = mutableWorkoutState.value.inProgressWorkout!!,
            programMetadata = mutableWorkoutState.value.programMetadata!!.toDomainModel(),
            workout = mutableWorkoutState.value.workout!!.toDomainModel(),
            completedSets = mutableWorkoutState.value.completedSets.fastMap { it.toDomainModel() },
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
            programMetadata = mutableWorkoutState.value.programMetadata!!.toDomainModel(),
            workout = mutableWorkoutState.value.workout!!.toDomainModel()
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

    private fun getWorkoutLiftAndLogIfNull(workoutLiftId: Long): LoggingWorkoutLiftUiModel? {
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
