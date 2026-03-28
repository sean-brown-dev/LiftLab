package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.programConfiguration.ProgramConfigurationState
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.CreateProgramUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.CreateWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.DeleteProgramUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.DeleteWorkoutUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.GetProgramConfigurationStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.ReorderWorkoutsUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.SetProgramAsActiveUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.UpdateProgramDeloadWeekUseCase
import com.browntowndev.liftlab.core.domain.useCase.programConfiguration.UpdateProgramNameUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.UpdateWorkoutNameUseCase
import com.browntowndev.liftlab.ui.mapping.toDomainModel
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workout.WorkoutUiModel
import com.browntowndev.liftlab.ui.viewmodels.lab.LabState
import com.browntowndev.liftlab.ui.viewmodels.lab.LabViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModelTest {
/*


    // Use cases
    @RelaxedMockK lateinit var updateProgramDeloadWeekUseCase: UpdateProgramDeloadWeekUseCase
    @RelaxedMockK lateinit var createProgramUseCase: CreateProgramUseCase
    @RelaxedMockK lateinit var createWorkoutUseCase: CreateWorkoutUseCase
    @RelaxedMockK lateinit var updateWorkoutNameUseCase: UpdateWorkoutNameUseCase
    @RelaxedMockK lateinit var updateProgramNameUseCase: UpdateProgramNameUseCase
    @RelaxedMockK lateinit var deleteWorkoutUseCase: DeleteWorkoutUseCase
    @RelaxedMockK lateinit var deleteProgramUseCase: DeleteProgramUseCase
    @RelaxedMockK lateinit var reorderWorkoutsUseCase: ReorderWorkoutsUseCase
    @RelaxedMockK lateinit var setProgramAsActiveUseCase: SetProgramAsActiveUseCase
    @RelaxedMockK lateinit var getProgramConfigurationStateFlowUseCase: GetProgramConfigurationStateFlowUseCase
    @RelaxedMockK lateinit var eventBus: EventBus

    private lateinit var viewModel: LabViewModel
    private val mainDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Crashlytics static
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        every { eventBus.register(any()) } just Runs
        every { eventBus.unregister(any()) } just Runs

        // Keep init quiet
        every { getProgramConfigurationStateFlowUseCase.invoke() } returns
            flowOf(
                ProgramConfigurationState(
                allPrograms = emptyList(),
                program = Program(
                    id = 1L,
                    name = "Program",
                    isActive = true,
                    deloadWeek = 4,
                    workouts = emptyList()
                )
            ))

        viewModel = LabViewModel(
            updateProgramDeloadWeekUseCase = updateProgramDeloadWeekUseCase,
            createProgramUseCase = createProgramUseCase,
            createWorkoutUseCase = createWorkoutUseCase,
            updateWorkoutNameUseCase = updateWorkoutNameUseCase,
            updateProgramNameUseCase = updateProgramNameUseCase,
            deleteWorkoutUseCase = deleteWorkoutUseCase,
            deleteProgramUseCase = deleteProgramUseCase,
            reorderWorkoutsUseCase = reorderWorkoutsUseCase,
            setProgramAsActiveUseCase = setProgramAsActiveUseCase,
            getProgramConfigurationStateFlowUseCase = getProgramConfigurationStateFlowUseCase,
            eventBus = eventBus
        )

        mainDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
        Dispatchers.resetMain()
    }

    // --- Helpers ---

    private fun updateState(block: (LabState) -> LabState) {
        val field = LabViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<LabState>
        flow.update(block)
    }

    private fun makeWorkout(
        id: Long,
        programId: Long,
        name: String = "W$id",
        position: Int = 0
    ): WorkoutUiModel = WorkoutUiModel(
        id = id,
        programId = programId,
        name = name,
        position = position,
        lifts = emptyList()
    )

    // --- UI state toggles ---

    @Test
    fun toggleEditDeloadWeek_togglesFlag() = runTest {
        assertFalse(viewModel.state.value.isEditingDeloadWeek)
        viewModel.toggleEditDeloadWeek()
        assertTrue(viewModel.state.value.isEditingDeloadWeek)
        viewModel.toggleEditDeloadWeek()
        assertFalse(viewModel.state.value.isEditingDeloadWeek)
    }

    @Test
    fun toggleCreateProgramModal_togglesFlag() = runTest {
        assertFalse(viewModel.state.value.isCreatingProgram)
        viewModel.toggleCreateProgramModal()
        assertTrue(viewModel.state.value.isCreatingProgram)
        viewModel.toggleCreateProgramModal()
        assertFalse(viewModel.state.value.isCreatingProgram)
    }

    @Test
    fun showAndCollapseEditWorkoutNameModal_setsAndClearsFields() = runTest {
        viewModel.showEditWorkoutNameModal(workoutIdToRename = 10L, originalWorkoutName = "Old")
        assertEquals(10L, viewModel.state.value.workoutIdToRename)
        assertEquals("Old", viewModel.state.value.originalWorkoutName)

        viewModel.collapseEditWorkoutNameModal()
        assertNull(viewModel.state.value.workoutIdToRename)
        assertNull(viewModel.state.value.originalWorkoutName)
    }

    @Test
    fun renameProgramAction_showsAndCollapsesProgramNameModal() = runTest {
        assertFalse(viewModel.state.value.isEditingProgramName)

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.RenameProgram))
        mainDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isEditingProgramName)

        viewModel.collapseEditProgramNameModal()
        assertFalse(viewModel.state.value.isEditingProgramName)
    }

    @Test
    fun reordering_and_managePrograms_toggles_andNavigatedBackTurnsBothOff() = runTest {
        assertFalse(viewModel.state.value.isReordering)
        assertFalse(viewModel.state.value.isManagingPrograms)

        viewModel.toggleReorderingScreen()
        assertTrue(viewModel.state.value.isReordering)

        viewModel.toggleManageProgramsScreen()
        assertTrue(viewModel.state.value.isManagingPrograms)

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isReordering)
        assertFalse(viewModel.state.value.isManagingPrograms)
    }

    // --- Create / update flows ---

    @Test
    fun createProgram_usesIsActiveWhenNotManaging_andPassesCurrentActive() = runTest {
        coEvery { createProgramUseCase(name = any(), isActive = any(), currentActiveProgram = any()) } just Runs

        viewModel.createProgram("New Program")
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            createProgramUseCase(name = "New Program", isActive = true, currentActiveProgram = viewModel.state.value.program!!.toDomainModel())
        }
    }

    @Test
    fun updateWorkoutName_callsUseCase_whenChanged() = runTest {
        coEvery { updateWorkoutNameUseCase(any(), any(), any()) } just Runs

        viewModel.showEditWorkoutNameModal(55L, "Old")
        viewModel.updateWorkoutName(55L, "New")
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { updateWorkoutNameUseCase(any(), 55L, "New") }
    }

    @Test
    fun updateWorkoutName_sameAsOriginal_collapsesModal_andDoesNotCallUseCase() = runTest {
        viewModel.showEditWorkoutNameModal(55L, "Old")
        viewModel.updateWorkoutName(55L, "Old")
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { updateWorkoutNameUseCase(any(), any(), any()) }
        assertNull(viewModel.state.value.workoutIdToRename)
        assertNull(viewModel.state.value.originalWorkoutName)
    }

    // --- Delete flows ---

    @Test
    fun beginAndCancelDeleteWorkout_setsAndClearsTarget() = runTest {
        val w = makeWorkout(1L, 99L)
        viewModel.beginDeleteWorkout(w)
        assertEquals(w, viewModel.state.value.workoutToDelete)

        viewModel.cancelDeleteWorkout()
        assertNull(viewModel.state.value.workoutToDelete)
    }

    @Test
    fun deleteWorkout_delegatesWithMappedDomain() = runTest {
        val w = makeWorkout(1L, 99L)
        val expectedDomain = w.toDomainModel()

        coJustRun { deleteWorkoutUseCase(any()) }

        viewModel.deleteWorkout(w)
        mainDispatcher.scheduler.advanceUntilIdle()

        // We don't need to know the domain type; eq(expectedDomain) is enough
        coVerify(exactly = 1) { deleteWorkoutUseCase(eq(expectedDomain)) }
    }

    @Test
    fun beginAndCancelDeleteProgram_toggleFlags() = runTest {
        viewModel.beginDeleteProgram(42L)
        assertTrue(viewModel.state.value.isDeletingProgram)
        assertEquals(42L, viewModel.state.value.idOfProgramToDelete)

        viewModel.cancelDeleteProgram()
        assertFalse(viewModel.state.value.isDeletingProgram)
        assertNull(viewModel.state.value.idOfProgramToDelete)
    }

    @Test
    fun deleteProgram_callsUseCase() = runTest {
        coEvery { deleteProgramUseCase(any()) } just Runs

        viewModel.deleteProgram(77L)
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { deleteProgramUseCase(77L) }
    }

    // --- Set active program ---

    @Test
    fun setProgramAsActive_callsUseCase_whenDifferentProgram() = runTest {
        // program == null -> considered different; allPrograms empty -> passes empty list
        coEvery { setProgramAsActiveUseCase(any(), any()) } just Runs

        viewModel.setProgramAsActive(999L)
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            setProgramAsActiveUseCase(999L, withArg { list -> assertTrue(list.isEmpty()) })
        }
    }

*/
}
