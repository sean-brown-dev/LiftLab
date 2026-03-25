package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
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

@OptIn(ExperimentalCoroutinesApi::class)
class CreateWorkoutLiftsFromLiftsUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: CreateWorkoutLiftsFromLiftsUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)

        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        // also cover Unit-returning call sites if any
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = CreateWorkoutLiftsFromLiftsUseCase(
            programsRepository = programsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `maps multiple lifts with correct positions, field copies, and defaults`() = runTest {
        val workoutId = 100L
        val firstPosition = 5

        val liftA = lift(
            id = 1L,
            name = "Bench Press",
            volumeTypes = 0b0010,
            secondaryVolumeTypes = 0b0001,
            incrementOverride = 2.5f,
            restTimerEnabled = true,
            // restTime left null for simplicity
        )
        val liftB = lift(
            id = 2L,
            name = "Row",
            volumeTypes = 0b0100,
            secondaryVolumeTypes = null, // verify null is preserved
            incrementOverride = null,
            restTimerEnabled = false
        )

        coEvery { programsRepository.getForWorkout(workoutId) } returns Program(id = 1L, name = "Test Program")

        val slot = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(1L,capture(slot)) }

        // When
        useCase(
            workoutId = workoutId,
            firstPosition = firstPosition,
            lifts = listOf(liftA, liftB)
        )

        // Then: repository called once, inside one transaction
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { programsRepository.applyDelta(1L, any()) }

        // Validate
        assertEquals(workoutId, slot.captured.workouts[0].workoutId)

        val wlA = slot.captured.workouts[0].lifts[0].insertLift!! as StandardWorkoutLift
        val wlB = slot.captured.workouts[0].lifts[1].insertLift!! as StandardWorkoutLift

        // --- Lift A ---
        assertEquals(workoutId, wlA.workoutId)
        assertEquals(liftA.id, wlA.liftId)
        assertEquals(liftA.name, wlA.liftName)
        assertEquals(liftA.volumeTypesBitmask, wlA.liftVolumeTypes)
        assertEquals(liftA.secondaryVolumeTypesBitmask, wlA.liftSecondaryVolumeTypes)
        assertEquals(firstPosition, wlA.position)
        assertEquals(3, wlA.setCount) // default
        assertEquals(liftA.incrementOverride, wlA.incrementOverride)
        assertEquals(liftA.restTime, wlA.restTime)
        assertEquals(liftA.restTimerEnabled, wlA.restTimerEnabled)
        assertEquals(8f, wlA.rpeTarget)            // default
        assertEquals(8, wlA.repRangeBottom)        // default
        assertEquals(10, wlA.repRangeTop)          // default
        assertEquals(ProgressionScheme.DOUBLE_PROGRESSION, wlA.progressionScheme)

        // deloadWeek and liftNote should be null by default
        assertEquals(null, wlA.deloadWeek)
        assertEquals(null, wlA.liftNote)

        // --- Lift B ---
        assertEquals(workoutId, wlB.workoutId)
        assertEquals(liftB.id, wlB.liftId)
        assertEquals(liftB.name, wlB.liftName)
        assertEquals(liftB.volumeTypesBitmask, wlB.liftVolumeTypes)
        assertEquals(null, wlB.liftSecondaryVolumeTypes) // null preserved
        assertEquals(firstPosition + 1, wlB.position)
        assertEquals(3, wlB.setCount)
        assertEquals(liftB.incrementOverride, wlB.incrementOverride)
        assertEquals(liftB.restTime, wlB.restTime)
        assertEquals(liftB.restTimerEnabled, wlB.restTimerEnabled)
        assertEquals(8f, wlB.rpeTarget)
        assertEquals(8, wlB.repRangeBottom)
        assertEquals(10, wlB.repRangeTop)
        assertEquals(ProgressionScheme.DOUBLE_PROGRESSION, wlB.progressionScheme)
        assertEquals(null, wlB.deloadWeek)
        assertEquals(null, wlB.liftNote)

        // Order retained
        assertTrue(wlA.position < wlB.position)
    }

    @Test
    fun `empty lifts short circuits and no repository is called`() = runTest {
        val workoutId = 200L
        val firstPosition = 0

        useCase(workoutId, firstPosition, emptyList())

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 0) { programsRepository.getForWorkout(any()) }
        coVerify(exactly = 0) { programsRepository.applyDelta(any(), any()) }
    }

    @Test
    fun `positions start at firstPosition and increment by one per lift`() = runTest {
        val workoutId = 300L

        val lifts = (1L..4L).map { id ->
            lift(
                id = id,
                name = "Lift $id",
                volumeTypes = id.toInt(),
                secondaryVolumeTypes = null,
                incrementOverride = null,
                restTimerEnabled = false
            )
        }

        coEvery { programsRepository.getForWorkout(workoutId) } returns Program(id = 1L, name = "Test Program")

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(1L, capture(captured)) }

        useCase(workoutId = workoutId, firstPosition = 7, lifts = lifts)

        val positions = captured.captured.workouts.single().lifts.fastMap { it.insertLift!!.position }
        assertEquals(listOf(7, 8, 9, 10), positions)
    }

    // ---------- helpers ----------

    private fun lift(
        id: Long,
        name: String,
        volumeTypes: Int,
        secondaryVolumeTypes: Int?,
        incrementOverride: Float?,
        restTimerEnabled: Boolean
    ): Lift = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
        every { this@mockk.volumeTypesBitmask } returns volumeTypes
        every { this@mockk.secondaryVolumeTypesBitmask } returns secondaryVolumeTypes
        every { this@mockk.incrementOverride } returns incrementOverride
        every { this@mockk.restTime } returns null  // keep null in tests; mapping is direct
        every { this@mockk.restTimerEnabled } returns restTimerEnabled
        // We don't assert movementPattern here to avoid enum/package coupling,
        // but mapping is performed by the use case.
    }
}
