package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.extensions.getRecalculatedWorkoutLiftStepSizeOptions
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetWorkoutConfigurationStateFlowUseCaseTest {

    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var useCase: GetWorkoutConfigurationStateFlowUseCase

    @BeforeEach
    fun setUp() {
        workoutsRepository = mockk(relaxed = true)
        programsRepository = mockk(relaxed = true)
        useCase = GetWorkoutConfigurationStateFlowUseCase(workoutsRepository, programsRepository)

        // We will verify/override calls to the top-level extension
        mockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutExtensionsKt")
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `emits initial state, fetches deload on first workout, skips duplicate, refetches on program change, and clears options when workout becomes null`() = runTest {
        // Arrange workouts
        val workoutA = mockk<Workout>(relaxed = true) { every { programId } returns 100L }
        val workoutB = mockk<Workout>(relaxed = true) { every { programId } returns 200L }

        // Programs deload weeks
        coEvery { programsRepository.getDeloadWeek(100L) } returns 2
        coEvery { programsRepository.getDeloadWeek(200L) } returns 3

        // Return a non-empty, type-safe map no matter what the value type is.
        // Map's V is covariant, so Map<Long, Nothing?> fits any Map<Long, T>.
        val stepsA: Map<Long, Map<Int, List<Int>>> = mapOf(1L to emptyMap())
        val stepsB: Map<Long, Map<Int, List<Int>>> = mapOf(2L to emptyMap(), 3L to emptyMap())

        // Extension calls per workout + programDeloadWeek + flag
        every { workoutA.getRecalculatedWorkoutLiftStepSizeOptions(programDeloadWeek = 2, liftLevelDeloadsEnabled = true) } returns stepsA
        every { workoutB.getRecalculatedWorkoutLiftStepSizeOptions(programDeloadWeek = 3, liftLevelDeloadsEnabled = true) } returns stepsB

        // Upstream: A, A (duplicate same instance), B, null
        coEvery { workoutsRepository.getFlow(999L) } returns flow {
            emit(workoutA)
            emit(workoutA) // filtered by distinctUntilChanged
            emit(workoutB) // program changed -> refetch deload
            emit(null)     // options should be empty, deload week persists
        }

        // Act
        val states = useCase(
            workoutId = 999L,
            liftLevelDeloadsEnabled = true
        ).toList(mutableListOf())

        // Assert
        // 1) scan emits initial default state first
        val s0 = states[0]
        assertEquals(null, s0.workout)
        assertEquals(null, s0.programDeloadWeek)
        assertTrue(s0.workoutLiftStepSizeOptions.isEmpty())

        // 2) First workout -> deload fetched (2), options from extension for A
        val s1 = states[1]
        assertEquals(workoutA, s1.workout)
        assertEquals(2, s1.programDeloadWeek)
        assertEquals(stepsA, s1.workoutLiftStepSizeOptions)

        // 3) Program changes (B) -> deload fetched (3)
        val s2 = states[2]
        assertEquals(workoutB, s2.workout)
        assertEquals(3, s2.programDeloadWeek)
        assertEquals(stepsB, s2.workoutLiftStepSizeOptions)

        // 4) Upstream emits null -> options cleared, deload week persists (3)
        val s3 = states[3]
        assertEquals(null, s3.workout)
        assertEquals(3, s3.programDeloadWeek)
        assertTrue(s3.workoutLiftStepSizeOptions.isEmpty())

        // Verify repository interactions:
        coVerify(exactly = 1) { programsRepository.getDeloadWeek(100L) }
        coVerify(exactly = 1) { programsRepository.getDeloadWeek(200L) }

        // Verify extension calls:
        verify(exactly = 1) { workoutA.getRecalculatedWorkoutLiftStepSizeOptions(2, true) }
        verify(exactly = 1) { workoutB.getRecalculatedWorkoutLiftStepSizeOptions(3, true) }
    }

    @Test
    fun `when different instances share the same programId, deload is NOT refetched but options are recalculated`() = runTest {
        val w1 = mockk<Workout>(relaxed = true) { every { programId } returns 300L }
        val w2 = mockk<Workout>(relaxed = true) { every { programId } returns 300L } // different instance, same program

        coEvery { programsRepository.getDeloadWeek(300L) } returns 4

        val steps1: Map<Long, Map<Int, List<Int>>> = mapOf(10L to emptyMap())
        val steps2: Map<Long, Map<Int, List<Int>>> = mapOf(20L to emptyMap())

        every { w1.getRecalculatedWorkoutLiftStepSizeOptions(4, false) } returns steps1
        every { w2.getRecalculatedWorkoutLiftStepSizeOptions(4, false) } returns steps2

        coEvery { workoutsRepository.getFlow(123L) } returns flow {
            emit(w1) // triggers fetch(300 -> 4)
            emit(w2) // same programId -> reuse 4, do NOT refetch
        }

        val states = useCase(workoutId = 123L, liftLevelDeloadsEnabled = false).toList(mutableListOf())

        // initial + w1 + w2
        assertEquals(3, states.size)

        val s1 = states[1]
        assertEquals(4, s1.programDeloadWeek)
        assertEquals(steps1, s1.workoutLiftStepSizeOptions)

        val s2 = states[2]
        assertEquals(4, s2.programDeloadWeek)              // carried over, not re-fetched
        assertEquals(steps2, s2.workoutLiftStepSizeOptions)

        // getDeloadWeek fetched exactly once
        coVerify(exactly = 1) { programsRepository.getDeloadWeek(300L) }

        // Both extension recalcs happened with the correct flag (false)
        verify(exactly = 1) { w1.getRecalculatedWorkoutLiftStepSizeOptions(4, false) }
        verify(exactly = 1) { w2.getRecalculatedWorkoutLiftStepSizeOptions(4, false) }
    }

    @Test
    fun `distinctUntilChanged prevents duplicate processing of the same workout instance`() = runTest {
        val w = mockk<Workout>(relaxed = true) { every { programId } returns 7L }

        coEvery { programsRepository.getDeloadWeek(7L) } returns 1
        val steps: Map<Long, Map<Int, List<Int>>> = mapOf(99L to emptyMap())
        every { w.getRecalculatedWorkoutLiftStepSizeOptions(1, true) } returns steps

        // Same instance emitted twice -> second suppressed by distinctUntilChanged
        coEvery { workoutsRepository.getFlow(77L) } returns flow {
            emit(w)
            emit(w) // suppressed
        }

        val states = useCase(workoutId = 77L, liftLevelDeloadsEnabled = true).toList(mutableListOf())

        // initial + first w
        assertEquals(2, states.size)
        assertEquals(1, states.last().programDeloadWeek)
        assertEquals(steps, states.last().workoutLiftStepSizeOptions)

        coVerify(exactly = 1) { programsRepository.getDeloadWeek(7L) }
        verify(exactly = 1) { w.getRecalculatedWorkoutLiftStepSizeOptions(1, true) }
    }
}
