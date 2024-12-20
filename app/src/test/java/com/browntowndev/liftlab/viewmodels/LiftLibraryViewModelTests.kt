package com.browntowndev.liftlab.viewmodels

import com.browntowndev.liftlab.core.common.FilterChipOption
import com.browntowndev.liftlab.core.common.FilterChipOption.Companion.MOVEMENT_PATTERN
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.repositories.LiftMetricChartRepository
import com.browntowndev.liftlab.core.persistence.repositories.LiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.TopAppBarAction
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.eventbus.TopAppBarEvent
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.ui.viewmodels.LiftLibraryViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.slot
import org.junit.Rule
import kotlin.time.Duration

@ExperimentalCoroutinesApi
class LiftLibraryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var liftsRepository: LiftsRepository

    @MockK
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository

    @MockK
    private lateinit var liftMetricChartRepository: LiftMetricChartRepository

    @MockK(relaxed = true)
    private lateinit var onNavigateHome: () -> Unit

    @MockK(relaxed = true)
    private lateinit var onNavigateToWorkoutBuilder: (workoutId: Long) -> Unit

    @MockK(relaxed = true)
    private lateinit var onNavigateToActiveWorkout: () -> Unit

    @MockK(relaxed = true)
    private lateinit var onNavigateToLiftDetails: (liftId: Long?) -> Unit

    @MockK
    private lateinit var transactionScope: TransactionScope

    private val eventBus = EventBus.getDefault()

    private lateinit var viewModel: LiftLibraryViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        transactionScope = mockk<TransactionScope>()
        coEvery { transactionScope.execute(any()) } coAnswers {
            val function = args[0] as (suspend () -> Unit)
            function()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initializeViewModel(
        workoutId: Long? = null,
        addAtPosition: Int? = null,
        initialMovementPatternFilter: String = "",
        newLiftMetricChartIds: List<Long> = emptyList()
    ) {
        viewModel = LiftLibraryViewModel(
            liftsRepository,
            workoutLiftsRepository,
            liftMetricChartRepository,
            onNavigateHome,
            onNavigateToWorkoutBuilder,
            onNavigateToActiveWorkout,
            onNavigateToLiftDetails,
            workoutId,
            addAtPosition,
            initialMovementPatternFilter,
            newLiftMetricChartIds,
            transactionScope,
            eventBus
        )
    }

    @Test
    fun `getFilteredLifts should return all lifts when no filters are applied`() = runTest {
        // Arrange
        val lifts = listOf(
            LiftDto(
                id = 1,
                name = "Lift 1",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2,
                incrementOverride = null,
                restTime = Duration.parse("3m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 2,
                name = "Lift 2",
                movementPattern = MovementPattern.HORIZONTAL_PULL,
                volumeTypesBitmask = 2,
                secondaryVolumeTypesBitmask = 4,
                incrementOverride = null,
                restTime = Duration.parse("2m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 3,
                name = "Lift 3",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 4,
                secondaryVolumeTypesBitmask = 8,
                incrementOverride = null,
                restTime = Duration.parse("5m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            )
        )
        val mockLiveData = mockk<LiveData<List<LiftDto>>>()
        val observerSlot = slot<Observer<List<LiftDto>>>()

        every { liftsRepository.getAllAsLiveData() } returns mockLiveData
        every { mockLiveData.observeForever(capture(observerSlot)) } answers {
            observerSlot.captured.onChanged(lifts)
        }
        every { mockLiveData.removeObserver(any()) } returns Unit

        initializeViewModel()

        // Act
        val state = viewModel.state.first()

        // Assert
        assertEquals(lifts, state.filteredLifts)
    }

    @Test
    fun `getFilteredLifts should filter by name correctly`() = runTest {
        // Arrange
        val lifts = listOf(
            LiftDto(
                id = 1,
                name = "Barbell Bench Press",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2,
                incrementOverride = null,
                restTime = Duration.parse("3m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 2,
                name = "Dumbbell Row",
                movementPattern = MovementPattern.HORIZONTAL_PULL,
                volumeTypesBitmask = 2,
                secondaryVolumeTypesBitmask = 4,
                incrementOverride = null,
                restTime = Duration.parse("2m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 3,
                name = "Barbell Squat",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 4,
                secondaryVolumeTypesBitmask = 8,
                incrementOverride = null,
                restTime = Duration.parse("5m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            )
        )
        val mockLiveData = mockk<LiveData<List<LiftDto>>>()
        val observerSlot = slot<Observer<List<LiftDto>>>()

        every { liftsRepository.getAllAsLiveData() } returns mockLiveData
        every { mockLiveData.observeForever(capture(observerSlot)) } answers {
            observerSlot.captured.onChanged(lifts)
        }
        every { mockLiveData.removeObserver(any()) } returns Unit

        initializeViewModel()

        // Act
        viewModel.handleTopAppBarPayloadEvent(
            TopAppBarEvent.PayloadActionEvent(
                TopAppBarAction.SearchTextChanged,
                "barbell"
            )
        )
        val state = viewModel.state.first()

        // Assert
        assertEquals(2, state.filteredLifts.size)
        assertTrue(state.filteredLifts.any { it.name == "Barbell Bench Press" })
        assertTrue(state.filteredLifts.any { it.name == "Barbell Squat" })
    }

    @Test
    fun `getFilteredLifts should filter by movement pattern correctly`() = runTest {
        // Arrange
        val lifts = listOf(
            LiftDto(
                id = 1,
                name = "Lift 1",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2,
                incrementOverride = null,
                restTime = Duration.parse("3m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 2,
                name = "Lift 2",
                movementPattern = MovementPattern.HORIZONTAL_PULL,
                volumeTypesBitmask = 2,
                secondaryVolumeTypesBitmask = 4,
                incrementOverride = null,
                restTime = Duration.parse("2m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 3,
                name = "Lift 3",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 4,
                secondaryVolumeTypesBitmask = 8,
                incrementOverride = null,
                restTime = Duration.parse("5m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            )
        )
        val mockLiveData = mockk<LiveData<List<LiftDto>>>()
        val observerSlot = slot<Observer<List<LiftDto>>>()

        every { liftsRepository.getAllAsLiveData() } returns mockLiveData
        every { mockLiveData.observeForever(capture(observerSlot)) } answers {
            observerSlot.captured.onChanged(lifts)
        }
        every { mockLiveData.removeObserver(any()) } returns Unit

        initializeViewModel()

        // Act
        viewModel.addMovementPatternFilter(FilterChipOption(MOVEMENT_PATTERN, "Horizontal Push"))
        viewModel.applyFilters()
        val state = viewModel.state.first()

        // Assert
        assertEquals(1, state.filteredLifts.size)
        assertEquals(MovementPattern.HORIZONTAL_PUSH, state.filteredLifts[0].movementPattern)
    }

    @Test
    fun `getFilteredLifts should filter by multiple movement patterns correctly`() = runTest {
        // Arrange
        val lifts = listOf(
            LiftDto(
                id = 1,
                name = "Lift 1",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2,
                incrementOverride = null,
                restTime = Duration.parse("3m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 2,
                name = "Lift 2",
                movementPattern = MovementPattern.HORIZONTAL_PULL,
                volumeTypesBitmask = 2,
                secondaryVolumeTypesBitmask = 4,
                incrementOverride = null,
                restTime = Duration.parse("2m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 3,
                name = "Lift 3",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 4,
                secondaryVolumeTypesBitmask = 8,
                incrementOverride = null,
                restTime = Duration.parse("5m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            )
        )
        val mockLiveData = mockk<LiveData<List<LiftDto>>>()
        val observerSlot = slot<Observer<List<LiftDto>>>()

        every { liftsRepository.getAllAsLiveData() } returns mockLiveData
        every { mockLiveData.observeForever(capture(observerSlot)) } answers {
            observerSlot.captured.onChanged(lifts)
        }
        every { mockLiveData.removeObserver(any()) } returns Unit

        initializeViewModel()

        // Act
        viewModel.addMovementPatternFilter(FilterChipOption(MOVEMENT_PATTERN, "Horizontal Push"))
        viewModel.addMovementPatternFilter(FilterChipOption(MOVEMENT_PATTERN, "Horizontal Pull"))
        viewModel.applyFilters()

        val state = viewModel.state.first()

        // Assert
        assertEquals(2, state.filteredLifts.size)
        assertTrue(state.filteredLifts.any { it.movementPattern == MovementPattern.HORIZONTAL_PUSH })
        assertTrue(state.filteredLifts.any { it.movementPattern == MovementPattern.HORIZONTAL_PULL })
    }

    @Test
    fun `getFilteredLifts should filter by name and movement pattern correctly`() = runTest {
        // Arrange
        val lifts = listOf(
            LiftDto(
                id = 1,
                name = "Barbell Bench Press",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2,
                incrementOverride = null,
                restTime = Duration.parse("3m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 2,
                name = "Dumbbell Row",
                movementPattern = MovementPattern.HORIZONTAL_PULL,
                volumeTypesBitmask = 2,
                secondaryVolumeTypesBitmask = 4,
                incrementOverride = null,
                restTime = Duration.parse("2m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 3,
                name = "Barbell Squat",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 4,
                secondaryVolumeTypesBitmask = 8,
                incrementOverride = null,
                restTime = Duration.parse("5m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            )
        )
        val mockLiveData = mockk<LiveData<List<LiftDto>>>()
        val observerSlot = slot<Observer<List<LiftDto>>>()

        every { liftsRepository.getAllAsLiveData() } returns mockLiveData
        every { mockLiveData.observeForever(capture(observerSlot)) } answers {
            observerSlot.captured.onChanged(lifts)
        }
        every { mockLiveData.removeObserver(any()) } returns Unit

        initializeViewModel()

        // Act
        viewModel.handleTopAppBarPayloadEvent(
            TopAppBarEvent.PayloadActionEvent(
                TopAppBarAction.SearchTextChanged,
                "barbell"
            )
        )
        viewModel.addMovementPatternFilter(FilterChipOption(MOVEMENT_PATTERN, "Horizontal Push"))
        viewModel.applyFilters()
        val state = viewModel.state.first()

        // Assert
        assertEquals(1, state.filteredLifts.size)
        assertEquals("Barbell Bench Press", state.filteredLifts[0].name)
    }

    @Test
    fun `getFilteredLifts should exclude lifts based on liftIdFilters`() = runTest {
        // Arrange
        val lifts = listOf(
            LiftDto(
                id = 1,
                name = "Lift 1",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2,
                incrementOverride = null,
                restTime = Duration.parse("3m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 2,
                name = "Lift 2",
                movementPattern = MovementPattern.HORIZONTAL_PULL,
                volumeTypesBitmask = 2,
                secondaryVolumeTypesBitmask = 4,
                incrementOverride = null,
                restTime = Duration.parse("2m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 3,
                name = "Lift 3",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 4,
                secondaryVolumeTypesBitmask = 8,
                incrementOverride = null,
                restTime = Duration.parse("5m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            )
        )
        val mockLiveData = mockk<LiveData<List<LiftDto>>>()
        val observerSlot = slot<Observer<List<LiftDto>>>()

        every { liftsRepository.getAllAsLiveData() } returns mockLiveData
        every { mockLiveData.observeForever(capture(observerSlot)) } answers {
            observerSlot.captured.onChanged(lifts)
        }
        every { mockLiveData.removeObserver(any()) } returns Unit
        coEvery { workoutLiftsRepository.getLiftIdsForWorkout(1) } returns listOf(1L, 3L)

        initializeViewModel(workoutId = 1)

        // Act
        val state = viewModel.state.first()

        // Assert
        assertEquals(1, state.filteredLifts.size)
        assertEquals(2, state.filteredLifts[0].id)
    }

    @Test
    fun `getFilteredLifts should apply all filters correctly`() = runTest {
        // Arrange
        val lifts = listOf(
            LiftDto(
                id = 1,
                name = "Barbell Bench Press",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2,
                incrementOverride = null,
                restTime = Duration.parse("3m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 2,
                name = "Dumbbell Row",
                movementPattern = MovementPattern.HORIZONTAL_PULL,
                volumeTypesBitmask = 2,
                secondaryVolumeTypesBitmask = 4,
                incrementOverride = null,
                restTime = Duration.parse("2m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 3,
                name = "Barbell Squat",
                movementPattern = MovementPattern.LEG_PUSH,
                volumeTypesBitmask = 4,
                secondaryVolumeTypesBitmask = 8,
                incrementOverride = null,
                restTime = Duration.parse("5m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
            LiftDto(
                id = 4,
                name = "Dumbbell Bench Press",
                movementPattern = MovementPattern.HORIZONTAL_PUSH,
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2,
                incrementOverride = null,
                restTime = Duration.parse("5m"),
                restTimerEnabled = true,
                isHidden = false,
                isBodyweight = false,
                note = null
            ),
        )

        val mockLiveData = mockk<LiveData<List<LiftDto>>>()
        val observerSlot = slot<Observer<List<LiftDto>>>()

        every { liftsRepository.getAllAsLiveData() } returns mockLiveData
        every { mockLiveData.observeForever(capture(observerSlot)) } answers {
            observerSlot.captured.onChanged(lifts)
        }
        every { mockLiveData.removeObserver(any()) } returns Unit
        coEvery { workoutLiftsRepository.getLiftIdsForWorkout(1) } returns listOf(1L, 3L)

        initializeViewModel(workoutId = 1)

        // Act
        viewModel.handleTopAppBarPayloadEvent(
            TopAppBarEvent.PayloadActionEvent(
                TopAppBarAction.SearchTextChanged,
                "dumbbell"
            )
        )
        viewModel.addMovementPatternFilter(FilterChipOption(MOVEMENT_PATTERN, MovementPattern.HORIZONTAL_PUSH.displayName()))
        viewModel.applyFilters()
        val state = viewModel.state.first()

        // Assert
        assertEquals(1, state.filteredLifts.size)
        assertEquals(4, state.filteredLifts[0].id)
    }
}