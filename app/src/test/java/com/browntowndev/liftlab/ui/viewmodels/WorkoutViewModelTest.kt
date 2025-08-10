package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
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
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    // Use cases & deps
    @RelaxedMockK lateinit var getActiveWorkoutStateFlowUseCase: GetActiveWorkoutStateFlowUseCase
    @RelaxedMockK lateinit var hydrateLoggingWorkoutWithExistingLiftDataUseCase: HydrateLoggingWorkoutWithExistingLiftDataUseCase
    @RelaxedMockK lateinit var getWorkoutCompletionSummaryUseCase: GetWorkoutCompletionSummaryUseCase
    @RelaxedMockK lateinit var reorderWorkoutLiftsUseCase: ReorderWorkoutLiftsUseCase
    @RelaxedMockK lateinit var startWorkoutUseCase: StartWorkoutUseCase
    @RelaxedMockK lateinit var skipDeloadAndStartWorkoutUseCase: SkipDeloadAndStartWorkoutUseCase
    @RelaxedMockK lateinit var completeWorkoutUseCase: CompleteWorkoutUseCase
    @RelaxedMockK lateinit var cancelWorkoutUseCase: CancelWorkoutUseCase
    @RelaxedMockK lateinit var upsertManySetResultsUseCase: UpsertManySetResultsUseCase
    @RelaxedMockK lateinit var upsertSetResultUseCase: UpsertSetResultUseCase
    @RelaxedMockK lateinit var deleteSetResultByIdUseCase: DeleteSetResultByIdUseCase
    @RelaxedMockK lateinit var insertRestTimerInProgressUseCase: InsertRestTimerInProgressUseCase
    @RelaxedMockK lateinit var updateRestTimeUseCase: UpdateRestTimeUseCase
    @RelaxedMockK lateinit var updateLiftNoteUseCase: UpdateLiftNoteUseCase
    @RelaxedMockK lateinit var completeSetUseCase: CompleteSetUseCase
    @RelaxedMockK lateinit var undoSetCompletionUseCase: UndoSetCompletionUseCase
    @RelaxedMockK lateinit var eventBus: EventBus

    private lateinit var viewModel: WorkoutViewModel
    private val testDispatcher = StandardTestDispatcher()

    private var historyNavigated = false

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)

        // No active workout emissions for these interaction tests
        every { getActiveWorkoutStateFlowUseCase.invoke() } returns emptyFlow()

        // EventBus is present but we don't need it to actually post/receive
        every { eventBus.register(any()) } just runs
        every { eventBus.unregister(any()) } just runs

        historyNavigated = false

        viewModel = WorkoutViewModel(
            getActiveWorkoutStateFlowUseCase = getActiveWorkoutStateFlowUseCase,
            hydrateLoggingWorkoutWithExistingLiftDataUseCase = hydrateLoggingWorkoutWithExistingLiftDataUseCase,
            getWorkoutCompletionSummaryUseCase = getWorkoutCompletionSummaryUseCase,
            reorderWorkoutLiftsUseCase = reorderWorkoutLiftsUseCase,
            startWorkoutUseCase = startWorkoutUseCase,
            skipDeloadAndStartWorkoutUseCase = skipDeloadAndStartWorkoutUseCase,
            completeWorkoutUseCase = completeWorkoutUseCase,
            cancelWorkoutUseCase = cancelWorkoutUseCase,
            upsertManySetResultsUseCase = upsertManySetResultsUseCase,
            upsertSetResultUseCase = upsertSetResultUseCase,
            deleteSetResultByIdUseCase = deleteSetResultByIdUseCase,
            insertRestTimerInProgressUseCase = insertRestTimerInProgressUseCase,
            updateRestTimeUseCase = updateRestTimeUseCase,
            updateLiftNoteUseCase = updateLiftNoteUseCase,
            navigateToWorkoutHistory = { historyNavigated = true },
            completeSetUseCase = completeSetUseCase,
            undoSetCompletionUseCase = undoSetCompletionUseCase,
            eventBus = eventBus
        )
        // Let init {} run
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleReorderLifts_togglesFlag() = runTest {
        val initial = viewModel.workoutState.value
        assertFalse(initial.isReordering, "precondition")

        viewModel.toggleReorderLifts()
        val first = viewModel.workoutState.value
        assertTrue(first.isReordering)

        viewModel.toggleReorderLifts()
        val second = viewModel.workoutState.value
        assertFalse(second.isReordering)
    }

    @Test
    fun setWorkoutLogVisibility_setsVisibility_andClearsReorderWhenVisible() = runTest {
        // Make isReordering true
        viewModel.toggleReorderLifts()
        assertTrue(viewModel.workoutState.value.isReordering)

        // Show workout log -> should clear isReordering
        viewModel.setWorkoutLogVisibility(true)
        val shown = viewModel.workoutState.value
        assertTrue(shown.workoutLogVisible)
        assertFalse(shown.isReordering)

        // Hide workout log -> should NOT change isReordering (stays false)
        viewModel.setWorkoutLogVisibility(false)
        val hidden = viewModel.workoutState.value
        assertFalse(hidden.workoutLogVisible)
        assertFalse(hidden.isReordering)
    }

    @Test
    fun toggleDeloadPrompt_togglesFlag() = runTest {
        val initial = viewModel.workoutState.value
        assertFalse(initial.isDeloadPromptDialogShown, "precondition")

        viewModel.toggleDeloadPrompt()
        assertTrue(viewModel.workoutState.value.isDeloadPromptDialogShown)

        viewModel.toggleDeloadPrompt()
        assertFalse(viewModel.workoutState.value.isDeloadPromptDialogShown)
    }

    @Test
    fun toggleConfirmCancelWorkoutModal_togglesFlag() = runTest {
        val initial = viewModel.workoutState.value
        assertFalse(initial.isConfirmCancelWorkoutDialogShown, "precondition")

        viewModel.toggleConfirmCancelWorkoutModal()
        assertTrue(viewModel.workoutState.value.isConfirmCancelWorkoutDialogShown)

        viewModel.toggleConfirmCancelWorkoutModal()
        assertFalse(viewModel.workoutState.value.isConfirmCancelWorkoutDialogShown)
    }

    @Test
    fun updateNote_delegatesToUseCase() = runTest {
        viewModel.updateNote(42L, "new note")

        // Let the launched coroutine run
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { updateLiftNoteUseCase(42L, "new note") }
    }

    @Test
    fun handleActionBarEvents_openWorkoutHistory_invokesCallback() = runTest {
        assertFalse(historyNavigated, "precondition")
        viewModel.handleActionBarEvents(TopAppBarEvent.ActionEvent(TopAppBarAction.OpenWorkoutHistory))
        assertTrue(historyNavigated)
    }

    @Test
    fun handleActionBarEvents_navigatedBack_hidesLogAndReorder() = runTest {
        // Put state into "log visible & reordering" to test the branch
        viewModel.setWorkoutLogVisibility(true)
        viewModel.toggleReorderLifts()
        assertTrue(viewModel.workoutState.value.workoutLogVisible)
        assertTrue(viewModel.workoutState.value.isReordering)

        viewModel.handleActionBarEvents(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))

        val after = viewModel.workoutState.value
        assertFalse(after.workoutLogVisible)
        assertFalse(after.isReordering)
    }
}
