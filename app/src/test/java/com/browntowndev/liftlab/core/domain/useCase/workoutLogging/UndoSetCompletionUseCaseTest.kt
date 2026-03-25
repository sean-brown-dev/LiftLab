package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class UndoSetCompletionUseCaseTest {

    private lateinit var restTimerRepo: RestTimerInProgressRepository
    private lateinit var workoutRepo: WorkoutInProgressRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UndoSetCompletionUseCase

    @BeforeEach
    fun setUp() {
        restTimerRepo = mockk(relaxed = true)
        workoutRepo = mockk(relaxed = true)

        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = UndoSetCompletionUseCase(
            restTimerInProgressRepository = restTimerRepo,
            workoutInProgressRepository = workoutRepo,
            transactionScope = transactionScope,
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `deletes matching normal set when workout NOT in progress (no rest timer clear)`() = runTest {
        // Given a single normal set result
        val target = normalSet(
            id = 10L,
            workoutId = 5L,
            liftPosition = 1,
            setPosition = 2
        )
        coEvery { workoutRepo.isWorkoutInProgress(5L) } returns false

        val deletedIds = mutableListOf<Long>()

        // When
        useCase(
            liftPosition = 1,
            setPosition = 2,
            myoRepSetPosition = null,
            setResults = listOf(target),
            onDeleteSetResult = { deletedIds += it }
        )

        // Then
        coVerify(exactly = 1) { workoutRepo.isWorkoutInProgress(5L) }
        coVerify(exactly = 0) { restTimerRepo.delete() }
        assertEquals(listOf(10L), deletedIds)
    }

    @Test
    fun `deletes matching normal set when workout IS in progress (clears rest timers)`() = runTest {
        val target = normalSet(
            id = 11L,
            workoutId = 6L,
            liftPosition = 0,
            setPosition = 0
        )
        coEvery { workoutRepo.isWorkoutInProgress(6L) } returns true

        val deletedIds = mutableListOf<Long>()

        useCase(
            liftPosition = 0,
            setPosition = 0,
            myoRepSetPosition = null,
            setResults = listOf(target),
            onDeleteSetResult = { deletedIds += it }
        )

        coVerify(exactly = 1) { workoutRepo.isWorkoutInProgress(6L) }
        coVerify(exactly = 1) { restTimerRepo.delete() }
        assertEquals(listOf(11L), deletedIds)
    }

    @Test
    fun `when myoRepSetPosition is null, chooses the non-myo set if both share same positions`() = runTest {
        val myo = myoSet(
            id = 21L,
            workoutId = 9L,
            liftPosition = 3,
            setPosition = 1,
            myoRepSetPosition = 1
        )
        val normal = normalSet(
            id = 22L,
            workoutId = 9L,
            liftPosition = 3,
            setPosition = 1
        )
        // Put myo first to ensure the filter skips it (myoRepSetPosition != null) and then matches the normal set
        coEvery { workoutRepo.isWorkoutInProgress(9L) } returns false

        val deletedIds = mutableListOf<Long>()
        useCase(
            liftPosition = 3,
            setPosition = 1,
            myoRepSetPosition = null,
            setResults = listOf(myo, normal),
            onDeleteSetResult = { deletedIds += it }
        )

        coVerify(exactly = 1) { workoutRepo.isWorkoutInProgress(9L) }
        coVerify(exactly = 0) { restTimerRepo.delete() }
        assertEquals(listOf(22L), deletedIds) // normal set deleted, not the myo set
    }

    @Test
    fun `when myoRepSetPosition is provided, chooses the matching myo-rep set`() = runTest {
        val myo = myoSet(
            id = 31L,
            workoutId = 7L,
            liftPosition = 2,
            setPosition = 2,
            myoRepSetPosition = 3
        )
        val normal = normalSet(
            id = 32L,
            workoutId = 7L,
            liftPosition = 2,
            setPosition = 2
        )
        coEvery { workoutRepo.isWorkoutInProgress(7L) } returns false

        val deletedIds = mutableListOf<Long>()
        useCase(
            liftPosition = 2,
            setPosition = 2,
            myoRepSetPosition = 3,
            setResults = listOf(normal, myo), // list order shouldn't matter here
            onDeleteSetResult = { deletedIds += it }
        )

        coVerify(exactly = 1) { workoutRepo.isWorkoutInProgress(7L) }
        coVerify(exactly = 0) { restTimerRepo.delete() }
        assertEquals(listOf(31L), deletedIds) // myo set deleted
    }

    @Test
    fun `no-op when no set matches the provided positions (no repo calls)`() = runTest {
        val other = normalSet(
            id = 41L,
            workoutId = 12L,
            liftPosition = 0,
            setPosition = 0
        )

        val deletedIds = mutableListOf<Long>()
        useCase(
            liftPosition = 9,
            setPosition = 9,
            myoRepSetPosition = null,
            setResults = listOf(other),
            onDeleteSetResult = { deletedIds += it }
        )

        // No match -> should not check workout state or delete timers, and should not delete any set
        coVerify(exactly = 0) { workoutRepo.isWorkoutInProgress(any()) }
        coVerify(exactly = 0) { restTimerRepo.delete() }
        assertEquals(emptyList(), deletedIds)
    }

    // ----------------- helpers -----------------

    private fun normalSet(
        id: Long,
        workoutId: Long,
        liftPosition: Int,
        setPosition: Int
    ): SetResult = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.workoutId } returns workoutId
        every { this@mockk.liftPosition } returns liftPosition
        every { this@mockk.setPosition } returns setPosition
    }

    private fun myoSet(
        id: Long,
        workoutId: Long,
        liftPosition: Int,
        setPosition: Int,
        myoRepSetPosition: Int
    ): MyoRepSetResult = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.workoutId } returns workoutId
        every { this@mockk.liftPosition } returns liftPosition
        every { this@mockk.setPosition } returns setPosition
        every { this@mockk.myoRepSetPosition } returns myoRepSetPosition
    }
}
