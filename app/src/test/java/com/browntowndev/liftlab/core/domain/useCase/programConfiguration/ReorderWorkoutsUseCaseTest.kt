package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class ReorderWorkoutsUseCaseTest {

    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: ReorderWorkoutsUseCase

    @BeforeEach
    fun setUp() {
        workoutsRepository = mockk(relaxed = true)

        // Preferred TransactionScope mock
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = ReorderWorkoutsUseCase(
            workoutsRepository = workoutsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `reorders multiple workouts, preserves input order, and calls updateMany with copies`() = runTest {
        val w1 = mockk<Workout>(relaxed = true) { every { id } returns 1L }
        val w2 = mockk<Workout>(relaxed = true) { every { id } returns 2L }

        // Copies returned by .copy(position = ...)
        val w1Copy = mockk<Workout>(relaxed = true)
        val w2Copy = mockk<Workout>(relaxed = true)
        every { w1.copy(position = 7) } returns w1Copy
        every { w2.copy(position = 3) } returns w2Copy

        val captured: CapturingSlot<List<Workout>> = slot()
        coEvery { workoutsRepository.updateMany(capture(captured)) } just Runs
        advanceUntilIdle()

        useCase(
            workouts = listOf(w1, w2),
            newOrders = mapOf(1L to 7, 2L to 3)
        )

        // Transaction executed once; repository received the two copies in input order
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { workoutsRepository.updateMany(any()) }
        assertEquals(listOf(w1Copy, w2Copy), captured.captured)

        // Ensure the correct new positions were used
        io.mockk.verify(exactly = 1) { w1.copy(position = 7) }
        io.mockk.verify(exactly = 1) { w2.copy(position = 3) }
    }

    @Test
    fun `extra ids in newOrders are ignored, only matching workouts are updated`() = runTest {
        val w1 = mockk<Workout>(relaxed = true) { every { id } returns 10L }
        val w2 = mockk<Workout>(relaxed = true) { every { id } returns 20L }

        val w1Copy = mockk<Workout>(relaxed = true)
        val w2Copy = mockk<Workout>(relaxed = true)
        every { w1.copy(position = 1) } returns w1Copy
        every { w2.copy(position = 2) } returns w2Copy

        val cap: CapturingSlot<List<Workout>> = slot()
        coEvery { workoutsRepository.updateMany(capture(cap)) } just Runs

        useCase(
            workouts = listOf(w1, w2),
            newOrders = mapOf(10L to 1, 20L to 2, 999L to 0) // 999L not present in workouts
        )

        coVerify(exactly = 1) { workoutsRepository.updateMany(any()) }
        assertEquals(listOf(w1Copy, w2Copy), cap.captured)
    }

    @Test
    fun `throws when a workout id is missing from newOrders and does not call updateMany`() = runTest {
        val w1 = mockk<Workout>(relaxed = true) { every { id } returns 101L }
        val w2 = mockk<Workout>(relaxed = true) { every { id } returns 202L }

        // Only stub copy for the first; second will fail the mapping
        val w1Copy = mockk<Workout>(relaxed = true)
        every { w1.copy(position = 5) } returns w1Copy

        val ex = assertThrows<IllegalArgumentException> {
            useCase(
                workouts = listOf(w1, w2),
                newOrders = mapOf(101L to 5) // missing 202L
            )
        }
        assertTrue(ex.message?.contains("202") == true, "Exception should mention missing workout id")

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 0) { workoutsRepository.updateMany(any()) }
    }

    @Test
    fun `empty workouts list results in updateMany with empty list`() = runTest {
        val cap: CapturingSlot<List<Workout>> = slot()
        coEvery { workoutsRepository.updateMany(capture(cap)) } just Runs
        advanceUntilIdle()

        useCase(workouts = emptyList(), newOrders = emptyMap())

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { workoutsRepository.updateMany(any()) }
        assertTrue(cap.captured.isEmpty())
    }
}
