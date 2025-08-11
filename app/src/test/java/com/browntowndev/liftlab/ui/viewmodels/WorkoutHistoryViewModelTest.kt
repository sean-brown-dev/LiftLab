
package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.useCase.metrics.GetSummarizedWorkoutMetricsStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteWorkoutLogEntryUseCase
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption.Companion.DATE_RANGE
import com.browntowndev.liftlab.ui.models.controls.FilterChipOption.Companion.PROGRAM
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workoutLogging.WorkoutLogEntryUiModel
import com.browntowndev.liftlab.ui.viewmodels.states.WorkoutHistoryState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModelTest {

    @RelaxedMockK lateinit var getSummarizedWorkoutMetricsStateFlowUseCase: GetSummarizedWorkoutMetricsStateFlowUseCase
    @RelaxedMockK lateinit var deleteWorkoutLogEntryUseCase: DeleteWorkoutLogEntryUseCase
    @RelaxedMockK lateinit var eventBus: EventBus

    private lateinit var viewModel: WorkoutHistoryViewModel
    private val mainDispatcher = StandardTestDispatcher()

    private var navigatedBack = false

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Crashlytics static
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        every { eventBus.register(any()) } just runs
        every { eventBus.unregister(any()) } just runs

        // Keep init quiet and independent from domain mapping
        every { getSummarizedWorkoutMetricsStateFlowUseCase.invoke() } returns emptyFlow()

        navigatedBack = false

        viewModel = WorkoutHistoryViewModel(
            getSummarizedWorkoutMetricsStateFlowUseCase = getSummarizedWorkoutMetricsStateFlowUseCase,
            deleteWorkoutLogEntryUseCase = deleteWorkoutLogEntryUseCase,
            onNavigateBack = { navigatedBack = true },
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

    private fun updateState(block: (WorkoutHistoryState) -> WorkoutHistoryState) {
        val field = WorkoutHistoryViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<WorkoutHistoryState>
        stateFlow.update(block)
    }

    private fun makeLog(id: Long, programId: Long, workoutId: Long, year: Int, month: Int, day: Int): WorkoutLogEntryUiModel {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month - 1, day, 0, 0, 0)
        }
        val date = Date(cal.timeInMillis)
        return WorkoutLogEntryUiModel(
            id = id,
            programId = programId,
            programName = "P$programId",
            workoutId = workoutId,
            workoutName = "W$workoutId",
            date = date,
            historicalWorkoutNameId = 0L,
            programWorkoutCount = 4,
            programDeloadWeek = 4,
            mesocycle = 1,
            microcycle = 1,
            microcyclePosition = 1,
            durationInMillis = 1000L,
            setLogEntries = emptyList(),
        )
    }

    // --- Tests ---

    @Test
    fun filterStarted_showsProgramAndWorkoutFilter() = runTest {
        assertFalse(viewModel.state.value.isProgramAndWorkoutFilterVisible)

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.FilterStarted))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isProgramAndWorkoutFilterVisible)
    }

    @Test
    fun navigatedBack_whenDatePickerVisible_togglesAndAppliesFilters() = runTest {
        // Seed logs and show date picker
        val logs = listOf(
            makeLog(1, 1, 10, 2024, 1, 1),
            makeLog(2, 1, 11, 2024, 1, 5),
            makeLog(3, 2, 20, 2024, 2, 1),
        )
        updateState { it.copy(dateOrderedWorkoutLogs = logs, filteredWorkoutLogs = logs, isDatePickerVisible = true) }

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isDatePickerVisible)
        // With no explicit date/program/workout filters, applyFilters should keep all logs
        assertEquals(logs.map { it.id }.toSet(), viewModel.state.value.filteredWorkoutLogs.map { it.id }.toSet())
    }

    @Test
    fun navigatedBack_whenFilterSheetVisible_appliesFiltersAndHidesSheet() = runTest {
        val logs = listOf(
            makeLog(1, 1, 10, 2024, 1, 1),
            makeLog(2, 2, 20, 2024, 2, 1),
        )
        updateState { it.copy(dateOrderedWorkoutLogs = logs, filteredWorkoutLogs = emptyList(), isProgramAndWorkoutFilterVisible = true) }

        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isProgramAndWorkoutFilterVisible)
        assertEquals(logs.map { it.id }.toSet(), viewModel.state.value.filteredWorkoutLogs.map { it.id }.toSet())
    }

    @Test
    fun navigatedBack_whenNothingVisible_navigatesBack() = runTest {
        assertFalse(navigatedBack)
        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        mainDispatcher.scheduler.advanceUntilIdle()
        assertTrue(navigatedBack)
    }

    @Test
    fun editDateRange_togglesPicker_andClosingAppliesFilters() = runTest {
        val logs = listOf(
            makeLog(1, 1, 10, 2024, 1, 1),
            makeLog(2, 1, 11, 2024, 1, 20),
            makeLog(3, 2, 20, 2024, 2, 1),
        )
        updateState { it.copy(dateOrderedWorkoutLogs = logs, filteredWorkoutLogs = logs) }

        // Open
        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.EditDateRange))
        mainDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isDatePickerVisible)

        // Set a range (Jan 1 - Jan 21)
        val start = logs[0].date.time
        val end = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2024, Calendar.JANUARY, 21, 0, 0, 0)
        }.timeInMillis
        viewModel.setDateRangeFilter(start = start, end = end)
        mainDispatcher.scheduler.advanceUntilIdle()

        // Close -> applies filters
        viewModel.handleTopAppBarActionEvent(TopAppBarEvent.ActionEvent(TopAppBarAction.EditDateRange))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isDatePickerVisible)
        val filteredIds = viewModel.state.value.filteredWorkoutLogs.map { it.id }.toSet()
        assertEquals(setOf(1L, 2L), filteredIds)

        // A date range chip should be present
        assertTrue(viewModel.state.value.filterChips.any { it.type == DATE_RANGE })
    }

    @Test
    fun addAndRemoveProgramFilter_updatesList_andApplyFiltersAddsChip() = runTest {
        updateState { it.copy(programAndWorkoutFilters = emptyList()) }
        val chip = FilterChipOption(type = PROGRAM, value = "Program A")

        viewModel.addWorkoutOrProgramFilter(chip)
        mainDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.programAndWorkoutFilters.contains(chip))

        // Removing via removeFilterChip should call applyFilters and drop the chip
        viewModel.removeFilterChip(chip)
        mainDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.programAndWorkoutFilters.contains(chip))
        assertTrue(viewModel.state.value.filterChips.none { it == chip })
    }

    @Test
    fun removeFilterChip_dateRange_resetsDates_andAppliesFilters() = runTest {
        // Seed a date range and logs
        val logs = listOf(
            makeLog(1, 1, 10, 2024, 1, 1),
            makeLog(2, 2, 20, 2024, 2, 1),
        )
        val start = logs[0].date.time
        val end = logs[1].date.time
        updateState {
            it.copy(
                dateOrderedWorkoutLogs = logs,
                filteredWorkoutLogs = emptyList(),
                startDateInMillis = start,
                endDateInMillis = end,
                filterChips = listOf(FilterChipOption(type = DATE_RANGE, value = "Jan 1 - Feb 1"))
            )
        }

        // Remove the date chip
        viewModel.removeFilterChip(FilterChipOption(type = DATE_RANGE, value = "Jan 1 - Feb 1"))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.startDateInMillis)
        assertNull(viewModel.state.value.endDateInMillis)
        // Filters reapplied; with no filters, all logs appear
        assertEquals(logs.size, viewModel.state.value.filteredWorkoutLogs.size)
        assertTrue(viewModel.state.value.filterChips.none { it.type == DATE_RANGE })
    }

    @Test
    fun delete_callsUseCase() = runTest {
        viewModel.delete(123L)
        mainDispatcher.scheduler.advanceUntilIdle()
        coVerify { deleteWorkoutLogEntryUseCase(123L) }
    }
}
