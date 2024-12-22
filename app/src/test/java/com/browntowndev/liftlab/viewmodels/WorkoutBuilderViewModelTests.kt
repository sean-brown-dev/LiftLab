package com.browntowndev.liftlab.viewmodels

import com.browntowndev.liftlab.core.common.ReorderableListItem
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutBuilderViewModelTest {

    private lateinit var viewModel: WorkoutBuilderViewModel
    private val mockProgramsRepository: ProgramsRepository = mockk()
    private val mockWorkoutsRepository: WorkoutsRepository = mockk()
    private val mockWorkoutLiftsRepository: WorkoutLiftsRepository = mockk()
    private val mockCustomLiftSetsRepository: CustomLiftSetsRepository = mockk()
    private val mockLiftsRepository: LiftsRepository = mockk()
    private val mockWorkoutInProgressRepository: WorkoutInProgressRepository = mockk()
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

        val dummyWorkout = WorkoutDto(
            id = workoutId,
            programId = 1,
            name = "Test Workout",
            position = 0,
            lifts = listOf(
                StandardWorkoutLiftDto(
                    id = 1,
                    workoutId = workoutId,
                    liftId = 1,
                    liftName = "Test Lift",
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

        coEvery { mockWorkoutsRepository.get(workoutId) } returns dummyWorkout
        coEvery { mockProgramsRepository.getDeloadWeek(any()) } returns 4

        viewModel = WorkoutBuilderViewModel(
            workoutId = workoutId,
            onNavigateBack = mockOnNavigateBack,
            programsRepository = mockProgramsRepository,
            workoutsRepository = mockWorkoutsRepository,
            workoutLiftsRepository = mockWorkoutLiftsRepository,
            customLiftSetsRepository = mockCustomLiftSetsRepository,
            liftsRepository = mockLiftsRepository,
            liftLevelDeloadsEnabled = liftLevelDeloadsEnabled,
            workoutInProgressRepository = mockWorkoutInProgressRepository,
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
        val liftToDelete = StandardWorkoutLiftDto(
            id = 1,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(liftToDelete))) }

        viewModel.toggleMovementPatternDeletionModal(liftToDelete.id)

        coEvery { mockWorkoutLiftsRepository.delete(any()) } just Runs

        viewModel.deleteMovementPattern()

        coVerify { mockWorkoutLiftsRepository.delete(liftToDelete) }
        assertFalse(viewModel.state.first().workout?.lifts?.contains(liftToDelete) ?: true)
        assertNull(viewModel.state.first().workoutLiftIdToDelete)
    }

    @Test
    fun `updateWorkoutName should update workout name and update state`() = runTest {
        val newName = "New Workout Name"

        coEvery { mockWorkoutsRepository.updateName(any(), any()) } just Runs

        viewModel.updateWorkoutName(newName)

        coVerify { mockWorkoutsRepository.updateName(workoutId, newName) }
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
        val standardLift = StandardWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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

        // Update the state to include a StandardWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(standardLift))) }

        coEvery { mockWorkoutLiftsRepository.update(any<CustomWorkoutLiftDto>()) } just Runs
        coEvery { mockCustomLiftSetsRepository.insertAll(any()) } returns listOf(1L)

        viewModel.toggleHasCustomLiftSets(workoutLiftId, true)

        coVerify { mockWorkoutLiftsRepository.update(match { it is CustomWorkoutLiftDto }) }
        coVerify { mockCustomLiftSetsRepository.insertAll(any()) }
        val updatedLift = viewModel.state.first().workout!!.lifts.find { it.id == workoutLiftId }
        assertTrue(updatedLift is CustomWorkoutLiftDto)
        assertFalse((updatedLift as CustomWorkoutLiftDto).customLiftSets.isEmpty())
    }

    @Test
    fun `toggleHasCustomLiftSets should update lift to StandardWorkoutLiftDto and delete custom sets`() = runTest {
        val workoutLiftId = 1L
        val customSets = listOf(
            StandardSetDto(
                workoutLiftId = workoutLiftId,
                position = 0,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10
            )
        )
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs
        coEvery { mockCustomLiftSetsRepository.deleteAllForLift(any()) } just Runs

        viewModel.toggleHasCustomLiftSets(workoutLiftId, false)

        coVerify { mockWorkoutLiftsRepository.update(match { it is StandardWorkoutLiftDto }) }
        coVerify { mockCustomLiftSetsRepository.deleteAllForLift(workoutLiftId) }
        val updatedLift = viewModel.state.first().workout!!.lifts.find { it.id == workoutLiftId }
        assertTrue(updatedLift is StandardWorkoutLiftDto)
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
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
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
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
        assertEquals(newIncrement, updatedLift?.incrementOverride)
    }

    @Test
    fun `reorderLifts should update lift positions and persist changes`() = runTest {
        val workoutLift1 = StandardWorkoutLiftDto(
            id = 1,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Lift 1",
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
        val workoutLift2 = StandardWorkoutLiftDto(
            id = 2,
            workoutId = workoutId,
            liftId = 2,
            liftName = "Lift 2",
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
            ReorderableListItem(workoutLift2.liftName, workoutLift2.id),
            ReorderableListItem(workoutLift1.liftName, workoutLift1.id)
        )

        // Update the state to include multiple lifts
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(workoutLift1, workoutLift2))) }

        coEvery { mockWorkoutLiftsRepository.updateMany(any()) } just Runs
        coEvery { mockWorkoutInProgressRepository.getWithoutCompletedSets() } returns null

        viewModel.reorderLifts(newLiftOrder)

        coVerify { mockWorkoutLiftsRepository.updateMany(any()) }
        val updatedLifts = viewModel.state.first().workout?.lifts
        assertEquals(workoutLift2.id, updatedLifts?.get(0)?.id)
        assertEquals(0, (updatedLifts?.get(0) as StandardWorkoutLiftDto).position)
        assertEquals(workoutLift1.id, updatedLifts?.get(1)?.id)
        assertEquals(1, (updatedLifts?.get(1) as StandardWorkoutLiftDto).position)
        assertFalse(viewModel.state.first().isReordering)
    }

    @Test
    fun `updateDeloadWeek should update deload week and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newDeloadWeek = 2

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

        viewModel.updateDeloadWeek(workoutLiftId, newDeloadWeek)

        coVerify { mockWorkoutLiftsRepository.update(match { (it as? StandardWorkoutLiftDto)?.deloadWeek == newDeloadWeek }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
        assertEquals(newDeloadWeek, updatedLift?.deloadWeek)
    }

    @Test
    fun `setLiftSetCount should update set count and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newSetCount = 5

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

        viewModel.setLiftSetCount(workoutLiftId, newSetCount)

        coVerify { mockWorkoutLiftsRepository.update(match { (it as? StandardWorkoutLiftDto)?.setCount == newSetCount }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
        assertEquals(newSetCount, updatedLift?.setCount)
    }

    @Test
    fun `setLiftRepRangeBottom should update rep range bottom and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newRepRangeBottom = 6

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

        viewModel.setLiftRepRangeBottom(workoutLiftId, newRepRangeBottom)

        coVerify { mockWorkoutLiftsRepository.update(match { (it as? StandardWorkoutLiftDto)?.repRangeBottom == newRepRangeBottom }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
        assertEquals(newRepRangeBottom, updatedLift?.repRangeBottom)
    }

    @Test
    fun `setLiftRepRangeTop should update rep range top and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newRepRangeTop = 12

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

        viewModel.setLiftRepRangeTop(workoutLiftId, newRepRangeTop)

        coVerify { mockWorkoutLiftsRepository.update(match { (it as? StandardWorkoutLiftDto)?.repRangeTop == newRepRangeTop }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
        assertEquals(newRepRangeTop, updatedLift?.repRangeTop)
    }

    @Test
    fun `setLiftRpeTarget should update RPE target and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newRpeTarget = 9f

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

        viewModel.setLiftRpeTarget(workoutLiftId, newRpeTarget)

        coVerify { mockWorkoutLiftsRepository.update(match { (it as? StandardWorkoutLiftDto)?.rpeTarget == newRpeTarget }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
        assertEquals(newRpeTarget, updatedLift?.rpeTarget)
    }

    @Test
    fun `setLiftProgressionScheme should update progression scheme and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newProgressionScheme = ProgressionScheme.DOUBLE_PROGRESSION

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

        viewModel.setLiftProgressionScheme(workoutLiftId, newProgressionScheme)

        coVerify { mockWorkoutLiftsRepository.update(match { (it as? StandardWorkoutLiftDto)?.progressionScheme == newProgressionScheme }) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
        assertEquals(newProgressionScheme, updatedLift?.progressionScheme)
    }

    @Test
    fun `updateStepSize should update step size and persist changes`() = runTest {
        val workoutLiftId = 1L
        val newStepSize = 3

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

        viewModel.updateStepSize(workoutLiftId, newStepSize)

        coVerify { mockWorkoutLiftsRepository.update(match { (it as? StandardWorkoutLiftDto)?.stepSize == newStepSize }) }
        val updatedLift = viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
        assertEquals(newStepSize, updatedLift?.stepSize)
    }

    @Test
    fun `addSet should add a new set to custom lift and update state`() = runTest {
        val workoutLiftId = 1L
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                StandardSetDto(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = 0,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.insert(any()) } returns 2L
        coEvery { mockWorkoutLiftsRepository.update(any<CustomWorkoutLiftDto>()) } just Runs

        viewModel.addSet(workoutLiftId)

        coVerify { mockCustomLiftSetsRepository.insert(any()) }
        coVerify { mockWorkoutLiftsRepository.update(any<CustomWorkoutLiftDto>()) }
        val updatedLift =
            viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto
        assertEquals(2, updatedLift?.setCount)
        assertEquals(2, updatedLift?.customLiftSets?.size)
        assertTrue(viewModel.state.first().detailExpansionStates[workoutLiftId]?.contains(1) ?: false)
    }

    @Test
    fun `deleteSet should remove set from custom lift and update state`() = runTest {
        val workoutLiftId = 1L
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                StandardSetDto(id = 1, workoutLiftId = workoutLiftId, position = 0, rpeTarget = 8f, repRangeBottom = 8, repRangeTop = 10),
                StandardSetDto(id = 2, workoutLiftId = workoutLiftId, position = 1, rpeTarget = 8f, repRangeBottom = 8, repRangeTop = 10)
            )
        )

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.deleteByPosition(any(), any()) } just Runs
        coEvery { mockWorkoutLiftsRepository.update(any<CustomWorkoutLiftDto>()) } just Runs

        viewModel.deleteSet(workoutLiftId, 0)

        coVerify { mockCustomLiftSetsRepository.deleteByPosition(workoutLiftId, 0) }
        coVerify { mockWorkoutLiftsRepository.update(any<CustomWorkoutLiftDto>()) }
        val updatedLift = viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto
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
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                StandardSetDto(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )
        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.update(any<StandardSetDto>()) } just Runs

        viewModel.setCustomSetRepRangeBottom(workoutLiftId, position, newRepRangeBottom)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    (it as? StandardSetDto)?.repRangeBottom == newRepRangeBottom
                }
            )
        }
        val updatedSet =
            (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
                ?.customLiftSets?.get(position) as? StandardSetDto
        assertEquals(newRepRangeBottom, updatedSet?.repRangeBottom)
    }

    @Test
    fun `setCustomSetRepRangeTop should update custom set rep range top and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newRepRangeTop = 12
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                StandardSetDto(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )
        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.update(any<StandardSetDto>()) } just Runs

        viewModel.setCustomSetRepRangeTop(workoutLiftId, position, newRepRangeTop)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    (it as? StandardSetDto)?.repRangeTop == newRepRangeTop
                }
            )
        }
        val updatedSet =
            (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
                ?.customLiftSets?.get(position) as? StandardSetDto
        assertEquals(newRepRangeTop, updatedSet?.repRangeTop)
    }

    @Test
    fun `setCustomSetRpeTarget should update custom set RPE target and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newRpeTarget = 9f
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                StandardSetDto(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }
        coEvery { mockCustomLiftSetsRepository.update(any<StandardSetDto>()) } just Runs

        viewModel.setCustomSetRpeTarget(workoutLiftId, position, newRpeTarget)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    (it as? StandardSetDto)?.rpeTarget == newRpeTarget
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
            ?.customLiftSets?.get(position) as? StandardSetDto
        assertEquals(newRpeTarget, updatedSet?.rpeTarget)
    }

    @Test
    fun `setCustomSetRepFloor should update custom set rep floor and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newRepFloor = 4
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                MyoRepSetDto(
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

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.update(any<MyoRepSetDto>()) } just Runs

        viewModel.setCustomSetRepFloor(workoutLiftId, position, newRepFloor)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    (it as? MyoRepSetDto)?.repFloor == newRepFloor
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
            ?.customLiftSets?.get(position) as? MyoRepSetDto
        assertEquals(newRepFloor, updatedSet?.repFloor)
    }

    @Test
    fun `setCustomSetUseSetMatching should update custom set use set matching and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val setMatching = true
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                MyoRepSetDto(
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

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.update(any<MyoRepSetDto>()) } just Runs

        viewModel.setCustomSetUseSetMatching(workoutLiftId, position, setMatching)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    (it as? MyoRepSetDto)?.setMatching == setMatching
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
            ?.customLiftSets?.get(position) as? MyoRepSetDto
        assertEquals(setMatching, updatedSet?.setMatching)
    }

    @Test
    fun `setCustomSetMatchSetGoal should update custom set match set goal and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newMatchSetGoal = 5
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                MyoRepSetDto(
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

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.update(any<MyoRepSetDto>()) } just Runs

        viewModel.setCustomSetMatchSetGoal(workoutLiftId, position, newMatchSetGoal)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    (it as? MyoRepSetDto)?.setGoal == newMatchSetGoal
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
            ?.customLiftSets?.get(position) as? MyoRepSetDto
        assertEquals(newMatchSetGoal, updatedSet?.setGoal)
    }

    @Test
    fun `setCustomSetMaxSets should update custom set max sets and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newMaxSets = 5
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                MyoRepSetDto(
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

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.update(any<MyoRepSetDto>()) } just Runs

        viewModel.setCustomSetMaxSets(workoutLiftId, position, newMaxSets)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    (it as? MyoRepSetDto)?.maxSets == newMaxSets
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
            ?.customLiftSets?.get(position) as? MyoRepSetDto
        assertEquals(newMaxSets, updatedSet?.maxSets)
    }

    @Test
    fun `setCustomSetDropPercentage should update custom set drop percentage and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newDropPercentage = 0.2f
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                DropSetDto(
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

        // Update the state to include a CustomWorkoutLiftDto
        //Simulate that there is a lift to delete by updating the private _state
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.update(any<DropSetDto>()) } just Runs

        viewModel.setCustomSetDropPercentage(workoutLiftId, position, newDropPercentage)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    (it as? DropSetDto)?.dropPercentage == newDropPercentage
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
            ?.customLiftSets?.get(position) as? DropSetDto
        assertEquals(newDropPercentage, updatedSet?.dropPercentage)
    }

    @Test
    fun `changeCustomSetType should update custom set type and persist changes`() = runTest {
        val workoutLiftId = 1L
        val position = 0
        val newSetType = SetType.DROP_SET
        val customLift = CustomWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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
                StandardSetDto(
                    id = 1,
                    workoutLiftId = workoutLiftId,
                    position = position,
                    rpeTarget = 8f,
                    repRangeBottom = 8,
                    repRangeTop = 10
                )
            )
        )

        // Update the state to include a CustomWorkoutLiftDto
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

        coEvery { mockCustomLiftSetsRepository.update(any<GenericLiftSet>()) } just Runs

        viewModel.changeCustomSetType(workoutLiftId, position, newSetType)

        coVerify {
            mockCustomLiftSetsRepository.update(
                match {
                    it is DropSetDto
                }
            )
        }
        val updatedSet = (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
            ?.customLiftSets?.get(position)
        assertTrue(updatedSet is DropSetDto)
    }

    @Test
    fun `confirmStandardSetRepRangeBottom should validate and update rep range bottom and persist changes`() =
        runTest {
            val workoutLiftId = 1L
            val initialRepRangeBottom = 10
            val initialRepRangeTop = 10
            val validatedRepRangeBottom = 9
            val standardWorkoutLift = StandardWorkoutLiftDto(
                id = workoutLiftId,
                workoutId = workoutId,
                liftId = 1,
                liftName = "Test Lift",
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

            // Update the state to include a StandardWorkoutLiftDto
            val viewModelClass = viewModel.javaClass
            val stateField = viewModelClass.getDeclaredField("_state")
            stateField.isAccessible = true
            val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
            mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(standardWorkoutLift))) }

            coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

            viewModel.confirmStandardSetRepRangeBottom(workoutLiftId)

            coVerify {
                mockWorkoutLiftsRepository.update(
                    match {
                        (it as? StandardWorkoutLiftDto)?.repRangeBottom == validatedRepRangeBottom
                    }
                )
            }
            val updatedLift =
                viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
            assertEquals(validatedRepRangeBottom, updatedLift?.repRangeBottom)
        }

    @Test
    fun `confirmStandardSetRepRangeTop should validate and update rep range top and persist changes`() = runTest {
        val workoutLiftId = 1L
        val initialRepRangeBottom = 8
        val initialRepRangeTop = 8
        val validatedRepRangeTop = 9
        val standardWorkoutLift = StandardWorkoutLiftDto(
            id = workoutLiftId,
            workoutId = workoutId,
            liftId = 1,
            liftName = "Test Lift",
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

        // Update the state to include a StandardWorkoutLiftDto
        val viewModelClass = viewModel.javaClass
        val stateField = viewModelClass.getDeclaredField("_state")
        stateField.isAccessible = true
        val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
        mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(standardWorkoutLift))) }

        coEvery { mockWorkoutLiftsRepository.update(any<StandardWorkoutLiftDto>()) } just Runs

        viewModel.confirmStandardSetRepRangeTop(workoutLiftId)

        coVerify {
            mockWorkoutLiftsRepository.update(
                match {
                    (it as? StandardWorkoutLiftDto)?.repRangeTop == validatedRepRangeTop
                }
            )
        }
        val updatedLift = viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? StandardWorkoutLiftDto
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
            val customLift = CustomWorkoutLiftDto(
                id = workoutLiftId,
                workoutId = workoutId,
                liftId = 1,
                liftName = "Test Lift",
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
                    StandardSetDto(
                        id = 1,
                        workoutLiftId = workoutLiftId,
                        position = position,
                        rpeTarget = 8f,
                        repRangeBottom = initialRepRangeBottom,
                        repRangeTop = initialRepRangeTop
                    )
                )
            )

            // Update the state to include a CustomWorkoutLiftDto
            val viewModelClass = viewModel.javaClass
            val stateField = viewModelClass.getDeclaredField("_state")
            stateField.isAccessible = true
            val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
            mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

            coEvery { mockCustomLiftSetsRepository.update(any<StandardSetDto>()) } just Runs

            viewModel.confirmCustomSetRepRangeBottom(workoutLiftId, position)

            coVerify {
                mockCustomLiftSetsRepository.update(
                    match {
                        (it as? StandardSetDto)?.repRangeBottom == validatedRepRangeBottom
                    }
                )
            }
            val updatedSet =
                (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
                    ?.customLiftSets?.get(position) as? StandardSetDto
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
            val customLift = CustomWorkoutLiftDto(
                id = workoutLiftId,
                workoutId = workoutId,
                liftId = 1,
                liftName = "Test Lift",
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
                    StandardSetDto(
                        id = 1,
                        workoutLiftId = workoutLiftId,
                        position = position,
                        rpeTarget = 8f,
                        repRangeBottom = initialRepRangeBottom,
                        repRangeTop = initialRepRangeTop
                    )
                )
            )

            // Update the state to include a CustomWorkoutLiftDto
            val viewModelClass = viewModel.javaClass
            val stateField = viewModelClass.getDeclaredField("_state")
            stateField.isAccessible = true
            val mutableStateFlow = stateField.get(viewModel) as MutableStateFlow<WorkoutBuilderState>
            mutableStateFlow.update { it.copy(workout = it.workout!!.copy(lifts = listOf(customLift))) }

            coEvery { mockCustomLiftSetsRepository.update(any<StandardSetDto>()) } just Runs

            viewModel.confirmCustomSetRepRangeTop(workoutLiftId, position)

            coVerify {
                mockCustomLiftSetsRepository.update(
                    match {
                        (it as? StandardSetDto)?.repRangeTop == validatedRepRangeTop
                    }
                )
            }
            val updatedSet =
                (viewModel.state.first().workout?.lifts?.find { it.id == workoutLiftId } as? CustomWorkoutLiftDto)
                    ?.customLiftSets?.get(position) as? StandardSetDto
            assertEquals(validatedRepRangeTop, updatedSet?.repRangeTop)
        }
}