@file:OptIn(ExperimentalStdlibApi::class)

package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateManyCustomLiftSetsUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateManyCustomLiftSetsUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)

        // Execute the provided block immediately
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = UpdateManyCustomLiftSetsUseCase(
            programsRepository = programsRepository,
            transactionScope = transactionScope
        )
    }

    @Test
    fun `applies delta once for multiple sets across multiple workout lifts and groups sets under correct lifts`() = runBlocking {
        // Arrange
        val programId = 42L
        val workoutId = 777L

        val s1 = StandardSet(
            id = 1L,
            workoutLiftId = 10L,
            position = 0,
            rpeTarget = 8.0f,
            repRangeBottom = 6,
            repRangeTop = 8
        )
        val s2 = MyoRepSet(
            id = 2L,
            workoutLiftId = 10L,
            position = 1,
            rpeTarget = 9.0f,
            repRangeBottom = 10,
            repRangeTop = 12,
            repFloor = 5,
            maxSets = 4,
            setMatching = false,
            setGoal = 3
        )
        val s3 = DropSet(
            id = 3L,
            workoutLiftId = 11L,
            position = 0,
            rpeTarget = 9.0f,
            repRangeBottom = 12,
            repRangeTop = 15,
            dropPercentage = 0.2f
        )

        val sets: List<GenericLiftSet> = listOf(s1, s2, s3)

        val deltaSlot = slot<ProgramDelta>()
        coEvery { programsRepository.applyDelta(programId, capture(deltaSlot)) } just Runs

        // Act
        useCase(programId = programId, workoutId = workoutId, sets = sets)

        // Assert interaction
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { programsRepository.applyDelta(programId, any()) }

        // Assert delta structure and contents
        val delta = deltaSlot.captured
        assertNotNull(delta)

        // One workout targeted, with correct workoutId
        assertEquals(1, delta.workouts.size, "Expected exactly one WorkoutChange in ProgramDelta")
        val workoutChange = delta.workouts.first()
        assertEquals(workoutId, workoutChange.workoutId, "WorkoutChange.workoutId mismatch")  // :contentReference[oaicite:0]{index=0}

        // Build expected grouping: workoutLiftId -> setIds
        val expectedByLift = sets.groupBy { it.workoutLiftId }.mapValues { e -> e.value.map { it.id }.toSet() }

        // Delta must have a LiftChange for each workoutLiftId, with matching set ids (order irrelevant)
        val actualByLift: Map<Long, Set<Long>> = workoutChange.lifts.associate { liftChange ->
            val setIds = liftChange.sets.map { it.set.id }.toSet()
            liftChange.workoutLiftId to setIds
        }

        assertEquals(expectedByLift.keys, actualByLift.keys, "LiftChange keys (workoutLiftId) mismatch") // :contentReference[oaicite:1]{index=1}
        expectedByLift.forEach { (liftId, expectedIds) ->
            val actualIds = actualByLift.getValue(liftId)
            assertEquals(expectedIds, actualIds, "Set ids for workoutLiftId=$liftId mismatch")
        }
    }

    @Test
    fun `handles empty input list by still producing a workout-scoped delta with no lift changes`() = runBlocking {
        val programId = 1L
        val workoutId = 2L

        val deltaSlot = slot<ProgramDelta>()
        coEvery { programsRepository.applyDelta(programId, capture(deltaSlot)) } just Runs

        useCase(programId = programId, workoutId = workoutId, sets = emptyList())

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { programsRepository.applyDelta(programId, any()) }

        val delta = deltaSlot.captured
        assertNotNull(delta)
        // Expect a WorkoutChange scoped to workoutId with no lifts (builder builds an empty change for the workout)  // :contentReference[oaicite:2]{index=2} :contentReference[oaicite:3]{index=3}
        assertEquals(1, delta.workouts.size, "Expected a workout-scoped delta even for empty input")
        val workoutChange = delta.workouts.first()
        assertEquals(workoutId, workoutChange.workoutId, "WorkoutChange.workoutId mismatch")
        assertTrue(workoutChange.lifts.isEmpty(), "Expected no LiftChange entries for empty input")
    }

    @Test
    fun `transactionScope is required to execute the work`() = runBlocking {
        val programId = 9L
        val workoutId = 9L

        // No-op scope that never invokes the block
        val tsNoInvoke: TransactionScope = mockk(relaxed = true) {
            coEvery { execute(any<suspend () -> Unit>()) } returns Unit
        }
        val uc = UpdateManyCustomLiftSetsUseCase(programsRepository, tsNoInvoke)

        val sets = listOf<GenericLiftSet>(
            StandardSet(
                id = 9L,
                workoutLiftId = 99L,
                position = 0,
                rpeTarget = 7.5f,
                repRangeBottom = 8,
                repRangeTop = 10
            )
        )

        uc(programId, workoutId, sets)

        // Because execute didn't invoke the lambda, applyDelta should never be called.
        coVerify(exactly = 0) { programsRepository.applyDelta(any(), any()) }
    }

    @Test
    fun `large mixed list results in a single delta with all sets grouped under their lifts`() = runBlocking {
        val programId = 100L
        val workoutId = 200L

        val sets = buildList<GenericLiftSet> {
            repeat(20) { i ->
                add(
                    StandardSet(
                        id = i + 1L,
                        workoutLiftId = 1000L,
                        position = i,
                        rpeTarget = 8.0f,
                        repRangeBottom = 6,
                        repRangeTop = 8
                    )
                )
            }
            repeat(15) { i ->
                add(
                    MyoRepSet(
                        id = 100 + i + 1L,
                        workoutLiftId = 1001L,
                        position = i,
                        rpeTarget = 9.0f,
                        repRangeBottom = 10,
                        repRangeTop = 12,
                        repFloor = 5,
                        maxSets = 4,
                        setMatching = (i % 2 == 0),
                        setGoal = 3
                    )
                )
            }
            repeat(15) { i ->
                add(
                    DropSet(
                        id = 200 + i + 1L,
                        workoutLiftId = 1002L,
                        position = i,
                        rpeTarget = 9.5f,
                        repRangeBottom = 12,
                        repRangeTop = 15,
                        dropPercentage = 0.15f
                    )
                )
            }
        }

        val deltaSlot = slot<ProgramDelta>()
        coEvery { programsRepository.applyDelta(programId, capture(deltaSlot)) } just Runs

        useCase(programId = programId, workoutId = workoutId, sets = sets)

        // Single atomic write
        coVerify(exactly = 1) { programsRepository.applyDelta(programId, any()) }

        // Validate grouping
        val delta = deltaSlot.captured
        val workoutChange = delta.workouts.single()
        assertEquals(workoutId, workoutChange.workoutId)

        val expectedByLift = sets.groupBy { it.workoutLiftId }.mapValues { e -> e.value.map { it.id }.toSet() }
        val actualByLift: Map<Long, Set<Long>> = workoutChange.lifts.associate { liftChange ->
            liftChange.workoutLiftId to liftChange.sets.map { it.set.id }.toSet()
        }

        assertEquals(expectedByLift.keys, actualByLift.keys, "Lift ids mismatch in large list delta") // :contentReference[oaicite:4]{index=4}
        expectedByLift.forEach { (liftId, expectedIds) ->
            val actualIds = actualByLift.getValue(liftId)
            assertEquals(expectedIds, actualIds, "Set ids mismatch for workoutLiftId=$liftId in large list delta")
        }
    }
}
