
package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.DeleteLiftUseCase
import com.browntowndev.liftlab.core.domain.useCase.liftConfiguration.GetFilterableLiftsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.metrics.CreateLiftMetricChartsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.CreateWorkoutLiftsFromLiftsUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration.ReplaceWorkoutLiftUseCase
import com.browntowndev.liftlab.ui.models.controls.Route
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.viewmodels.liftLibrary.LiftLibraryState
import com.browntowndev.liftlab.ui.viewmodels.liftLibrary.LiftLibraryViewModel
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
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LiftLibraryViewModelTest {

    // Use cases
    @RelaxedMockK
    lateinit var deleteLiftUseCase: DeleteLiftUseCase
    @RelaxedMockK lateinit var replaceWorkoutLiftUseCase: ReplaceWorkoutLiftUseCase
    @RelaxedMockK lateinit var createLiftMetricChartsUseCase: CreateLiftMetricChartsUseCase
    @RelaxedMockK lateinit var createWorkoutLiftsFromLiftsUseCase: CreateWorkoutLiftsFromLiftsUseCase
    @RelaxedMockK lateinit var getFilterableLiftsStateFlowUseCase: GetFilterableLiftsStateFlowUseCase
    @RelaxedMockK lateinit var eventBus: EventBus

    private lateinit var viewModel: LiftLibraryViewModel

    private var navigatedHome = false
    private var navigatedToWorkoutBuilderId: Long? = null
    private var navigatedToActiveWorkout = false
    private var navigatedToLiftDetailsId: Long? = -1L

    private val mainDispatcher = StandardTestDispatcher()

    // Crashlytics
    private lateinit var crashlytics: FirebaseCrashlytics

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Static Crashlytics
        mockkStatic(FirebaseCrashlytics::class)
        crashlytics = mockk(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics

        // EventBus no-ops
        every { eventBus.register(any()) } just Runs
        every { eventBus.unregister(any()) } just Runs

        // By default don't emit anything from the use case in init
        coEvery { getFilterableLiftsStateFlowUseCase.invoke(any()) } returns emptyFlow()

        navigatedHome = false
        navigatedToWorkoutBuilderId = null
        navigatedToActiveWorkout = false
        navigatedToLiftDetailsId = -1L

        viewModel = LiftLibraryViewModel(
            deleteLiftUseCase = deleteLiftUseCase,
            replaceWorkoutLiftUseCase = replaceWorkoutLiftUseCase,
            createLiftMetricChartsUseCase = createLiftMetricChartsUseCase,
            createWorkoutLiftsFromLiftsUseCase = createWorkoutLiftsFromLiftsUseCase,
            onNavigateHome = { navigatedHome = true },
            onNavigateToWorkoutBuilder = { id -> navigatedToWorkoutBuilderId = id },
            onNavigateToActiveWorkout = { navigatedToActiveWorkout = true },
            onNavigateToLiftDetails = { id -> navigatedToLiftDetailsId = id },
            getFilterableLiftsStateFlowUseCase = getFilterableLiftsStateFlowUseCase,
            workoutId = null,
            addAtPosition = null,
            initialMovementPatternFilter = "",
            newLiftMetricChartIds = emptyList(),
            eventBus = eventBus
        )

        mainDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
        Dispatchers.resetMain()
    }

    // ---- Helpers ----

    private fun updateState(block: (LiftLibraryState) -> LiftLibraryState) {
        // Access private _state via reflection and update it
        val field = LiftLibraryViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<LiftLibraryState>
        stateFlow.update(block)
    }

    // ---- Tests ----

    @Test
    fun filterStarted_togglesShowFilterSelection() = runTest {
        // Initially false
        assertFalse(viewModel.state.value.showFilterSelection)

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.FilterStarted))
        mainDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.showFilterSelection)

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.FilterStarted))
        mainDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.showFilterSelection)
    }

    @Test
    fun navigatedBack_appliesFiltersAndHidesFilterSheet_whenVisible() = runTest {
        // Force visible
        updateState { it.copy(showFilterSelection = true) }
        assertTrue(viewModel.state.value.showFilterSelection)

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.showFilterSelection)
        // filteredLifts are updated by applyFilters() but we don't need specific assertions here
    }

    @Test
    fun confirmAddLift_withChartIds_callsCreateLiftMetricCharts_andNavigatesHome() = runTest {
        updateState {
            it.copy(
                // No workout context; we want the metric-charts branch
                newLiftMetricChartIds = listOf(1L, 2L),
                selectedNewLifts = listOf(10L, 20L)
            )
        }

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.ConfirmAddLift))
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { createLiftMetricChartsUseCase(chartIds = listOf(1L, 2L), liftIds = match { it.containsAll(listOf(10L, 20L)) }) }
        assertTrue(navigatedHome)
    }

    @Test
    fun confirmAddLift_withoutChartIds_callsCreateWorkoutLiftsFromLifts_andNavigatesToBuilder() = runTest {
        updateState {
            it.copy(
                // Ensure we take the addWorkoutLifts path
                newLiftMetricChartIds = emptyList(),
                selectedNewLifts = listOf(10L, 20L),
                // Provide required navigation context
                workoutId = 777L,
                addAtPosition = 5,
                // filteredLifts can be empty; we verify the use case is invoked regardless of list contents
                filteredLifts = emptyList()
            )
        }

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.ConfirmAddLift))
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { createWorkoutLiftsFromLiftsUseCase(workoutId = 777L, firstPosition = 5, lifts = any()) }
        assertEquals(777L, navigatedToWorkoutBuilderId)
    }

    @Test
    fun createNewLift_opensLiftDetailsWithNull() = runTest {
        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.CreateNewLift))
        mainDispatcher.scheduler.advanceUntilIdle()
        assertNull(navigatedToLiftDetailsId)
    }

    @Test
    fun searchTextChanged_setsNameFilter_andAppliesFilters() = runTest {
        // Make it visible to observe the hide-on-apply behavior
        updateState { it.copy(showFilterSelection = true) }
        assertTrue(viewModel.state.value.showFilterSelection)

        viewModel.handleTopAppBarPayloadEvent(TopAppBarEvent.PayloadActionEvent(TopAppBarAction.SearchTextChanged, "bench"))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals("bench", viewModel.state.value.nameFilter)
        assertFalse(viewModel.state.value.showFilterSelection) // applyFilters() hides it
    }

    @Test
    fun addAndRemoveSelectedLift_updatesSelection() = runTest {
        assertTrue(viewModel.state.value.selectedNewLifts.isEmpty())

        viewModel.addSelectedLift(42L)
        mainDispatcher.scheduler.advanceUntilIdle()
        assertTrue(42L in viewModel.state.value.selectedNewLifts)

        viewModel.removeSelectedLift(42L)
        mainDispatcher.scheduler.advanceUntilIdle()
        assertFalse(42L in viewModel.state.value.selectedNewLifts)
    }

    @Test
    fun replaceWorkoutLift_routesBasedOnCaller() = runTest {
        // Provide workout id for navigateBackToWorkoutBuilder()
        updateState { it.copy(workoutId = 999L) }

        // Case 1: from WorkoutBuilder
        viewModel.replaceWorkoutLift(workoutLiftId = 123L, replacementLiftId = 456L, callerRouteId = Route.WorkoutBuilder.id)
        mainDispatcher.scheduler.advanceUntilIdle()
        coVerify { replaceWorkoutLiftUseCase(workoutId = 999L, workoutLiftId = 123L, replacementLiftId = 456L) }
        assertEquals(999L, navigatedToWorkoutBuilderId)

        // Reset navigation target
        navigatedToWorkoutBuilderId = null

        // Case 2: from anywhere else
        viewModel.replaceWorkoutLift(workoutLiftId = 111L, replacementLiftId = 222L, callerRouteId = -1L)
        mainDispatcher.scheduler.advanceUntilIdle()
        coVerify { replaceWorkoutLiftUseCase(workoutId = 999L, workoutLiftId = 111L, replacementLiftId = 222L) }
        assertTrue(navigatedToActiveWorkout)
    }
    // ---- Filtering tests ----

    @Test
    fun applyFilters_withName_only_filtersCaseInsensitive() = runTest {
        // Build lifts
        val mpA = MovementPattern.entries[0]
        val mpB = MovementPattern.entries[1]

        val bench = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 1L, name = "Bench Press", movementPattern = mpA,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )
        val bent = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 2L, name = "Bent Over Row", movementPattern = mpA,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )
        val squat = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 3L, name = "Back Squat", movementPattern = mpB,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )

        updateState { it.copy(allLifts = listOf(bench, bent, squat)) }

        // Trigger name filter via payload event (which calls applyFilters internally)
        viewModel.handleTopAppBarPayloadEvent(
            com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent.PayloadActionEvent(
                TopAppBarAction.SearchTextChanged, "ben"
            )
        )
        mainDispatcher.scheduler.advanceUntilIdle()

        val names = viewModel.state.value.filteredLifts.map { it.name }
        assertTrue("Bench Press" in names && "Bent Over Row" in names)
        assertFalse("Back Squat" in names)
    }

    @Test
    fun applyFilters_withMovementPattern_only_filtersCorrectly() = runTest {
        val mpA = MovementPattern.entries[0]
        val mpB = MovementPattern.entries[1]

        val a1 = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 10L, name = "A1", movementPattern = mpA,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )
        val a2 = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 11L, name = "A2", movementPattern = mpA,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )
        val b1 = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 12L, name = "B1", movementPattern = mpB,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )

        updateState { it.copy(allLifts = listOf(a1, a2, b1)) }

        // Add movement pattern filter (no implicit apply)
        val opt = com.browntowndev.liftlab.ui.models.controls.FilterChipOption(
            type = com.browntowndev.liftlab.ui.models.controls.FilterChipOption.MOVEMENT_PATTERN,
            value = a1.movementPatternDisplayName
        )
        viewModel.addMovementPatternFilter(opt)
        mainDispatcher.scheduler.advanceUntilIdle()

        // Now apply
        viewModel.applyFilters()
        mainDispatcher.scheduler.advanceUntilIdle()

        val ids = viewModel.state.value.filteredLifts.map { it.id }.toSet()
        assertEquals(setOf(10L, 11L), ids)
    }

    @Test
    fun applyFilters_combined_name_and_pattern_and_exclusionIds() = runTest {
        val mpA = MovementPattern.entries[0]
        val mpB = MovementPattern.entries[1]

        val aRow = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 100L, name = "Cable Row", movementPattern = mpA,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )
        val aCurl = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 101L, name = "Curl", movementPattern = mpA,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )
        val bRow = com.browntowndev.liftlab.ui.models.workout.LiftUiModel(
            id = 102L, name = "Chest Supported Row", movementPattern = mpB,
            volumeTypes = emptyList(), secondaryVolumeTypes = emptyList(),
            incrementOverride = null, restTime = null, restTimerEnabled = false,
            isBodyweight = false, note = null
        )

        // Start with all lifts and pre-seeded exclusion list (pretend workout already has aRow)
        updateState { it.copy(allLifts = listOf(aRow, aCurl, bRow), liftsToFilterOut = hashSetOf(100L)) }

        // Add MP filter for mpA and set name filter to "row"
        val mpFilter = com.browntowndev.liftlab.ui.models.controls.FilterChipOption(
            type = com.browntowndev.liftlab.ui.models.controls.FilterChipOption.MOVEMENT_PATTERN,
            value = aRow.movementPatternDisplayName
        )
        viewModel.addMovementPatternFilter(mpFilter)
        mainDispatcher.scheduler.advanceUntilIdle()

        // Name filter path uses payload event and calls applyFilters()
        viewModel.handleTopAppBarPayloadEvent(
            com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent.PayloadActionEvent(
                TopAppBarAction.SearchTextChanged, "row"
            )
        )
        mainDispatcher.scheduler.advanceUntilIdle()

        val names = viewModel.state.value.filteredLifts.map { it.name }.toSet()
        // Should include only rows in mpA, but also exclude id 100 (aRow) due to liftsToFilterOut
        assertEquals(setOf<String>(), names) // no results, since only matching "row" under mpA is excluded
    }

}
