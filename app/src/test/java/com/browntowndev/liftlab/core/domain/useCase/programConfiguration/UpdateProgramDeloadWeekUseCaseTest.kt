
package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.delta.ProgramUpdate
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.extensions.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateProgramDeloadWeekUseCaseTest {
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateProgramDeloadWeekUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = UpdateProgramDeloadWeekUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("applyDelta includes program patch AND lift updates from getAllLiftsWithRecalculatedStepSize")
    fun updates_deload_week_and_recalculates_lifts() = runTest {
        // Given a program with two workouts and two standard lifts
        val lift1 = StandardWorkoutLift(
            id = 101L,
            workoutId = 10L,
            liftId = 501L,
            liftName = "Bench",
            position = 0,
            setCount = 3,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            repRangeBottom = 5,
            repRangeTop = 8,
            rpeTarget = 8f,
            stepSize = 2,
            deloadWeek = 4,
            restTime = null,
            restTimerEnabled = false,
            incrementOverride = null,
            liftMovementPattern = MovementPattern.CHEST_ISO,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = 0,
            liftNote = null
        )
        val lift2 = StandardWorkoutLift(
            id = 102L,
            workoutId = 11L,
            liftId = 502L,
            liftName = "Squat",
            position = 1,
            setCount = 3,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            repRangeBottom = 3,
            repRangeTop = 6,
            rpeTarget = 8f,
            stepSize = 3,
            deloadWeek = 4,
            restTime = null,
            restTimerEnabled = false,
            incrementOverride = null,
            liftMovementPattern = MovementPattern.CHEST_ISO,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = 0,
            liftNote = null
        )
        val program = Program(
            id = 42L,
            name = "P",
            isActive = false,
            deloadWeek = 4,
            workouts = listOf(
                Workout(id = 10L, programId = 42L, name = "W1", position = 0, lifts = listOf(lift1)),
                Workout(id = 11L, programId = 42L, name = "W2", position = 1, lifts = listOf(lift2)),
            )
        )

        // We will NOT rely on the real calculation. Stub the extension:
        mockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutExtensionsKt")
        val recalculated1 = lift1.copy(stepSize = 4) // pretend new step size
        val recalculated2 = lift2.copy(stepSize = 2) // pretend new step size
        every {
            // deloadToUseInsteadOfLiftLevel will be the new deload (e.g., 5) or null if using lift-specific
            program.workouts.getAllLiftsWithRecalculatedStepSize(any())
        } returns mapOf(
            lift1.workoutId to listOf(recalculated1),
            lift2.workoutId to listOf(recalculated2)
        )

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(program.id), capture(captured)) }

        // When: set a new deload week and do NOT use lift-specific overrides
        val newDeloadWeek = 5
        useCase.invoke(program, deloadWeek = newDeloadWeek, useLiftSpecificDeload = false)

        // Then: program patch + two lift updates addressed to correct workoutIds
        val delta = captured.captured
        assertEquals(ProgramUpdate(deloadWeek = newDeloadWeek), delta.programUpdate)

        // We should have two workout blocks (for workoutId=10 and 11) with their respective lift changes
        assertEquals(2, delta.workouts.size)
        val byWorkoutId = delta.workouts.associateBy { it.workoutId }
        assertTrue(10L in byWorkoutId.keys)
        assertTrue(11L in byWorkoutId.keys)

        val liftsForW1 = byWorkoutId.getValue(10L).lifts
        assertEquals(1, liftsForW1.size)
        assertEquals(recalculated1.id, liftsForW1[0].workoutLiftId)
        assertEquals(4, liftsForW1[0].liftUpdate?.stepSize)

        val liftsForW2 = byWorkoutId.getValue(11L).lifts
        assertEquals(1, liftsForW2.size)
        assertEquals(recalculated2.id, liftsForW2[0].workoutLiftId)
        assertEquals(2, liftsForW2[0].liftUpdate?.stepSize)

        coVerify { programsRepository.applyDelta(eq(program.id), any()) }
    }
}
