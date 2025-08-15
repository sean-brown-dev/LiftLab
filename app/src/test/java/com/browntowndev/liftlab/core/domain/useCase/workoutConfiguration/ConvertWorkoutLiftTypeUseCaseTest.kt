
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConvertWorkoutLiftTypeUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: ConvertWorkoutLiftTypeUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = ConvertWorkoutLiftTypeUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("enableCustomSets=true from Standard→Custom: applies a delta (details verified in integration tests)")
    fun enable_custom_sets_from_standard_applies_delta() = runTest {
        val std = StandardWorkoutLift(
            id = 11L,
            workoutId = 22L,
            setCount = 3,
            repRangeBottom = 4,
            repRangeTop = 5,
            rpeTarget = 6f,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            stepSize = 2,
            liftId = 33L,
            liftName = "Lift Name",
            liftNote = "Lift Note",
            liftMovementPattern = MovementPattern.HORIZONTAL_PULL,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = 0,
            deloadWeek = null,
            restTime = null,
            restTimerEnabled = true,
            incrementOverride = null
        )

        coJustRun { programsRepository.applyDelta(any(), any()) }

        useCase(programId = 1L, workoutLiftToConvert = std as GenericWorkoutLift, enableCustomSets = true)

        coVerify(exactly = 1) { programsRepository.applyDelta(eq(1L), any()) }
    }

    @Test
    @DisplayName("enableCustomSets=true throws if already Custom")
    fun enable_custom_sets_throws_if_already_custom() = runTest {
        val custom = mockk<CustomWorkoutLift>()
        val ex = assertThrows<Exception> {
            useCase(programId = 1L, workoutLiftToConvert = custom as GenericWorkoutLift, enableCustomSets = true)
        }
        assertTrue(ex.message!!.contains("already has custom"))
    }

    @Test
    @DisplayName("enableCustomSets=false throws if not Custom")
    fun disable_custom_sets_throws_if_not_custom() = runTest {
        val std = mockk<StandardWorkoutLift>()
        val ex = assertThrows<Exception> {
            useCase(
                programId = 1L,
                workoutLiftToConvert = std as GenericWorkoutLift,
                enableCustomSets = false
            )
        }
        assertTrue(ex.message!!.contains("does not have custom"))
    }

    @Test
    @DisplayName("enableCustomSets=false from Custom→Standard: updates lift fields from top custom set and purges all sets")
    fun disable_custom_sets_converts_and_purges() = runTest {
        val topBottom = 10 to 12
        val topRpe = 9f

        // Custom with two sets; the one with the higher position should be used
        val custom = mockk<CustomWorkoutLift> {
            every { id } returns 55L
            every { workoutId } returns 77L
            every { customLiftSets } returns listOf(
                mockk { every { position } returns 0; every { repRangeBottom } returns 6; every { repRangeTop } returns 8; every { rpeTarget } returns 7f },
                mockk { every { position } returns 3; every { repRangeBottom } returns topBottom.first; every { repRangeTop } returns topBottom.second; every { rpeTarget } returns topRpe },
            )
        }
        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(1L), capture(captured)) }

        useCase(programId = 1L, workoutLiftToConvert = custom as GenericWorkoutLift, enableCustomSets = false)

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        val wc = delta.workouts[0]
        assertEquals(77L, wc.workoutId)
        assertEquals(1, wc.lifts.size)

        val lc = wc.lifts[0]
        val u = lc.liftUpdate!!
        // Patch-aware checks
        assertTrue(u.repRangeTop is Patch.Set && u.repRangeTop.value == topBottom.second)
        assertTrue(u.repRangeBottom is Patch.Set && u.repRangeBottom.value == topBottom.first)
        assertTrue(u.rpeTarget is Patch.Set && u.rpeTarget.value == topRpe)

        assertTrue(lc.removeAllSets)
        assertTrue(lc.sets.isEmpty())
        assertTrue(lc.removedSetIds.isEmpty())
    }

    @Test
    @DisplayName("enableCustomSets=true from Standard→Custom: clears scalar fields with Patch.Set(null) and adds custom sets")
    fun enable_custom_sets_from_standard_clears_and_adds_custom_sets() = runTest {
        val std = StandardWorkoutLift(
            id = 11L,
            workoutId = 22L,
            setCount = 3,
            repRangeBottom = 4,
            repRangeTop = 5,
            rpeTarget = 6f,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            stepSize = 2,
            liftId = 33L,
            liftName = "Lift Name",
            liftNote = "Lift Note",
            liftMovementPattern = MovementPattern.HORIZONTAL_PULL,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = 0,
            deloadWeek = null,
            restTime = null,
            restTimerEnabled = true,
            incrementOverride = null
        )

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(1L), capture(captured)) }

        useCase(programId = 1L, workoutLiftToConvert = std as GenericWorkoutLift, enableCustomSets = true)

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        val wc = delta.workouts[0]
        assertEquals(22L, wc.workoutId)
        assertEquals(1, wc.lifts.size)

        val lc = wc.lifts[0]
        val u = lc.liftUpdate!!
        // These must be explicit clears
        assertTrue(u.repRangeBottom is Patch.Set && u.repRangeBottom.value == null)
        assertTrue(u.repRangeTop is Patch.Set && u.repRangeTop.value == null)
        assertTrue(u.rpeTarget is Patch.Set && u.rpeTarget.value == null)
        assertTrue(u.stepSize is Patch.Set && u.stepSize.value == null)

        // Should be generating at least one custom set via generateCustomSets()
        assertTrue(lc.sets.isNotEmpty(), "Expected custom sets to be added")
        // And not removing anything in this branch
        assertTrue(!lc.removeAllSets)
        assertTrue(lc.removedSetIds.isEmpty())
    }
}
