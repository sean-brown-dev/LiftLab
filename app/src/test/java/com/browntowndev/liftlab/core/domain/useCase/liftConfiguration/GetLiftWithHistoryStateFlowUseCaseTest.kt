package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import app.cash.turbine.test
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.util.Date
import java.util.concurrent.TimeUnit

class GetLiftWithHistoryStateFlowUseCaseTest {

    private lateinit var liftsRepository: LiftsRepository
    private lateinit var workoutLogRepository: WorkoutLogRepository
    private lateinit var getLiftWithHistoryStateFlowUseCase: GetLiftWithHistoryStateFlowUseCase

    // Test Data
    private val testLift = Lift(
        id = 1L,
        name = "Barbell Bench Press",
        movementPattern = MovementPattern.HORIZONTAL_PUSH,
        volumeTypesBitmask = VolumeType.AB.bitMask,
        isBodyweight = false,
        restTimerEnabled = true,
        note = null,
        secondaryVolumeTypesBitmask = null,
        incrementOverride = null,
        restTime = null,
    )
    private val date1 = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10))
    private val date2 = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5))
    private val date3 = Date(System.currentTimeMillis())

    // Sets are defined out of order to test sorting
    private val set1 = createTestSetLogEntry(reps = 10, weight = 100f) // 1RM ~133.3, Volume = 1000
    private val set2 = createTestSetLogEntry(reps = 5, weight = 120f)  // 1RM ~140.0, Volume = 600
    private val set3 = createTestSetLogEntry(reps = 1, weight = 135f)  // 1RM  135.0, Volume = 135
    private val set4 = createTestSetLogEntry(reps = 3, weight = 130f)  // 1RM ~143.0, Volume = 390
    private val set5 = createTestSetLogEntry(
        reps = 8,
        weight = 110f
    )  // 1RM ~140.0, Volume = 880 (lower 1RM than set2 due to formula precision)

    private val workoutLog1 = createTestWorkoutLogEntry(date = date1, setResults = listOf(set1, set5))
    private val workoutLog2 = createTestWorkoutLogEntry(date = date2, setResults = listOf(set2))
    private val workoutLog3 = createTestWorkoutLogEntry(date = date3, setResults = listOf(set3, set4))

    private val testWorkoutLogs = listOf(workoutLog1, workoutLog2, workoutLog3)

    /**
     * Helper to create a SetLogEntry with defaults for non-essential test properties.
     * Focuses on reps and weight, which are crucial for calculations.
     */
    private fun createTestSetLogEntry(
        reps: Int,
        weight: Float,
        id: Long = (0..10000L).random(), // Use random ID to avoid accidental equality
        rpe: Float = 10f, // RPE 10 is a common value for max effort sets, affecting 1RM
    ): SetLogEntry {
        return SetLogEntry(
            id = id,
            workoutLogEntryId = 1L,
            liftId = 1L,
            workoutLiftDeloadWeek = null,
            liftName = "Test Lift",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            setType = SetType.STANDARD,
            liftPosition = 1,
            setPosition = 1,
            myoRepSetPosition = null,
            repRangeTop = 10,
            repRangeBottom = 8,
            rpeTarget = 8.5f,
            weightRecommendation = null,
            weight = weight,
            reps = reps,
            rpe = rpe,
            persistedOneRepMax = null,
            isPersonalRecord = false,
            setMatching = null,
            maxSets = null,
            repFloor = null,
            dropPercentage = null,
            isDeload = false
        )
    }

    /**
     * Helper to create a WorkoutLogEntry with defaults for non-essential test properties.
     * Focuses on the date and the list of sets, which are crucial for history.
     */
    private fun createTestWorkoutLogEntry(
        date: Date,
        setResults: List<SetLogEntry>,
        id: Long = (0..10000L).random(), // Use random ID
    ): WorkoutLogEntry {
        return WorkoutLogEntry(
            id = id,
            historicalWorkoutNameId = 1L,
            programWorkoutCount = 1,
            programDeloadWeek = 4,
            programName = "Test Program",
            workoutName = "Test Workout A",
            programId = 1L,
            workoutId = 1L,
            mesocycle = 1,
            microcycle = 1,
            microcyclePosition = 1,
            date = date,
            durationInMillis = 3600_000, // 1 hour
            setLogEntries = setResults
        )
    }

    @BeforeEach
    fun setUp() {
        liftsRepository = mockk()
        workoutLogRepository = mockk()
        getLiftWithHistoryStateFlowUseCase = GetLiftWithHistoryStateFlowUseCase(
            liftsRepository = liftsRepository,
            workoutLogRepository = workoutLogRepository
        )
    }

    @Test
    fun `invoke with null liftId returns default state for new lift creation`() = runTest {
        // When
        val resultFlow = getLiftWithHistoryStateFlowUseCase(null)

        // Then
        resultFlow.test {
            val state = awaitItem()

            // Assert Lift properties for a new lift
            assertEquals(0L, state.lift.id)
            assertEquals("", state.lift.name)
            assertEquals(MovementPattern.AB_ISO, state.lift.movementPattern)
            assert(state.lift.restTimerEnabled)

            // Assert history is empty
            assert(state.workoutLogEntries.isEmpty())
            assertNull(state.maxVolume)
            assertNull(state.maxWeight)
            assert(state.topTenPerformances.isEmpty())
            assertEquals(0, state.totalReps)
            assertEquals(0f, state.totalVolume)

            awaitComplete()
        }

        // Verify no repository interactions
        verify(exactly = 0) { liftsRepository.getByIdFlow(any()) }
        verify(exactly = 0) { workoutLogRepository.getWorkoutLogsForLiftFlow(any()) }
    }

    @Test
    fun `invoke with valid liftId and no history returns lift with empty stats`() = runTest {
        // Given
        val liftId = 1L
        every { liftsRepository.getByIdFlow(liftId) } returns flowOf(testLift)
        every { workoutLogRepository.getWorkoutLogsForLiftFlow(liftId) } returns flowOf(emptyList())

        // When
        val resultFlow = getLiftWithHistoryStateFlowUseCase(liftId)

        // Then
        resultFlow.test {
            val state = awaitItem()
            assertEquals(testLift, state.lift)
            assert(state.workoutLogEntries.isEmpty())
            assertNull(state.maxVolume)
            assertNull(state.maxWeight)
            assert(state.topTenPerformances.isEmpty())
            assertEquals(0, state.totalReps)
            assertEquals(0f, state.totalVolume)
            awaitComplete()
        }
    }

    @Test
    fun `invoke with valid liftId returns lift with correctly calculated history`() = runTest {
        // Given
        val liftId = 1L
        every { liftsRepository.getByIdFlow(liftId) } returns flowOf(testLift)
        every { workoutLogRepository.getWorkoutLogsForLiftFlow(liftId) } returns flowOf(
            testWorkoutLogs
        )

        // When
        val resultFlow = getLiftWithHistoryStateFlowUseCase(liftId)

        // Then
        resultFlow.test {
            val state = awaitItem()

            // Assert correct lift and logs are present
            assertEquals(testLift, state.lift)
            assertEquals(testWorkoutLogs, state.workoutLogEntries)

            // Assert Max Volume (10 * 100f = 1000f in workoutLog1)
            assertNotNull(state.maxVolume)
            assertEquals(date1, state.maxVolume.first)
            assertEquals(1000f, state.maxVolume.second)

            // Assert Max Weight (135f in workoutLog3)
            assertNotNull(state.maxWeight)
            assertEquals(date3, state.maxWeight.first)
            assertEquals(135f, state.maxWeight.second)

            // Assert Total Reps (10 + 8 + 5 + 1 + 3 = 27)
            assertEquals(27, state.totalReps)

            // Assert Total Volume (1000 + 880 + 600 + 135 + 390 = 3005)
            assertEquals(3005f, state.totalVolume)

            // Assert Top Performances (sorted by 1RM descending)
            val topPerformances = state.topTenPerformances
            assertEquals(5, topPerformances.size) // We have 5 total sets
            assertEquals(set4.oneRepMax, topPerformances[0].second.oneRepMax) // 1RM ~143.0
            assertEquals(set2.oneRepMax, topPerformances[1].second.oneRepMax) // 1RM ~140.0
            assertEquals(set5.oneRepMax, topPerformances[2].second.oneRepMax) // 1RM ~140.0 (but slightly lower)
            assertEquals(set3.oneRepMax, topPerformances[3].second.oneRepMax) // 1RM  135.0
            assertEquals(set1.oneRepMax, topPerformances[4].second.oneRepMax) // 1RM ~133.3

            awaitComplete()
        }
    }

    @Test
    fun `invoke with non-existent liftId throws IllegalArgumentException`() {
        // Given
        val nonExistentLiftId = 99L
        every { liftsRepository.getByIdFlow(nonExistentLiftId) } returns flowOf(null)
        every { workoutLogRepository.getWorkoutLogsForLiftFlow(nonExistentLiftId) } returns flowOf(
            emptyList()
        )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            runTest {
                getLiftWithHistoryStateFlowUseCase(nonExistentLiftId).collect()
            }
        }
        assertEquals("Lift with id 99 not found", exception.message)
    }

    @Test
    fun `topTenPerformances caps the list at 10 items`() = runTest {
        // Given
        val liftId = 1L
        val manySets = (1..20).map { createTestSetLogEntry(reps = 5, weight = 100f + it) }
        val longWorkoutLog = listOf(createTestWorkoutLogEntry(date = date1, setResults = manySets))

        every { liftsRepository.getByIdFlow(liftId) } returns flowOf(testLift)
        every { workoutLogRepository.getWorkoutLogsForLiftFlow(liftId) } returns flowOf(
            longWorkoutLog
        )

        // When
        val resultFlow = getLiftWithHistoryStateFlowUseCase(liftId)

        // Then
        resultFlow.test {
            val state = awaitItem()
            val topPerformances = state.topTenPerformances

            // Assert size is capped at 10
            assertEquals(10, topPerformances.size)
            // Assert it took the best performing set (highest weight)
            assertEquals(manySets.last(), topPerformances.first().second)
            awaitComplete()
        }
    }
}