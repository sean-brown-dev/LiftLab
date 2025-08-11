package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.domain.models.workout.FilterableLiftsState
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetFilterableLiftsStateFlowUseCaseTest {

    private lateinit var liftsRepository: LiftsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var useCase: GetFilterableLiftsStateFlowUseCase

    // A controllable source for liftsRepository.getAllFlow()
    private lateinit var liftsFlow: MutableStateFlow<List<Lift>>

    @BeforeEach
    fun setUp() {
        liftsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)

        liftsFlow = MutableStateFlow(emptyList())
        every { liftsRepository.getAllFlow() } returns liftsFlow

        useCase = GetFilterableLiftsStateFlowUseCase(
            liftsRepository = liftsRepository,
            workoutLiftsRepository = workoutLiftsRepository
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `when workoutId is null, liftIdsForWorkout is empty and repo is not queried`() = runTest {
        // Arrange: initial lifts
        val l1 = mockk<Lift>(relaxed = true)
        val l2 = mockk<Lift>(relaxed = true)
        liftsFlow.value = listOf(l1, l2)

        // Act
        val state: FilterableLiftsState = useCase(workoutId = null).first()

        // Assert: passthrough lifts + empty filter set, and no call to getLiftIdsForWorkout
        assertEquals(listOf(l1, l2), state.lifts)
        assertTrue(state.liftIdsForWorkout.isEmpty())
        coVerify(exactly = 0) { workoutLiftsRepository.getLiftIdsForWorkout(any()) }
    }

    @Test
    fun `when workoutId is provided, fetches ids once and reuses them for all emissions`() = runTest {
        // Arrange: upstream starts with one lift, then will change
        val a = mockk<Lift>(relaxed = true)
        val b = mockk<Lift>(relaxed = true)
        liftsFlow.value = listOf(a)

        // The use case should call this once at invocation time
        coEvery { workoutLiftsRepository.getLiftIdsForWorkout(42L) } returns listOf(10L, 20L)

        val emissions = mutableListOf<FilterableLiftsState>()

        // Collect with an unconfined dispatcher to avoid subscription races
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(workoutId = 42L).take(2).toList(emissions)
        }

        // Ensure subscription happened and the initial state emitted
        advanceUntilIdle()

        // Trigger a second emission by changing the lifts stream
        liftsFlow.value = listOf(a, b)
        advanceUntilIdle()
        job.join()

        // Assert: first emission
        val s0 = emissions[0]
        assertEquals(listOf(a), s0.lifts)
        assertEquals(setOf(10L, 20L), s0.liftIdsForWorkout)

        // Assert: second emission reflects updated lifts but SAME ids set
        val s1 = emissions[1]
        assertEquals(listOf(a, b), s1.lifts)
        assertEquals(setOf(10L, 20L), s1.liftIdsForWorkout)

        // IDs fetched exactly once
        coVerify(exactly = 1) { workoutLiftsRepository.getLiftIdsForWorkout(42L) }
    }

    @Test
    fun `handles empty ids list from repository and still updates lifts over time`() = runTest {
        liftsFlow.value = emptyList()
        coEvery { workoutLiftsRepository.getLiftIdsForWorkout(7L) } returns emptyList()

        val emissions = mutableListOf<FilterableLiftsState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(workoutId = 7L).take(2).toList(emissions)
        }
        advanceUntilIdle()

        // push new lifts
        val l = mockk<Lift>(relaxed = true)
        liftsFlow.value = listOf(l)
        advanceUntilIdle()
        job.join()

        assertTrue(emissions[0].liftIdsForWorkout.isEmpty())
        assertTrue(emissions[1].liftIdsForWorkout.isEmpty())
        assertEquals(emptyList<Lift>(), emissions[0].lifts)
        assertEquals(listOf(l), emissions[1].lifts)
    }
}
