package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class AddSetUseCaseTest {

    private lateinit var customLiftSetsRepository: CustomLiftSetsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: AddSetUseCase

    @BeforeEach
    fun setUp() {
        customLiftSetsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)

        // Preferred TransactionScope mock style
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = AddSetUseCase(
            customLiftSetsRepository = customLiftSetsRepository,
            workoutLiftsRepository = workoutLiftsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `adds a set to matching CustomWorkoutLift, increments setCount, updates lift, and inserts StandardSet with defaults`() = runTest {
        // Given a CustomWorkoutLift with 3 existing custom sets and setCount = 3
        val (base, copied, existingSetsSize) = customLiftWithCopy(
            id = 111L,
            currentSetCount = 3,
            customSetsSize = 3
        )

        val updateCaptured: CapturingSlot<GenericWorkoutLift> = slot()
        coEvery { workoutLiftsRepository.update(capture(updateCaptured)) } just Runs

        val setCaptured: CapturingSlot<StandardSet> = slot()
        coEvery { customLiftSetsRepository.insert(capture(setCaptured)) } returns 555L

        // When
        useCase(
            workoutLifts = listOf(base),
            workoutLiftId = 111L
        )

        // Then (transaction invoked once; repositories called once)
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { workoutLiftsRepository.update(any()) }
        coVerify(exactly = 1) { customLiftSetsRepository.insert(any()) }

        // Updated lift is exactly the 'copied' object (setCount incremented)
        assertSame(copied, updateCaptured.captured)
        assertEquals(111L, (updateCaptured.captured as CustomWorkoutLift).id)
        assertEquals(4, (updateCaptured.captured as CustomWorkoutLift).setCount)

        // Inserted StandardSet uses the copied lift id, proper position, and defaults
        val inserted = setCaptured.captured
        assertNotNull(inserted)
        assertEquals(111L, inserted.workoutLiftId)
        assertEquals(existingSetsSize, inserted.position) // == original customLiftSets.size
        assertEquals(8f, inserted.rpeTarget)
        assertEquals(8, inserted.repRangeBottom)
        assertEquals(10, inserted.repRangeTop)
    }

    @Test
    fun `throws when matching workout lift is not found`() = runTest {
        // Empty list -> no match -> NPE due to '!!'
        assertThrows<NullPointerException> {
            useCase(workoutLifts = emptyList(), workoutLiftId = 1L)
        }
        // No repository interactions
        coVerify(exactly = 0) { workoutLiftsRepository.update(any()) }
        coVerify(exactly = 0) { customLiftSetsRepository.insert(any()) }
    }

    @Test
    fun `throws when a lift with matching id exists but is NOT a CustomWorkoutLift`() = runTest {
        val standard = mockk<StandardWorkoutLift>(relaxed = true) { every { id } returns 999L }

        assertThrows<NullPointerException> {
            useCase(workoutLifts = listOf(standard), workoutLiftId = 999L)
        }
        coVerify(exactly = 0) { workoutLiftsRepository.update(any()) }
        coVerify(exactly = 0) { customLiftSetsRepository.insert(any()) }
    }

    @Test
    fun `when multiple lifts are present, only the targeted CustomWorkoutLift is updated and used for the new set`() = runTest {
        val (base1, copied1, size1) = customLiftWithCopy(id = 1L, currentSetCount = 1, customSetsSize = 1)
        val (base2, copied2, size2) = customLiftWithCopy(id = 2L, currentSetCount = 5, customSetsSize = 5)

        val updateCaptured: CapturingSlot<GenericWorkoutLift> = slot()
        coEvery { workoutLiftsRepository.update(capture(updateCaptured)) } just Runs

        val setCaptured: CapturingSlot<StandardSet> = slot()
        coEvery { customLiftSetsRepository.insert(capture(setCaptured)) } returns 7L

        useCase(
            workoutLifts = listOf(base1, base2),
            workoutLiftId = 2L // target the SECOND one
        )

        coVerify(exactly = 1) { customLiftSetsRepository.insert(any()) }
        coVerify(exactly = 1) { workoutLiftsRepository.update(any()) }

        // Ensure the second (id=2) was used
        assertSame(copied2, updateCaptured.captured)
        assertEquals(2L, setCaptured.captured.workoutLiftId)
        assertEquals(size2, setCaptured.captured.position)

        // Sanity: first custom lift untouched (no extra interactions to detect here,
        // but we can at least assert the ids differ and positions differ)
        assertTrue(size1 != size2 || base1.id != base2.id)
    }

    // ----- helpers -----

    /**
     * Builds a CustomWorkoutLift mock with:
     *  - id
     *  - setCount
     *  - customLiftSets.size == customSetsSize
     * and stubs .copy(setCount = currentSetCount + 1) to return a second mock ("copied")
     * that preserves id and customLiftSets, with setCount incremented.
     */
    private fun customLiftWithCopy(
        id: Long,
        currentSetCount: Int,
        customSetsSize: Int
    ): Triple<CustomWorkoutLift, CustomWorkoutLift, Int> {
        val sets = List(customSetsSize) { mockk<GenericLiftSet>(relaxed = true) }
        val base = mockk<CustomWorkoutLift>(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.setCount } returns currentSetCount
            every { this@mockk.customLiftSets } returns sets
        }
        val copied = mockk<CustomWorkoutLift>(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.setCount } returns currentSetCount + 1
            every { this@mockk.customLiftSets } returns sets
        }
        every { base.copy(setCount = currentSetCount + 1) } returns copied
        return Triple(base, copied, customSetsSize)
    }
}
