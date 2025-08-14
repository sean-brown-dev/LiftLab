package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.GetActiveProgramWorkoutCountFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReorderWorkoutLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateRestTimeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CancelWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetResultByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetActiveWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.HydrateLoggingWorkoutWithExistingLiftDataUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.InsertRestTimerInProgressUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.SkipDeloadAndStartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.StartWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpdateLiftNoteUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertManySetResultsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetResultUseCase
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.viewmodels.workout.BaseWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.workout.WorkoutState
import com.browntowndev.liftlab.ui.viewmodels.workout.WorkoutViewModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    // Use cases & deps
    @RelaxedMockK lateinit var getActiveWorkoutStateFlowUseCase: GetActiveWorkoutStateFlowUseCase
    @RelaxedMockK lateinit var getActiveProgramWorkoutCountFlowUseCase: GetActiveProgramWorkoutCountFlowUseCase
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
            getActiveProgramWorkoutCountFlowUseCase = getActiveProgramWorkoutCountFlowUseCase,
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

    // --- helpers for these tests ---
    private fun modifyState(block: (WorkoutState) -> WorkoutState) {
        val field = BaseWorkoutViewModel::class.java.getDeclaredField("mutableWorkoutState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<WorkoutState>
        flow.value = block(flow.value)
    }

    private fun sampleWorkoutWithTwoLifts(): com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutUiModel {
        val lifts = listOf(
            com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutLiftUiModel(
                id = 10L, liftId = 1000L, position = 0,
                progressionScheme = com.browntowndev.liftlab.core.domain.enums.ProgressionScheme.LINEAR_PROGRESSION,
                deloadWeek = null, incrementOverride = null, sets = emptyList()
            ),
            com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutLiftUiModel(
                id = 20L, liftId = 2000L, position = 1,
                progressionScheme = com.browntowndev.liftlab.core.domain.enums.ProgressionScheme.LINEAR_PROGRESSION,
                deloadWeek = null, incrementOverride = null, sets = emptyList()
            ),
        )
        return com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutUiModel(
            id = 1L, name = "Workout", lifts = lifts
        )
    }

    // --- reorderLifts mapping & delegation ---
    @Test
    fun reorderLifts_delegatesWithCorrectIndices() = runTest {
        // seed workout
        modifyState { it.copy(
            programMetadata = mockk { every { programId } returns 1L },
            workout = sampleWorkoutWithTwoLifts(),
            completedSets = emptyList())
        }

        // expect call
        coEvery { reorderWorkoutLiftsUseCase(programId = any(), workout = any(), completedSets = any(), newWorkoutLiftIndices = any()) } just runs

        // swap order: 20 -> index 0, 10 -> index 1
        val order = listOf(
            com.browntowndev.liftlab.ui.models.controls.ReorderableListItem("Lift A", key = 20L),
            com.browntowndev.liftlab.ui.models.controls.ReorderableListItem("Lift B", key = 10L),
        )

        viewModel.reorderLifts(order)
        testDispatcher.scheduler.advanceUntilIdle()

        val mapSlot = slot<Map<Long, Int>>()
        coVerify(exactly = 1) {
            reorderWorkoutLiftsUseCase(programId = any(), workout = any(), completedSets = any(), newWorkoutLiftIndices = capture(mapSlot))
        }
        assertEquals(mapOf(20L to 0, 10L to 1), mapSlot.captured)
    }

    // --- start workout path ---
    @Test
    fun startWorkout_delegatesAndUpdatesState() = runTest {
        modifyState { it.copy(workout = sampleWorkoutWithTwoLifts()) }
        coEvery { startWorkoutUseCase(1L) } returns 1

        viewModel.startWorkout()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { startWorkoutUseCase(1L) }
        assertTrue(viewModel.workoutState.value.workoutLogVisible)
        assertFalse(viewModel.workoutState.value.isDeloadPromptDialogShown)
    }

    // --- skip deload and start ---
    @Test
    fun skipDeloadMicrocycleAndStartWorkout_delegatesAndUpdatesState() = runTest {
        val program = com.browntowndev.liftlab.ui.models.workoutLogging.ActiveProgramMetadataUiModel(
            programId = 7L, name = "P", deloadWeek = 2,
            currentMesocycle = 1, currentMicrocycle = 1, currentMicrocyclePosition = 0,
            workoutCount = 3
        )
        modifyState { it.copy(programMetadata = program, workout = sampleWorkoutWithTwoLifts()) }
        coEvery { skipDeloadAndStartWorkoutUseCase(programMetadata = any(), workoutId = 1L) } returns 1

        viewModel.skipDeloadMicrocycleAndStartWorkout()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { skipDeloadAndStartWorkoutUseCase(programMetadata = any(), workoutId = 1L) }
        assertTrue(viewModel.workoutState.value.workoutLogVisible)
        assertFalse(viewModel.workoutState.value.isDeloadPromptDialogShown)
    }

    // --- conditional start: show prompt (deload + at start of microcycle) ---
    @Test
    fun showDeloadPromptOrStartWorkout_whenDeloadAndAtStart_showsPromptOnly() = runTest {
        val program = com.browntowndev.liftlab.ui.models.workoutLogging.ActiveProgramMetadataUiModel(
            programId = 7L, name = "P",
            deloadWeek = 1,                    // currentMicrocycle=0 -> +1 == 1 -> deload
            currentMesocycle = 0, currentMicrocycle = 0, currentMicrocyclePosition = 0,
            workoutCount = 3
        )
        modifyState { it.copy(programMetadata = program, workout = sampleWorkoutWithTwoLifts()) }
        // ensure start is not invoked
        coEvery { startWorkoutUseCase(any()) } returns 1

        viewModel.showDeloadPromptOrStartWorkout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.workoutState.value.isDeloadPromptDialogShown)
        coVerify(exactly = 0) { startWorkoutUseCase(any()) }
    }

    // --- conditional start: not deload (or mid microcycle) -> starts workout ---
    @Test
    fun showDeloadPromptOrStartWorkout_whenNotDeload_startsWorkout() = runTest {
        val program = com.browntowndev.liftlab.ui.models.workoutLogging.ActiveProgramMetadataUiModel(
            programId = 7L, name = "P",
            deloadWeek = 4,                 // not a deload week
            currentMesocycle = 0, currentMicrocycle = 0, currentMicrocyclePosition = 1,
            workoutCount = 3
        )
        modifyState { it.copy(programMetadata = program, workout = sampleWorkoutWithTwoLifts()) }
        coEvery { startWorkoutUseCase(1L) } returns 1

        viewModel.showDeloadPromptOrStartWorkout()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { startWorkoutUseCase(1L) }
        assertTrue(viewModel.workoutState.value.workoutLogVisible)
    }

    // --- toggle completion summary: hiding path (no mapping needed) ---
    @Test
    fun toggleCompletionSummary_whenVisible_turnsOffAndClears_andStopsReordering() = runTest {
        modifyState { it.copy(isCompletionSummaryVisible = true, isReordering = true) }

        viewModel.toggleCompletionSummary()
        testDispatcher.scheduler.advanceUntilIdle()

        val s = viewModel.workoutState.value
        assertFalse(s.isCompletionSummaryVisible)
        assertNull(s.workoutCompletionSummary)
        assertFalse(s.isReordering)
    }

    // --- update rest time when lift exists ---
    @Test
    fun updateRestTime_delegatesWithLiftId() = runTest {
        val workout = sampleWorkoutWithTwoLifts() // lift id=10L has liftId=1000L
        modifyState { it.copy(workout = workout) }
        coEvery { updateRestTimeUseCase(liftId = 1000L, enabled = true, restTime = any()) } just runs

        viewModel.updateRestTime(workoutLiftId = 10L, newRestTime = kotlin.time.Duration.parse("30s"), enabled = true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { updateRestTimeUseCase(liftId = 1000L, enabled = true, restTime = any()) }
    }

    // --- action bar: FinishWorkout branches ---
    @Test
    fun actionBar_FinishWorkout_whenSummaryVisible_callsFinishWorkout() = runTest {
        // use a spy to avoid needing real inProgress/workout domain objects
        val spy = spyk(viewModel)
        every { spy.finishWorkout() } just runs

        // set state: summary visible
        val field = BaseWorkoutViewModel::class.java.getDeclaredField("mutableWorkoutState")
        field.isAccessible = true
        val flow = field.get(spy) as MutableStateFlow<WorkoutState>
        flow.value = flow.value.copy(isCompletionSummaryVisible = true)

        spy.handleActionBarEvents(TopAppBarEvent.ActionEvent(TopAppBarAction.FinishWorkout))
        verify(exactly = 1) { spy.finishWorkout() }
    }

    @Test
    fun actionBar_FinishWorkout_whenSummaryHidden_callsToggleCompletionSummary() = runTest {
        val spy = spyk(viewModel)
        every { spy.toggleCompletionSummary() } just runs

        // ensure hidden
        val field = BaseWorkoutViewModel::class.java.getDeclaredField("mutableWorkoutState")
        field.isAccessible = true
        val flow = field.get(spy) as MutableStateFlow<WorkoutState>
        flow.value = flow.value.copy(isCompletionSummaryVisible = false)

        spy.handleActionBarEvents(TopAppBarEvent.ActionEvent(TopAppBarAction.FinishWorkout))
        verify(exactly = 1) { spy.toggleCompletionSummary() }
    }

    // --- cancel workout: closes modal and calls use case ---
    @Test
    fun cancelWorkout_whenModalShown_closesModal_andCallsUseCase() = runTest {
        val program = com.browntowndev.liftlab.ui.models.workoutLogging.ActiveProgramMetadataUiModel(
            programId = 7L, name = "P", deloadWeek = 4,
            currentMesocycle = 0, currentMicrocycle = 0, currentMicrocyclePosition = 0,
            workoutCount = 3
        )
        modifyState {
            it.copy(
                programMetadata = program,
                workout = sampleWorkoutWithTwoLifts(),
                isConfirmCancelWorkoutDialogShown = true
            )
        }
        coEvery { cancelWorkoutUseCase(programMetadata = any(), workout = any()) } just runs

        viewModel.cancelWorkout()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { cancelWorkoutUseCase(programMetadata = any(), workout = any()) }
        assertFalse(viewModel.workoutState.value.isConfirmCancelWorkoutDialogShown)
    }
}
