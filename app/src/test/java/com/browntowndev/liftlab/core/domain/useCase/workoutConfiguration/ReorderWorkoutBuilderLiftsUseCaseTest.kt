package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class ReorderWorkoutBuilderLiftsUseCaseTest {

    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: ReorderWorkoutBuilderLiftsUseCase

    @BeforeEach
    fun setUp() {
        workoutLiftsRepository = mockk(relaxed = true)
        programsRepository = mockk(relaxed = true)
        workoutInProgressRepository = mockk(relaxed = true)
        liveWorkoutCompletedSetsRepository = mockk(relaxed = true)

        // Your preferred TransactionScope style
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = ReorderWorkoutBuilderLiftsUseCase(
            workoutLiftsRepository = workoutLiftsRepository,
            programsRepository = programsRepository,
            workoutInProgressRepository = workoutInProgressRepository,
            liveWorkoutCompletedSetsRepository = liveWorkoutCompletedSetsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `reorders standard lifts and does not touch in-progress state when workout is NOT in progress`() = runTest {
        val workoutId = 10L
        val a = stdLift(id = 1001L, workoutId = workoutId, liftId = 11L, position = 0)
        val b = stdLift(id = 1002L, workoutId = workoutId, liftId = 22L, position = 1)

        val newPositions = mapOf(
            a.id to 7,
            b.id to 3
        )

        coEvery { workoutInProgressRepository.isWorkoutInProgress(workoutId) } returns false

        val captured = slot<List<StandardWorkoutLift>>()
        coEvery { workoutLiftsRepository.updateMany(capture(captured)) } just Runs

        useCase(
            workoutId = workoutId,
            workoutLifts = listOf(a, b),
            newWorkoutLiftIndices = newPositions
        )

        // Transaction ran
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }

        // Updated copies with new positions
        coVerify(exactly = 1) { workoutLiftsRepository.updateMany(any()) }
        val updated = captured.captured
        assertEquals(listOf(7, 3), updated.map { it.position }, "positions should be updated")
        assertEquals(listOf(a.id, b.id), updated.map { it.id }, "ids should remain the same")

        // No in-progress adjustments
        coVerify(exactly = 1) { workoutInProgressRepository.isWorkoutInProgress(workoutId) }
        coVerify(exactly = 0) { programsRepository.getActive() }
        coVerify(exactly = 0) { liveWorkoutCompletedSetsRepository.getAll() }
        coVerify(exactly = 0) { liveWorkoutCompletedSetsRepository.upsertMany(any()) }
    }

    @Test
    fun `when workout IS in progress and active program exists, completed set positions are remapped and upserted`() = runTest {
        val workoutId = 20L
        // New positions: by workout-lift id (NOT liftId)
        val s1 = stdLift(id = 2001L, workoutId = workoutId, liftId = 101L, position = 1) // becomes 9
        val s2 = stdLift(id = 2002L, workoutId = workoutId, liftId = 202L, position = 2) // becomes 4

        val newPositions = mapOf(
            s1.id to 9,
            s2.id to 4
        )

        coEvery { workoutInProgressRepository.isWorkoutInProgress(workoutId) } returns true
        // Non-null active program gates the in-progress remap path
        coEvery { programsRepository.getActive() } returns mockk(relaxed = true)

        // Completed sets referencing liftIds of updated lifts
        val std = mockk<StandardSetResult>(relaxed = true) {
            every { liftId } returns 101L
        }
        val myo = mockk<MyoRepSetResult>(relaxed = true) {
            every { liftId } returns 202L
        }
        val lin = mockk<LinearProgressionSetResult>(relaxed = true) {
            every { liftId } returns 202L
        }

        // Capture the liftPosition each copy is called with
        val stdPos = slot<Int>()
        val myoPos = slot<Int>()
        val linPos = slot<Int>()

        val stdUpdated = mockk<StandardSetResult>(relaxed = true)
        val myoUpdated = mockk<MyoRepSetResult>(relaxed = true)
        val linUpdated = mockk<LinearProgressionSetResult>(relaxed = true)

        every { std.copy(liftPosition = capture(stdPos)) } returns stdUpdated
        every { myo.copy(liftPosition = capture(myoPos)) } returns myoUpdated
        every { lin.copy(liftPosition = capture(linPos)) } returns linUpdated

        coEvery { liveWorkoutCompletedSetsRepository.getAll() } returns listOf(std, myo, lin)

        val upsertCaptured = slot<List<SetResult>>() // we only care that the three updated objects are passed
        coEvery { liveWorkoutCompletedSetsRepository.upsertMany(capture(upsertCaptured)) } coAnswers {
            emptyList()
        }

        useCase(
            workoutId = workoutId,
            workoutLifts = listOf(s1, s2),
            newWorkoutLiftIndices = newPositions
        )

        // Transaction ran
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }

        // Remap happened using new positions from workout-lift (by liftId join)
        assertEquals(9, stdPos.captured, "std set should be moved to s1.position=9 based on liftId=101")
        assertEquals(4, myoPos.captured, "myo set should be moved to s2.position=4 based on liftId=202")
        assertEquals(4, linPos.captured, "linear set should be moved to s2.position=4 based on liftId=202")

        // All three updated results were upserted
        coVerify(exactly = 1) { liveWorkoutCompletedSetsRepository.upsertMany(any()) }
        assertEquals(setOf(stdUpdated, myoUpdated, linUpdated), upsertCaptured.captured.toSet())
    }

    @Test
    fun `throws when a workout lift id is missing from new indices map`() = runTest {
        val workoutId = 30L
        val a = stdLift(id = 3001L, workoutId = workoutId, liftId = 1L, position = 0)
        val newPositions = emptyMap<Long, Int>() // missing mapping for 3001L

        val ex = assertThrows<NullPointerException> {
            useCase(
                workoutId = workoutId,
                workoutLifts = listOf(a),
                newWorkoutLiftIndices = newPositions
            )
        }
        assertNotNull(ex)

        // updateMany should not be reached
        coVerify(exactly = 0) { workoutLiftsRepository.updateMany(any()) }
    }

    @Test
    fun `throws when workoutLifts contains an unknown GenericWorkoutLift implementation`() = runTest {
        val workoutId = 40L
        // An anonymous implementation will hit the 'else -> throw' branch
        val unknown = object : GenericWorkoutLift {
            override val id = 4001L
            override val workoutId = workoutId
            override val liftId = 909L
            override val liftName = "Unknown"
            override val liftMovementPattern = MovementPattern.HORIZONTAL_PUSH
            override val liftVolumeTypes = 0
            override val liftSecondaryVolumeTypes: Int? = null
            override val liftNote: String? = null
            override val position = 0
            override val setCount = 3
            override val progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION
            override val deloadWeek: Int? = null
            override val incrementOverride: Float? = null
            override val restTime: Duration? = null
            override val restTimerEnabled: Boolean = false
        }

        val ex = assertThrows<Exception> {
            useCase(
                workoutId = workoutId,
                workoutLifts = listOf(unknown),
                newWorkoutLiftIndices = mapOf(unknown.id to 5)
            )
        }
        assertTrue(ex.message?.contains("not defined") == true)

        // updateMany should not be called
        coVerify(exactly = 0) { workoutLiftsRepository.updateMany(any()) }
    }

    @Test
    fun `throws when a completed set type is unknown (during in-progress remap)`() = runTest {
        val workoutId = 50L
        val s1 = stdLift(id = 5001L, workoutId = workoutId, liftId = 5L, position = 0)

        coEvery { workoutInProgressRepository.isWorkoutInProgress(workoutId) } returns true
        coEvery { programsRepository.getActive() } returns mockk(relaxed = true)

        // Unknown SetResult implementation (will hit 'else -> throw' in use case)
        val badSet = object : com.browntowndev.liftlab.core.domain.models.interfaces.SetResult {
            override val id = 9999L
            override val workoutId = workoutId
            override val liftId = 5L
            override val liftPosition = 0
            override val setPosition = 0
            override val weight = 0f
            override val reps = 0
            override val rpe = 0f
            override val oneRepMax = 0
            override val setType = com.browntowndev.liftlab.core.domain.enums.SetType.STANDARD
            override val isDeload = false
            override fun copyBase(
                id: Long,
                workoutId: Long,
                liftId: Long,
                liftPosition: Int,
                setPosition: Int,
                weight: Float,
                reps: Int,
                rpe: Float,
                setType: com.browntowndev.liftlab.core.domain.enums.SetType,
                isDeload: Boolean
            ) = this
        }

        coEvery { liveWorkoutCompletedSetsRepository.getAll() } returns listOf(badSet)

        assertThrows<Exception> {
            runTest {
                useCase(
                    workoutId = workoutId,
                    workoutLifts = listOf(s1),
                    newWorkoutLiftIndices = mapOf(s1.id to 8)
                )
            }
        }

        // No upsert should happen on failure
        coVerify(exactly = 0) { liveWorkoutCompletedSetsRepository.upsertMany(any()) }
    }

    // ---------- helpers ----------

    private fun stdLift(
        id: Long,
        workoutId: Long,
        liftId: Long,
        position: Int
    ) = StandardWorkoutLift(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftName = "Lift-$liftId",
        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
        liftVolumeTypes = 0,
        liftSecondaryVolumeTypes = null,
        position = position,
        setCount = 3,
        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
        incrementOverride = null,
        restTime = null,
        restTimerEnabled = false,
        deloadWeek = null,
        liftNote = null,
        rpeTarget = 8f,
        repRangeBottom = 8,
        repRangeTop = 10,
        stepSize = null
    )
}
