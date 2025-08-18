
package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.AddSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ConvertWorkoutLiftTypeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.DeleteCustomSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.DeleteWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.GetWorkoutConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReorderWorkoutBuilderLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateCustomLiftSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateLiftIncrementOverrideUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateManyCustomLiftSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateRestTimeUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutLiftDeloadWeekUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutNameUseCase
import com.browntowndev.liftlab.ui.mapping.toDomainModel
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workout.CustomWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workout.DropSetUiModel
import com.browntowndev.liftlab.ui.models.workout.MyoRepSetUiModel
import com.browntowndev.liftlab.ui.models.workout.StandardSetUiModel
import com.browntowndev.liftlab.ui.models.workout.StandardWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutUiModel
import com.browntowndev.liftlab.ui.viewmodels.workoutBuilder.PickerType
import com.browntowndev.liftlab.ui.viewmodels.workoutBuilder.WorkoutBuilderState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutBuilderViewModelTest {

    // Use cases
    @RelaxedMockK lateinit var convertWorkoutLiftTypeUseCase: ConvertWorkoutLiftTypeUseCase
    @RelaxedMockK lateinit var reorderWorkoutBuilderLiftsUseCase: ReorderWorkoutBuilderLiftsUseCase
    @RelaxedMockK lateinit var deleteWorkoutLiftUseCase: DeleteWorkoutLiftUseCase
    @RelaxedMockK lateinit var updateWorkoutNameUseCase: UpdateWorkoutNameUseCase
    @RelaxedMockK lateinit var updateRestTimeUseCase: UpdateRestTimeUseCase
    @RelaxedMockK lateinit var updateLiftIncrementOverrideUseCase: UpdateLiftIncrementOverrideUseCase
    @RelaxedMockK lateinit var updateWorkoutLiftUseCase: UpdateWorkoutLiftUseCase
    @RelaxedMockK lateinit var deleteCustomLiftSetByPositionUseCase: DeleteCustomSetUseCase
    @RelaxedMockK lateinit var updateCustomLiftSetUseCase: UpdateCustomLiftSetUseCase
    @RelaxedMockK lateinit var updateManyCustomLiftsSetUseCase: UpdateManyCustomLiftSetsUseCase
    @RelaxedMockK lateinit var addSetUseCase: AddSetUseCase
    @RelaxedMockK lateinit var updateWorkoutLiftDeloadWeekUseCase: UpdateWorkoutLiftDeloadWeekUseCase
    @RelaxedMockK lateinit var getWorkoutConfigurationStateFlowUseCase: GetWorkoutConfigurationStateFlowUseCase
    @RelaxedMockK lateinit var eventBus: EventBus

    private lateinit var viewModel: WorkoutBuilderViewModel
    private val testDispatcher = StandardTestDispatcher()
    private var navigatedBack = false

    // Fixture: a preloaded workout with one standard and one custom lift
    private lateinit var standardLift: StandardWorkoutLiftUiModel
    private lateinit var customLift: CustomWorkoutLiftUiModel
    private lateinit var workout: WorkoutUiModel
    private lateinit var crashlytics: FirebaseCrashlytics

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Mock static accessor so no real FirebaseApp is needed
        mockkStatic(FirebaseCrashlytics::class)
        crashlytics = mockk(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics

        every { getWorkoutConfigurationStateFlowUseCase.invoke(any(), any()) } returns emptyFlow()

        every { eventBus.register(any()) } just Runs
        every { eventBus.unregister(any()) } just Runs

        navigatedBack = false

        viewModel = WorkoutBuilderViewModel(
            workoutId = 123L,
            liftLevelDeloadsEnabled = true,
            onNavigateBack = { navigatedBack = true },
            convertWorkoutLiftTypeUseCase = convertWorkoutLiftTypeUseCase,
            reorderWorkoutBuilderLiftsUseCase = reorderWorkoutBuilderLiftsUseCase,
            deleteWorkoutLiftUseCase = deleteWorkoutLiftUseCase,
            updateWorkoutNameUseCase = updateWorkoutNameUseCase,
            updateRestTimeUseCase = updateRestTimeUseCase,
            updateLiftIncrementOverrideUseCase = updateLiftIncrementOverrideUseCase,
            updateWorkoutLiftUseCase = updateWorkoutLiftUseCase,
            deleteCustomSetUseCase = deleteCustomLiftSetByPositionUseCase,
            updateCustomLiftSetUseCase = updateCustomLiftSetUseCase,
            updateManyCustomLiftSetsUseCase = updateManyCustomLiftsSetUseCase,
            addSetUseCase = addSetUseCase,
            updateWorkoutLiftDeloadWeekUseCase = updateWorkoutLiftDeloadWeekUseCase,
            getWorkoutConfigurationStateFlowUseCase = getWorkoutConfigurationStateFlowUseCase,
            eventBus = eventBus
        )

        // Build a simple workout fixture and inject directly into private _state
        val pattern = MovementPattern.entries.first()
        val scheme = ProgressionScheme.entries.first()

        standardLift = StandardWorkoutLiftUiModel(
            id = 10L,
            workoutId = 123L,
            liftId = 1001L,
            liftName = "Standard Lift",
            liftMovementPattern = pattern,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = 0,
            setCount = 3,
            progressionScheme = scheme,
            deloadWeek = null,
            incrementOverride = null,
            restTime = Duration.parse("PT60S"),
            restTimerEnabled = true,
            rpeTarget = 8f,
            repRangeBottom = 6,
            repRangeTop = 10,
            stepSize = 2,
            liftNote = null
        )

        customLift = CustomWorkoutLiftUiModel(
            id = 20L,
            workoutId = 123L,
            liftId = 1002L,
            liftName = "Custom Lift",
            liftMovementPattern = pattern,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = 1,
            progressionScheme = scheme,
            deloadWeek = null,
            incrementOverride = null,
            restTime = Duration.parse("PT60S"),
            restTimerEnabled = true,
            liftNote = null,
            customLiftSets = listOf(
                StandardSetUiModel(id = 201, workoutLiftId = 20L, position = 1, rpeTarget = 7.0f, repRangeBottom = 8, repRangeTop = 12),
                DropSetUiModel(id = 202, workoutLiftId = 20L, position = 2, rpeTarget = 7.5f, repRangeBottom = 10, repRangeTop = 12, dropPercentage = 0.2f),
                MyoRepSetUiModel(id = 203, workoutLiftId = 20L, position = 3, rpeTarget = 8.0f, repRangeBottom = 12, repRangeTop = 15, repFloor = 8, maxSets = 5, setMatching = false, setGoal = 3)
            )
        )

        workout = WorkoutUiModel(
            id = 123L,
            programId = 77L,
            name = "Workout A",
            position = 0,
            lifts = listOf(standardLift, customLift)
        )

        injectWorkout(workout, programDeloadWeek = 4)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun injectWorkout(workout: WorkoutUiModel, programDeloadWeek: Int) {
        val field = WorkoutBuilderViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        stateFlow.update { it.copy(workout = workout, programDeloadWeek = programDeloadWeek) }
    }

    // ---------- Basic state/UI behavior ----------

    @Test
    fun toggleMovementPatternDeletionModal_setsIdAndClears() = runTest {
        assertNull(viewModel.state.value.workoutLiftIdToDelete)

        viewModel.toggleMovementPatternDeletionModal(standardLift.id)
        assertEquals(standardLift.id, viewModel.state.value.workoutLiftIdToDelete)

        viewModel.toggleMovementPatternDeletionModal(null)
        assertNull(viewModel.state.value.workoutLiftIdToDelete)
    }

    @Test
    fun toggleWorkoutRenameModal_togglesFlag() = runTest {
        assertFalse(viewModel.state.value.isEditingName)
        viewModel.toggleWorkoutRenameModal()
        assertTrue(viewModel.state.value.isEditingName)
        viewModel.toggleWorkoutRenameModal()
        assertFalse(viewModel.state.value.isEditingName)
    }

    @Test
    fun toggleReorderLifts_togglesFlag() = runTest {
        assertFalse(viewModel.state.value.isReordering)
        viewModel.toggleReorderLifts()
        assertTrue(viewModel.state.value.isReordering)
        viewModel.toggleReorderLifts()
        assertFalse(viewModel.state.value.isReordering)
    }

    @Test
    fun toggleRpePicker_setsAndClearsPickerState() = runTest {
        viewModel.toggleRpePicker(
            visible = true,
            workoutLiftId = standardLift.id,
            position = 2,
            type = PickerType.Rpe,
            currentRpe = 8.5f,
            currentPercentage = 0.75f
        )
        val shown = viewModel.state.value.pickerState!!
        assertEquals(standardLift.id, shown.workoutLiftId)
        assertEquals(2, shown.setPosition)
        assertEquals(PickerType.Rpe, shown.type)
        assertEquals(8.5f, shown.currentRpe)
        assertEquals(0.75f, shown.currentPercentage)

        viewModel.toggleRpePicker(
            visible = false,
            workoutLiftId = standardLift.id,
            position = 2,
            type = PickerType.Rpe
        )
        assertNull(viewModel.state.value.pickerState)
    }

    @Test
    fun toggleDetailExpansion_togglesPositionsIndependently() = runTest {
        val liftId = standardLift.id
        assertTrue(viewModel.state.value.detailExpansionStates.isEmpty())

        viewModel.toggleDetailExpansion(liftId, 1)
        assertTrue(viewModel.state.value.detailExpansionStates[liftId]?.contains(1) == true)

        viewModel.toggleDetailExpansion(liftId, 3)
        val expanded = viewModel.state.value.detailExpansionStates[liftId]!!
        assertTrue(1 in expanded && 3 in expanded)

        viewModel.toggleDetailExpansion(liftId, 1)
        val after = viewModel.state.value.detailExpansionStates[liftId]!!
        assertTrue(3 in after && 1 !in after)
    }

    @Test
    fun handleActionBarEvents_invokesNavigateBackAndTogglesFlags() = runTest {
        assertFalse(navigatedBack)
        viewModel.handleActionBarEvents(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        assertTrue(navigatedBack)

        val beforeEdit = viewModel.state.value.isEditingName
        viewModel.handleActionBarEvents(TopAppBarEvent.ActionEvent(TopAppBarAction.RenameWorkout))
        assertEquals(!beforeEdit, viewModel.state.value.isEditingName)

        val beforeReorder = viewModel.state.value.isReordering
        viewModel.handleActionBarEvents(TopAppBarEvent.ActionEvent(TopAppBarAction.ReorderLifts))
        assertEquals(!beforeReorder, viewModel.state.value.isReordering)
    }

    // ---------- Delegations when workout is loaded ----------

    @Test
    fun setRestTime_delegatesWithLiftIdAndArgs() = runTest {
        val newTime = Duration.parse("PT90S")
        viewModel.setRestTime(workoutLiftId = customLift.id, newRestTime = newTime, enabled = true)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateRestTimeUseCase(customLift.liftId, true, newTime) }
    }

    @Test
    fun setIncrementOverride_delegatesWithLiftId() = runTest {
        viewModel.setIncrementOverride(workoutLiftId = customLift.id, newIncrement = 2.5f)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateLiftIncrementOverrideUseCase(customLift.liftId, 2.5f) }
    }

    @Test
    fun setLiftSetCount_updatesViaUseCase() = runTest {
        viewModel.setLiftSetCount(workoutLiftId = standardLift.id, newSetCount = 5)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateWorkoutLiftUseCase(workout.programId, any()) }
    }

    @Test
    fun setLiftRpeTarget_updatesViaUseCase() = runTest {
        viewModel.setLiftRpeTarget(workoutLiftId = standardLift.id, newRpeTarget = 9.0f)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateWorkoutLiftUseCase(workout.programId, any()) }
    }

    @Test
    fun updateDeloadWeek_delegatesWithProgramDeloadWeek() = runTest {
        viewModel.updateDeloadWeek(workoutLiftId = standardLift.id, newDeloadWeek = 2)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateWorkoutLiftDeloadWeekUseCase(workout.programId, any(), 2, 4) }
    }

    @Test
    fun toggleHasCustomLiftSets_delegatesToConvertUseCase() = runTest {
        viewModel.toggleHasCustomLiftSets(workoutLiftId = standardLift.id, enableCustomSets = true)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { convertWorkoutLiftTypeUseCase(workout.programId, any(), true) }
    }

    @Test
    fun deleteMovementPattern_usesCurrentDeletionId() = runTest {
        viewModel.toggleMovementPatternDeletionModal(standardLift.id)
        viewModel.deleteMovementPattern()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { deleteWorkoutLiftUseCase(any(), any()) }
    }

    @Test
    fun addSet_delegatesWithWorkoutLiftId() = runTest {
        viewModel.addSet(workoutLiftId = customLift.id)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { addSetUseCase(workout.programId, customLift.toDomainModel()) }
    }

    @Test
    fun deleteSet_delegatesToDeleteCustomLiftSetByPosition() = runTest {
        viewModel.deleteSet(workoutLiftId = customLift.id, position = 2)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { deleteCustomLiftSetByPositionUseCase(workout.programId, workout.id, customLift.id, 203L) }
    }

    @Test
    fun setCustomSetRpeTarget_updatesCustomSet() = runTest {
        viewModel.setCustomSetRpeTarget(workoutLiftId = customLift.id, position = 1, newRpeTarget = 8.5f)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateCustomLiftSetUseCase(workout.programId, workout.id, any()) }
    }

    @Test
    fun myorep_edits_delegateToUpdateCustomSetUseCase() = runTest {
        viewModel.setCustomSetRepFloor(workoutLiftId = customLift.id, position = 2, newRepFloor = 9)
        viewModel.setCustomSetUseSetMatching(workoutLiftId = customLift.id, position = 2, setMatching = true)
        viewModel.setCustomSetMatchSetGoal(workoutLiftId = customLift.id, position = 2, newMatchSetGoal = 4)
        viewModel.setCustomSetMaxSets(workoutLiftId = customLift.id, position = 2, newMaxSets = 7)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(atLeast = 4) { updateCustomLiftSetUseCase(workout.programId, workout.id, any()) }
    }

    @Test
    fun dropSet_edit_delegatesToUpdateCustomSetUseCase() = runTest {
        viewModel.setCustomSetDropPercentage(workoutLiftId = customLift.id, position = 1, newDropPercentage = 0.3f)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateCustomLiftSetUseCase(workout.programId, workout.id, any()) }
    }

    @Test fun edit_withOutOfBoundsIndex_doesNotCallUseCase() = runTest {
        viewModel.setCustomSetRepFloor(customLift.id, position = 99, newRepFloor = 9)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { updateCustomLiftSetUseCase(any(), any(), any()) }
        coVerify(exactly = 1) { crashlytics.recordException(any()) }
    }

    // ---------- Rep range editing ----------

    @Test
    fun setLiftRepRangeBottom_updatesStateOnly() = runTest {
        viewModel.setLiftRepRangeBottom(workoutLiftId = standardLift.id, newRepRangeBottom = 9)
        testDispatcher.scheduler.advanceUntilIdle()
        val updated = viewModel.state.value.workout!!.lifts.first { it.id == standardLift.id } as StandardWorkoutLiftUiModel
        assertEquals(9, updated.repRangeBottom)
    }

    @Test
    fun setLiftRepRangeTop_updatesStateOnly() = runTest {
        viewModel.setLiftRepRangeTop(workoutLiftId = standardLift.id, newRepRangeTop = 12)
        testDispatcher.scheduler.advanceUntilIdle()
        val updated = viewModel.state.value.workout!!.lifts.first { it.id == standardLift.id } as StandardWorkoutLiftUiModel
        assertEquals(12, updated.repRangeTop)
    }

    @Test
    fun updateWorkoutLiftRepRangeBottom_validatesAndSaves() = runTest {
        // make bottom invalid (>= top) to exercise validation path
        injectWorkout(
            workout.copy(
                lifts = workout.lifts.map {
                    if (it.id == standardLift.id) (it as StandardWorkoutLiftUiModel).copy(repRangeBottom = it.repRangeTop)
                    else it
                }
            ),
            programDeloadWeek = 4
        )
        viewModel.updateWorkoutLiftRepRangeBottom(workoutLiftId = standardLift.id)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateWorkoutLiftUseCase(workout.programId, any()) }
    }

    @Test
    fun updateWorkoutLiftRepRangeTop_validatesAndSaves() = runTest {
        // make top invalid (<= bottom) to exercise validation path
        injectWorkout(
            workout.copy(
                lifts = workout.lifts.map {
                    if (it.id == standardLift.id) (it as StandardWorkoutLiftUiModel).copy(repRangeTop = it.repRangeBottom)
                    else it
                }
            ),
            programDeloadWeek = 4
        )
        viewModel.updateWorkoutLiftRepRangeTop(workoutLiftId = standardLift.id)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { updateWorkoutLiftUseCase(workout.programId, any()) }
    }

    @Test
    fun guardedActions_doNotCallUseCases_whenWorkoutNotLoaded() = runTest {
        // Clear workout from state
        injectWorkout(workout = workout.copy(lifts = emptyList()), programDeloadWeek = 4)
        val field = WorkoutBuilderViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        stateFlow.update { it.copy(workout = null) }

        viewModel.setRestTime(workoutLiftId = 1L, newRestTime = Duration.parse("PT90S"), enabled = true)
        viewModel.setIncrementOverride(workoutLiftId = 1L, newIncrement = 2.5f)
        viewModel.reorderLifts(newLiftOrder = emptyList())
        viewModel.setLiftSetCount(workoutLiftId = 1L, newSetCount = 5)
        viewModel.setLiftRpeTarget(workoutLiftId = 1L, newRpeTarget = 8.0f)

        coVerify(exactly = 0) { updateRestTimeUseCase.invoke(any(), any(), any()) }
        coVerify(exactly = 0) { updateLiftIncrementOverrideUseCase.invoke(any(), any()) }
        coVerify(exactly = 0) { reorderWorkoutBuilderLiftsUseCase.invoke(any(), any(), any(), any()) }
        coVerify(exactly = 0) { updateWorkoutLiftUseCase.invoke(any(), any()) }
    }

    @Test
    fun changeCustomSetType_convertsPreviousMyoRepAndBatchesTwoUpdates() = runTest {
        // Rebuild the workout so that the set immediately BEFORE the edited one is a MyoRep.
        // Layout by LIST INDEX (0-based), since ViewModel uses list index for lookup:
        //   idx 0 -> Standard(pos=1, id=301)
        //   idx 1 -> MyoRep(pos=2,   id=302)  <-- previous
        //   idx 2 -> Standard(pos=3, id=303)  <-- this will be changed to DROP_SET
        val reorderedCustomLift = customLift.copy(
            customLiftSets = listOf(
                StandardSetUiModel(
                    id = 301L, workoutLiftId = customLift.id,
                    position = 0, rpeTarget = 7.0f, repRangeBottom = 8, repRangeTop = 12
                ),
                MyoRepSetUiModel(
                    id = 302L, workoutLiftId = customLift.id,
                    position = 1, rpeTarget = 8.0f, repRangeBottom = 12, repRangeTop = 15,
                    repFloor = 8, maxSets = 5, setMatching = false, setGoal = 3
                ),
                StandardSetUiModel(
                    id = 303L, workoutLiftId = customLift.id,
                    position = 2, rpeTarget = 7.5f, repRangeBottom = 10, repRangeTop = 12
                )
            )
        )
        val reorderedWorkout = workout.copy(
            lifts = workout.lifts.map { if (it.id == customLift.id) reorderedCustomLift else it }
        )
        injectWorkout(reorderedWorkout, programDeloadWeek = 4)
        testDispatcher.scheduler.advanceUntilIdle()

        // Capture the batch update
        val setsSlot = slot<List<GenericLiftSet>>()
        coEvery {
            updateManyCustomLiftsSetUseCase(
                programId = reorderedWorkout.programId,
                workoutId = reorderedWorkout.id,
                sets = capture(setsSlot)
            )
        } just Runs

        // Act: change the third item (index=2) to DROP_SET
        viewModel.changeCustomSetType(
            workoutLiftId = reorderedCustomLift.id,
            position = 2, // list index == 2
            newSetType = com.browntowndev.liftlab.core.domain.enums.SetType.DROP_SET
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: single batched call
        coVerify(exactly = 1) {
            updateManyCustomLiftsSetUseCase(
                reorderedWorkout.programId,
                reorderedWorkout.id,
                any()
            )
        }

        // Validate the transformed payload
        val updated = setsSlot.captured
        // Expect exactly 2 domain sets: previous(302) now StandardSet, current(303) now DropSet
        org.junit.jupiter.api.Assertions.assertEquals(2, updated.size)

        val byId = updated.associateBy { it.id }
        val prevDomain = byId[302L]
        val currDomain = byId[303L]

        assertNotNull(prevDomain, "Expected previous MyoRep (id=302) to be included after conversion")
        assertNotNull(currDomain, "Expected current set (id=303) to be included after transform")

        // Type assertions on the domain models
        assertTrue(
            prevDomain is StandardSet,
            "Previous MyoRep should be converted to StandardSet"
        )
        assertTrue(
            currDomain is DropSet,
            "Current set should be transformed to DropSet"
        )
    }

    @Test
    fun changeCustomSetType_preventMyoRepBeforeExistingDropSet_byConvertingDropToStandard_batched() = runTest {
        // Given a custom lift with Standard (pos=1, id=401) followed by DropSet (pos=2, id=402)
        val reorderedCustomLift = customLift.copy(
            customLiftSets = listOf(
                StandardSetUiModel(
                    id = 401L, workoutLiftId = customLift.id,
                    position = 0, rpeTarget = 7.0f, repRangeBottom = 8, repRangeTop = 12
                ),
                DropSetUiModel(
                    id = 402L, workoutLiftId = customLift.id,
                    position = 1, rpeTarget = 9.0f, repRangeBottom = 10, repRangeTop = 12,
                    dropPercentage = 0.20f
                )
            )
        )
        val reorderedWorkout = workout.copy(
            lifts = workout.lifts.map { if (it.id == customLift.id) reorderedCustomLift else it }
        )
        injectWorkout(reorderedWorkout, programDeloadWeek = 4)
        testDispatcher.scheduler.advanceUntilIdle()

        // Capture the batched update
        val setsSlot = slot<List<GenericLiftSet>>()
        coEvery {
            updateManyCustomLiftsSetUseCase(
                programId = reorderedWorkout.programId,
                workoutId = reorderedWorkout.id,
                sets = capture(setsSlot)
            )
        } just Runs

        // When: user changes the PREVIOUS standard (pos=1) to a MYOREP
        viewModel.changeCustomSetType(
            workoutLiftId = reorderedCustomLift.id,
            position = 0,
            newSetType = com.browntowndev.liftlab.core.domain.enums.SetType.MYOREP
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: expect one batched call that includes:
        //  - previous (id=401) transformed to MyoRep
        //  - following Drop (id=402) converted to Standard to maintain invariant
        coVerify(exactly = 1) {
            updateManyCustomLiftsSetUseCase(
                reorderedWorkout.programId,
                reorderedWorkout.id,
                any()
            )
        }

        val updated = setsSlot.captured
        org.junit.jupiter.api.Assertions.assertEquals(2, updated.size)

        val byId = updated.associateBy { it.id }
        val prevDomain = byId[401L]
        val nextDomain = byId[402L]

        assertNotNull(prevDomain, "Expected previous set (id=401) to be included after transform to MyoRep")
        assertNotNull(nextDomain, "Expected following DropSet (id=402) to be included for conversion to Standard")

        // This is the expected behavior that your current logic misses.
        assertTrue(
            prevDomain is com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet,
            "Previous set should be converted to MyoRep"
        )
        assertTrue(
            nextDomain is com.browntowndev.liftlab.core.domain.models.workout.StandardSet,
            "Following DropSet should be converted to Standard to avoid MyoRep -> Drop adjacency"
        )
    }
}
