package com.browntowndev.liftlab.viewmodels

import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.DropSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.StandardSet
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.repositories.standard.CustomLiftSetsRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.standard.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.standard.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.standard.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.standard.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.standard.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.standard.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.ui.viewmodels.WorkoutBuilderViewModel
import com.browntowndev.liftlab.ui.viewmodels.states.PickerType
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutBuilderState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutBuilderViewModelTests {

    private lateinit var viewModel: WorkoutBuilderViewModel
    private val mockProgramsRepository: ProgramsRepository = mockk()
    private val mockWorkoutsRepositoryImpl: WorkoutsRepositoryImpl = mockk()
    private val mockWorkoutLiftsRepositoryImpl: WorkoutLiftsRepositoryImpl = mockk()
    private val mockCustomLiftSetsRepositoryImpl: CustomLiftSetsRepositoryImpl = mockk()
    private val mockLiftsRepository: LiftsRepository = mockk()
    private val mockWorkoutInProgressRepositoryImpl: WorkoutInProgressRepositoryImpl = mockk()
    private val mockSetResultsRepository: PreviousSetResultsRepository = mockk()
    private val mockEventBus: EventBus = mockk(relaxUnitFun = true)
    private val mockOnNavigateBack: () -> Unit = mockk()

    private val workoutId = 1L
    private val liftLevelDeloadsEnabled = true

    @MockK
    private lateinit var transactionScope: TransactionScope

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        transactionScope = mockk<TransactionScope>()
        coEvery { transactionScope.execute(any()) } coAnswers {
            val function = args[0] as (suspend () -> Unit)
            function()
        }

        val dummyWorkoutEntity = Workout(
            id = workoutId,
            programId = 1,
            name = "Test WorkoutEntity",
            position = 0,
            lifts = listOf(
                StandardWorkoutLift(
                    id = 1,
                    workoutId = workoutId,
                    liftId = 1,
                    liftName = "Test LiftEntity",
                    liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                    liftVolumeTypes = VolumeType.CHEST.bitMask,
                    liftSecondaryVolumeTypes = null,
                    deloadWeek = 3,
                    liftNote = null,
                    position = 0,
                    setCount = 3,
                    repRangeBottom = 8,
                    repRangeTop = 10,
                    rpeTarget = 8f,
                    incrementOverride = null,
                    restTime = 2.minutes,
                    restTimerEnabled = true,
                    progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                    stepSize = 2,
                )
            ),
        )

        coEvery { mockWorkoutsRepositoryImpl.getFlow(workoutId) } returns flowOf(dummyWorkoutEntity)
        coEvery { mockProgramsRepository.getDeloadWeek(any()) } returns 4

        viewModel = WorkoutBuilderViewModel(
            workoutId = workoutId,
            onNavigateBack = mockOnNavigateBack,
            programsRepository = mockProgramsRepository,
            workoutsRepositoryImpl = mockWorkoutsRepositoryImpl,
            workoutLiftsRepositoryImpl = mockWorkoutLiftsRepositoryImpl,
            customLiftSetsRepositoryImpl = mockCustomLiftSetsRepositoryImpl,
            liftsRepository = mockLiftsRepository,
            liftLevelDeloadsEnabled = liftLevelDeloadsEnabled,
            workoutInProgressRepositoryImpl = mockWorkoutInProgressRepositoryImpl,
            setResultsRepository = mockSetResultsRepository,
            transactionScope = transactionScope,
            eventBus = mockEventBus
        )
    }

    @Test
    fun `handleActionBarEvents - NavigatedBack should call onNavigateBack`() {
        val actionEvent = TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack)
        every { mockOnNavigateBack.invoke() } just Runs

        viewModel.handleActionBarEvents(actionEvent)

        verify { mockOnNavigateBack.invoke() }
    }

    @Test
    fun `handleActionBarEvents - RenameWorkout should toggle workout rename modal`() = runTest {
        val actionEvent = TopAppBarEvent.ActionEvent(TopAppBarAction.RenameWorkout)

        viewModel.handleActionBarEvents(actionEvent)

        assertTrue(viewModel.state.first().isEditingName)
    }

    @Test
    fun `handleActionBarEvents - ReorderLifts should toggle reorder lifts`() = runTest {
        val actionEvent = TopAppBarEvent.ActionEvent(TopAppBarAction.ReorderLifts)

        viewModel.handleActionBarEvents(actionEvent)

        assertTrue(viewModel.state.first().isReordering)
    }

    @Test
    fun `toggleMovementPatternDeletionModal should update state with workoutLiftId`() = runTest {
        val workoutLiftId = 1L

        viewModel.toggleMovementPatternDeletionModal(workoutLiftId)

        assertEquals(workoutLiftId, viewModel.state.first().workoutLiftIdToDelete)
    }

    @Test
    fun `deleteMovementPattern should delete lift and update state`() = runTest {
        val liftEntityToDelete = StandardWorkoutLift(
            id = 1,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 3,
            repRangeBottom = 8,
            repRangeTop = 10,
            rpeTarget = 8f,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            stepSize = 2,
        )
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(liftEntityToDelete))) }

        viewModel.toggleMovementPatternDeletionModal(liftEntityToDelete.id)

        coEvery { mockWorkoutLiftsRepositoryImpl.delete(any()) } just Runs

        viewModel.deleteMovementPattern()

        coVerify { mockWorkoutLiftsRepositoryImpl.delete(liftEntityToDelete) }
        assertFalse(viewModel.state.first().workout?.lifts?.contains(liftEntityToDelete) ?: true)
        assertNull(viewModel.state.first().workoutLiftIdToDelete)
    }

    @Test
    fun `updateWorkoutName should update workout name and update state`() = runTest {
        val newName = "New WorkoutEntity Name"

        coEvery { mockWorkoutsRepositoryImpl.updateName(any(), any()) } just Runs

        viewModel.updateWorkoutName(newName)

        coVerify { mockWorkoutsRepositoryImpl.updateName(workoutId, newName) }
        assertEquals(newName, viewModel.state.first().workout?.name)
        assertFalse(viewModel.state.first().isEditingName)
    }

    @Test
    fun `toggleWorkoutRenameModal should toggle isEditingName in state`() = runTest {
        viewModel.toggleWorkoutRenameModal()

        assertTrue(viewModel.state.first().isEditingName)

        viewModel.toggleWorkoutRenameModal()

        assertFalse(viewModel.state.first().isEditingName)
    }

    @Test
    fun `toggleReorderLifts should toggle isReordering in state`() = runTest {
        viewModel.toggleReorderLifts()

        assertTrue(viewModel.state.first().isReordering)

        viewModel.toggleReorderLifts()

        assertFalse(viewModel.state.first().isReordering)
    }

    @Test
    fun `togglePicker with visible true should update pickerState`() = runTest {
        val workoutLiftId = 1L
        val position = 1
        val type = PickerType.Rpe
        val currentRpe = 8f
        val currentPercentage = 80f

        viewModel.togglePicker(true, workoutLiftId, position, type, currentRpe, currentPercentage)

        val pickerState = viewModel.state.first().pickerState
        assertNotNull(pickerState)
        assertEquals(workoutLiftId, pickerState?.workoutLiftId)
        assertEquals(position, pickerState?.setPosition)
        assertEquals(type, pickerState?.type)
        assertEquals(currentRpe, pickerState?.currentRpe)
        assertEquals(currentPercentage, pickerState?.currentPercentage)
    }

    @Test
    fun `togglePicker with visible false should set pickerState to null`() = runTest {
        viewModel.togglePicker(false, 1L, 1, PickerType.Rpe)

        assertNull(viewModel.state.first().pickerState)
    }

    @Test
    fun `toggleDetailExpansion should update detailExpansionStates`() = runTest {
        val workoutLiftId = 1L
        val position = 1

        viewModel.toggleDetailExpansion(workoutLiftId, position)

        assertTrue(viewModel.state.first().detailExpansionStates[workoutLiftId]?.contains(position) ?: false)

        viewModel.toggleDetailExpansion(workoutLiftId, position)

        assertFalse(viewModel.state.first().detailExpansionStates[workoutLiftId]?.contains(position) ?: true)
    }

    @Test
    fun `toggleHasCustomLiftSets should update lift to CustomWorkoutLiftDto and insert custom sets`() = runTest {
        val workoutLiftId = 1L
        val standardLiftEntity = StandardWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 3,
            repRangeBottom = 8,
            repRangeTop = 10,
            rpeTarget = 8f,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            stepSize = 2,
        )

        // Update the state to include a StandardWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(standardLiftEntity))) }

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<CustomWorkoutLift>()) } just Runs
        coEvery { mockCustomLiftSetsRepositoryImpl.insertMany(any()) } returns listOf(1L)

        viewModel.toggleHasCustomLiftSets(workoutLiftId, true)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { it is CustomWorkoutLift }) }
        coVerify { mockCustomLiftSetsRepositoryImpl.insertMany(any()) }
        val updatedLift = viewModel.state.first().workout!!.lifts.find { it.id == workoutLiftId }
        assertTrue(updatedLift is CustomWorkoutLift)
        assertFalse((updatedLift as CustomWorkoutLift).customLiftSets.isEmpty())
    }

    @Test
    fun `toggleHasCustomLiftSets should update lift to StandardWorkoutLiftDto and delete custom sets`() = runTest {
        val workoutLiftId = 1L
        val customSets = listOf(
            StandardSet(
                workoutLiftId = workoutLiftId,
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10
            )
        )
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 3,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = customSets
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs
        coEvery { mockCustomLiftSetsRepositoryImpl.deleteAllForLift(any()) } just Runs

        viewModel.toggleHasCustomLiftSets(workoutLiftId, false)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { it is StandardWorkoutLift }) }
        coVerify { mockCustomLiftSetsRepositoryImpl.deleteAllForLift(workoutLiftId) }
        val updatedLift = viewModel.state.first().workout!!.lifts.find { it.id == workoutLiftId }
        assertTrue(updatedLift is StandardWorkoutLift)
    }

    @Test
    fun `setRestTime should update rest time and rest timer enabled status`() = runTest {
        val workoutLiftId = 1L
        val newRestTime = 3.minutes
        val enabled = false

        coEvery { mockLiftsRepository.updateRestTime(any(), any(), any()) } just Runs

        viewModel.setRestTime(workoutLiftId, newRestTime, enabled)

        coVerify { mockLiftsRepository.updateRestTime(any(), enabled, newRestTime) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newRestTime, updatedLift?.restTime)
        assertEquals(enabled, updatedLift?.restTimerEnabled)
    }

    @Test
    fun `setIncrementOverride should update increment override`() = runTest {
        val workoutLiftId = 1L
        val newIncrement = 2.5f

        coEvery { mockLiftsRepository.updateIncrementOverride(any(), any()) } just Runs

        viewModel.setIncrementOverride(workoutLiftId, newIncrement)

        coVerify { mockLiftsRepository.updateIncrementOverride(any(), newIncrement) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newIncrement, updatedLift?.incrementOverride)
    }

    @Test
    fun `reorderLifts should update lift positions and persist changes`() = runTest {
        val workoutLiftEntity1 = StandardWorkoutLift(
            id = 1,
            workoutId = workoutId,
            liftId = 1,
            liftName = "LiftEntity 1",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = null,
            liftNote = null,
            position = 0,
            setCount = 3,
            repRangeBottom = 8,
            repRangeTop = 10,
            rpeTarget = 8f,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
            stepSize = 1
        )
        val workoutLiftEntity2 = StandardWorkoutLift(
            id = 2,
            workoutId = workoutId,
            liftId = 2,
            liftName = "LiftEntity 2",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = null,
            liftNote = null,
            position = 1,
            setCount = 4,
            repRangeBottom = 6,
            repRangeTop = 8,
            rpeTarget = 7f,
            incrementOverride = null,
            restTime = 3.minutes,
            restTimerEnabled = false,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            stepSize = 1
        )
        val newLiftOrder = listOf(
            ReorderableListItem(workoutLiftEntity2.liftName, workoutLiftEntity2.id),
            ReorderableListItem(workoutLiftEntity1.liftName, workoutLiftEntity1.id)
        )

        // Update the state to include multiple lifts
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(workoutLiftEntity1, workoutLiftEntity2))) }

        coEvery { mockWorkoutLiftsRepositoryImpl.updateMany(any()) } just Runs
        coEvery { mockWorkoutInProgressRepositoryImpl.getWithoutCompletedSets() } returns null

        viewModel.reorderLifts(newLiftOrder)

        coVerify { mockWorkoutLiftsRepositoryImpl.updateMany(any()) }
        val updatedLifts = viewModel.state.first().workout?.lifts
        assertEquals(workoutLiftEntity2.id, updatedLifts?.get(0)?.id)
        assertEquals(0, (updatedLifts?.get(0) as StandardWorkoutLift).position)
        assertEquals(workoutLiftEntity1.id, updatedLifts?.get(1)?.id)
        assertEquals(1, (updatedLifts?.get(1) as StandardWorkoutLift).position)
        assertFalse(viewModel.state.first().isReordering)
    }

    @Test
    fun `updateDeloadWeek should update deload week and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newDeloadWeek = 2

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

        viewModel.updateDeloadWeek(workoutLiftId, newDeloadWeek)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { (it as? StandardWorkoutLift)?.deloadWeek == newDeloadWeek }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newDeloadWeek, updatedLift?.deloadWeek)
    }

    @Test
    fun `setLiftSetCount should update set count and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newSetCount = 5

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

        viewModel.setLiftSetCount(workoutLiftId, newSetCount)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { (it as? StandardWorkoutLift)?.setCount == newSetCount }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newSetCount, updatedLift?.setCount)
    }

    @Test
    fun `setLiftRepRangeBottom should update rep range bottom and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newRepRangeBottom = 6

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

        viewModel.setLiftRepRangeBottom(workoutLiftId, newRepRangeBottom)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { (it as? StandardWorkoutLift)?.repRangeBottom == newRepRangeBottom }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newRepRangeBottom, updatedLift?.repRangeBottom)
    }

    @Test
    fun `setLiftRepRangeTop should update rep range top and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newRepRangeTop = 12

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

        viewModel.setLiftRepRangeTop(workoutLiftId, newRepRangeTop)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { (it as? StandardWorkoutLift)?.repRangeTop == newRepRangeTop }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newRepRangeTop, updatedLift?.repRangeTop)
    }

    @Test
    fun `setLiftRpeTarget should update RPE target and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newRpeTarget = 9f

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

        viewModel.setLiftRpeTarget(workoutLiftId, newRpeTarget)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { (it as? StandardWorkoutLift)?.rpeTarget == newRpeTarget }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newRpeTarget, updatedLift?.rpeTarget)
    }

    @Test
    fun `setLiftProgressionScheme should update progression scheme and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newProgressionScheme = ProgressionScheme.DOUBLE_PROGRESSION

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

        viewModel.setLiftProgressionScheme(workoutLiftId, newProgressionScheme)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { (it as? StandardWorkoutLift)?.progressionScheme == newProgressionScheme }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newProgressionScheme, updatedLift?.progressionScheme)
    }

    @Test
    fun `updateStepSize should update step size and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newStepSize = 3

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

        viewModel.updateStepSize(workoutLiftId, newStepSize)

        coVerify { mockWorkoutLiftsRepositoryImpl.update(match { (it as? StandardWorkoutLift)?.stepSize == newStepSize }) }
        val updatedLift = viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(newStepSize, updatedLift?.stepSize)
    }

    @Test
    fun `addSet should add a new set to custom lift and update state`() = runTest {
        val workoutLiftId = 1L
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                StandardSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.insert(any()) } returns 2L
        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<CustomWorkoutLift>()) } just Runs

        viewModel.addSet(workoutLiftId)

        coVerify { mockCustomLiftSetsRepositoryImpl.insert(any()) }
        coVerify { mockWorkoutLiftsRepositoryImpl.update(any<CustomWorkoutLift>()) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift
        assertEquals(2, updatedLift?.setCount)
        assertEquals(2, updatedLift?.customLiftSets?.size)
        assertTrue(viewModel.state.first().detailExpansionStates[workoutLiftId]?.contains(1) ?: false)
    }

    @Test
    fun `deleteSet should remove set from custom lift and update state`() = runTest {
        val workoutLiftId = 1L
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 2,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                StandardSet(id = 1, workoutLiftId = workoutLiftId, position = 0, rpeTarget = 8f, repRangeBottom = 8, repRangeTop = 10),
                StandardSet(id = 2, workoutLiftId = workoutLiftId, position = 1, rpeTarget = 8f, repRangeBottom = 8, repRangeTop = 10)
            )
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.deleteByPosition(any(), any()) } just Runs
        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<CustomWorkoutLift>()) } just Runs

        viewModel.deleteSet(workoutLiftId, 0)

        coVerify { mockCustomLiftSetsRepositoryImpl.deleteByPosition(workoutLiftId, 0) }
        coVerify { mockWorkoutLiftsRepositoryImpl.update(any<CustomWorkoutLift>()) }
        val updatedLift = viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift
        assertEquals(1, updatedLift?.setCount)
        assertEquals(1, updatedLift?.customLiftSets?.size)
        assertEquals(2L, updatedLift?.customLiftSets?.get(0)?.id)
        assertEquals(0, updatedLift?.customLiftSets?.get(0)?.position)
    }

    @Test
    fun `setCustomSetRepRangeBottom should update custom set rep range bottom and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newRepRangeBottom = 6
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                StandardSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )
        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<StandardSet>()) } just Runs

        viewModel.setCustomSetRepRangeBottom(workoutLiftId, position, newRepRangeBottom)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    (it as? StandardSet)?.repRangeBottom == newRepRangeBottom
                }
            )
        }
        val updatedSet =
            (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
                ?.customLiftSets?.get(position) as? StandardSet
        assertEquals(newRepRangeBottom, updatedSet?.repRangeBottom)
    }

    @Test
    fun `setCustomSetRepRangeTop should update custom set rep range top and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newRepRangeTop = 12
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                StandardSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )
        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<StandardSet>()) } just Runs

        viewModel.setCustomSetRepRangeTop(workoutLiftId, position, newRepRangeTop)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    (it as? StandardSet)?.repRangeTop == newRepRangeTop
                }
            )
        }
        val updatedSet =
            (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
                ?.customLiftSets?.get(position) as? StandardSet
        assertEquals(newRepRangeTop, updatedSet?.repRangeTop)
    }

    @Test
    fun `setCustomSetRpeTarget should update custom set RPE target and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newRpeTarget = 9f
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                StandardSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }
        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<StandardSet>()) } just Runs

        viewModel.setCustomSetRpeTarget(workoutLiftId, position, newRpeTarget)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    (it as? StandardSet)?.rpeTarget == newRpeTarget
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
            ?.customLiftSets?.get(position) as? StandardSet
        assertEquals(newRpeTarget, updatedSet?.rpeTarget)
    }

    @Test
    fun `setCustomSetRepFloor should update custom set rep floor and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newRepFloor = 4
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                MyoRepSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10,
                    repFloor = 5,
                    setMatching = false,
                    setGoal = 3
                )
            )
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<MyoRepSet>()) } just Runs

        viewModel.setCustomSetRepFloor(workoutLiftId, position, newRepFloor)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    (it as? MyoRepSet)?.repFloor == newRepFloor
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
            ?.customLiftSets?.get(position) as? MyoRepSet
        assertEquals(newRepFloor, updatedSet?.repFloor)
    }

    @Test
    fun `setCustomSetUseSetMatching should update custom set use set matching and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val setMatching = true
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                MyoRepSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10,
                    repFloor = 5,
                    setMatching = false,
                    setGoal = 3
                )
            )
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<MyoRepSet>()) } just Runs

        viewModel.setCustomSetUseSetMatching(workoutLiftId, position, setMatching)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    (it as? MyoRepSet)?.setMatching == setMatching
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
            ?.customLiftSets?.get(position) as? MyoRepSet
        assertEquals(setMatching, updatedSet?.setMatching)
    }

    @Test
    fun `setCustomSetMatchSetGoal should update custom set match set goal and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newMatchSetGoal = 5
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                MyoRepSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10,
                    repFloor = 5,
                    setMatching = true,
                    setGoal = 3
                )
            )
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<MyoRepSet>()) } just Runs

        viewModel.setCustomSetMatchSetGoal(workoutLiftId, position, newMatchSetGoal)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    (it as? MyoRepSet)?.setGoal == newMatchSetGoal
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
            ?.customLiftSets?.get(position) as? MyoRepSet
        assertEquals(newMatchSetGoal, updatedSet?.setGoal)
    }

    @Test
    fun `setCustomSetMaxSets should update custom set max sets and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newMaxSets = 5
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                MyoRepSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10,
                    repFloor = 5,
                    setMatching = false,
                    setGoal = 3,
                    maxSets = null,
                )
            )
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<MyoRepSet>()) } just Runs

        viewModel.setCustomSetMaxSets(workoutLiftId, position, newMaxSets)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    (it as? MyoRepSet)?.maxSets == newMaxSets
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
            ?.customLiftSets?.get(position) as? MyoRepSet
        assertEquals(newMaxSets, updatedSet?.maxSets)
    }

    @Test
    fun `setCustomSetDropPercentage should update custom set drop percentage and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newDropPercentage = 0.2f
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                DropSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10,
                    dropPercentage = 0.1f
                )
            )
        )

        // Update the state to include a CustomWorkoutLift
        //Simulate that there is a liftEntity to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<DropSet>()) } just Runs

        viewModel.setCustomSetDropPercentage(workoutLiftId, position, newDropPercentage)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    (it as? DropSet)?.dropPercentage == newDropPercentage
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
            ?.customLiftSets?.get(position) as? DropSet
        assertEquals(newDropPercentage, updatedSet?.dropPercentage)
    }

    @Test
    fun `changeCustomSetType should update custom set type and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newSetType = SetType.DROP_SET
        val customLiftEntity = CustomWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 1,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            customLiftSets = listOf(
                StandardSet(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )

        // Update the state to include a CustomWorkoutLift
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

        coEvery { mockCustomLiftSetsRepositoryImpl.update(any<GenericLiftSet>()) } just Runs

        viewModel.changeCustomSetType(workoutLiftId, position, newSetType)

        coVerify {
            mockCustomLiftSetsRepositoryImpl.update(
                match {
                    it is DropSet
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
            ?.customLiftSets?.get(position)
        assertTrue(updatedSet is DropSet)
    }

    @Test
    fun `confirmStandardSetRepRangeBottom should validate and update rep range bottom and persist changes`() =
        runTest {
            val workoutLiftId = 1L
            val initialRepRangeBottom = 10
            val initialRepRangeTop = 10
            val validatedRepRangeBottom = 9
            val standardWorkoutLiftEntity = StandardWorkoutLift(
                id = workoutLiftId,
                workoutId = workoutId,
                liftId = 1,
                liftName = "Test LiftEntity",
                liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                liftVolumeTypes = VolumeType.CHEST.bitMask,
                liftSecondaryVolumeTypes = null,
                deloadWeek = 3,
                liftNote = null,
                position = 0,
                setCount = 3,
                repRangeBottom = initialRepRangeBottom,
                repRangeTop = initialRepRangeTop,
                rpeTarget = 8f,
                incrementOverride = null,
                restTime = 2.minutes,
                restTimerEnabled = true,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                stepSize = 2
            )

            // Update the state to include a StandardWorkoutLift
            val viewModelClass = viewModel.javaClass
            val stateField = viewModelClass.getDeclaredField("_state")
            stateField.isAccessible = true
            val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
            mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(standardWorkoutLiftEntity))) }

            coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

            viewModel.confirmStandardSetRepRangeBottom(workoutLiftId)

            coVerify {
                mockWorkoutLiftsRepositoryImpl.update(
                    match {
                        (it as? StandardWorkoutLift)?.repRangeBottom == validatedRepRangeBottom
                    }
                )
            }
            val updatedLift =
                viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
            assertEquals(validatedRepRangeBottom, updatedLift?.repRangeBottom)
        }

    @Test
    fun `confirmStandardSetRepRangeTop should validate and update rep range top and persist changes`() = runTest {
        val workoutLiftId = 1L
        val initialRepRangeBottom = 8
        val initialRepRangeTop = 8
        val validatedRepRangeTop = 9
        val standardWorkoutLiftEntity = StandardWorkoutLift(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test LiftEntity",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = VolumeType.CHEST.bitMask,
            liftSecondaryVolumeTypes = null,
            deloadWeek = 3,
            liftNote = null,
            position = 0,
            setCount = 3,
            repRangeBottom = initialRepRangeBottom,
            repRangeTop = initialRepRangeTop,
            rpeTarget = 8f,
            incrementOverride = null,
            restTime = 2.minutes,
            restTimerEnabled = true,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            stepSize = 2
        )

        // Update the state to include a StandardWorkoutLift
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(standardWorkoutLiftEntity))) }

        coEvery { mockWorkoutLiftsRepositoryImpl.update(any<StandardWorkoutLift>()) } just Runs

        viewModel.confirmStandardSetRepRangeTop(workoutLiftId)

        coVerify {
            mockWorkoutLiftsRepositoryImpl.update(
                match {
                    (it as? StandardWorkoutLift)?.repRangeTop == validatedRepRangeTop
                }
            )
        }
        val updatedLift = viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLift
        assertEquals(validatedRepRangeTop, updatedLift?.repRangeTop)
    }

    @Test
    fun `confirmCustomSetRepRangeBottom should validate and update custom set rep range bottom and persist changes`() =
        runTest {
            val workoutLiftId = 1L
            val position = 0
            val initialRepRangeBottom = 10
            val initialRepRangeTop = 10
            val validatedRepRangeBottom = 9
            val customLiftEntity = CustomWorkoutLift(
                id = workoutLiftId,
                workoutId = workoutId,
                liftId = 1,
                liftName = "Test LiftEntity",
                liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                liftVolumeTypes = VolumeType.CHEST.bitMask,
                liftSecondaryVolumeTypes = null,
                deloadWeek = 3,
                liftNote = null,
                position = 0,
                setCount = 1,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                incrementOverride = null,
                restTime = 2.minutes,
                restTimerEnabled = true,
                customLiftSets = listOf(
                    StandardSet(
                        id = 1,
                        workoutLiftId = workoutLiftId,
                        position = position,
                        rpeTarget = 8f,
                        repRangeBottom = initialRepRangeBottom,
                        repRangeTop = initialRepRangeTop
                    )
                )
            )

            // Update the state to include a CustomWorkoutLift
            val viewModelClass = viewModel.javaClass
            val stateField = viewModelClass.getDeclaredField("_state")
            stateField.isAccessible = true
            val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
            mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

            coEvery { mockCustomLiftSetsRepositoryImpl.update(any<StandardSet>()) } just Runs

            viewModel.confirmCustomSetRepRangeBottom(workoutLiftId, position)

            coVerify {
                mockCustomLiftSetsRepositoryImpl.update(
                    match {
                        (it as? StandardSet)?.repRangeBottom == validatedRepRangeBottom
                    }
                )
            }
            val updatedSet =
                (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
                    ?.customLiftSets?.get(position) as? StandardSet
            assertEquals(validatedRepRangeBottom, updatedSet?.repRangeBottom)
        }

    @Test
    fun `confirmCustomSetRepRangeTop should validate and update custom set rep range top and persist changes`() =
        runTest {
            val workoutLiftId = 1L
            val position = 0
            val initialRepRangeBottom = 8
            val initialRepRangeTop = 8
            val validatedRepRangeTop = 9
            val customLiftEntity = CustomWorkoutLift(
                id = workoutLiftId,
                workoutId = workoutId,
                liftId = 1,
                liftName = "Test LiftEntity",
                liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                liftVolumeTypes = VolumeType.CHEST.bitMask,
                liftSecondaryVolumeTypes = null,
                deloadWeek = 3,
                liftNote = null,
                position = 0,
                setCount = 1,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                incrementOverride = null,
                restTime = 2.minutes,
                restTimerEnabled = true,
                customLiftSets = listOf(
                    StandardSet(
                        id = 1,
                        workoutLiftId = workoutLiftId,
                        position = position,
                        rpeTarget = 8f,
                        repRangeBottom = initialRepRangeBottom,
                        repRangeTop = initialRepRangeTop
                    )
                )
            )

            // Update the state to include a CustomWorkoutLift
            val viewModelClass = viewModel.javaClass
            val stateField = viewModelClass.getDeclaredField("_state")
            stateField.isAccessible = true
            val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
            mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLiftEntity))) }

            coEvery { mockCustomLiftSetsRepositoryImpl.update(any<StandardSet>()) } just Runs

            viewModel.confirmCustomSetRepRangeTop(workoutLiftId, position)

            coVerify {
                mockCustomLiftSetsRepositoryImpl.update(
                    match {
                        (it as? StandardSet)?.repRangeTop == validatedRepRangeTop
                    }
                )
            }
            val updatedSet =
                (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLift)
                    ?.customLiftSets?.get(position) as? StandardSet
            assertEquals(validatedRepRangeTop, updatedSet?.repRangeTop)
        }
}