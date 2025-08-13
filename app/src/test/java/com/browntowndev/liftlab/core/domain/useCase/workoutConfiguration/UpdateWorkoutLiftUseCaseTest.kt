
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateWorkoutLiftUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateWorkoutLiftUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = UpdateWorkoutLiftUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("Updates a custom lift and includes its sets in the delta")
    fun updates_custom_lift_and_sets() = runTest {
        val custom = mockk<CustomWorkoutLift> {
            every { id } returns 44L
            every { workoutId } returns 9L
            every { liftId } returns 1001L
            every { liftName } returns "Some Lift"
            every { position } returns 3
            every { setCount } returns 5
            every { progressionScheme } returns ProgressionScheme.WAVE_LOADING_PROGRESSION
            every { deloadWeek } returns null
            every { customLiftSets } returns listOf(mockk(), mockk())
        }

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(10L), capture(captured)) }

        useCase.invoke(programId = 10L, workoutLift = custom as GenericWorkoutLift)

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        val wc = delta.workouts[0]
        assertEquals(9L, wc.workoutId)
        val lc = wc.lifts[0]
        assertEquals(44L, lc.workoutLiftId)
        assertEquals(2, lc.sets.size) // both custom sets included
    }
}
