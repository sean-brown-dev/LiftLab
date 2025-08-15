
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
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

class UpdateWorkoutLiftDeloadWeekUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateWorkoutLiftDeloadWeekUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = UpdateWorkoutLiftDeloadWeekUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("For non-standard lifts, sets deloadWeek and leaves stepSize null")
    fun updates_deload_for_custom_without_step() = runTest {
        val lift = mockk<CustomWorkoutLift> {
            every { id } returns 33L
            every { workoutId } returns 3L
        }

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(1L), capture(captured)) }

        useCase.invoke(programId = 1L, workoutLift = lift as GenericWorkoutLift, deloadWeek = 2, programDeloadWeek = 4)

        val lc = captured.captured.workouts[0].lifts[0]
        assertEquals(33L, lc.workoutLiftId)
        assertEquals(Patch.Set(2), lc.liftUpdate!!.deloadWeek)
        assertEquals(Patch.Set(null), lc.liftUpdate.stepSize)
    }
}
