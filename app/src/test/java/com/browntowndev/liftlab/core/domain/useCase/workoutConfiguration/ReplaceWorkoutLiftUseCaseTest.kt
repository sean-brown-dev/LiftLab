
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ReplaceWorkoutLiftUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: ReplaceWorkoutLiftUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = ReplaceWorkoutLiftUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("Replaces a workout-lift's liftId via delta")
    fun replaces_lift() = runTest {
        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(1L), capture(captured)) }

        useCase.invoke(
            workoutId = 0L,
            workoutLiftId = 5L,
            replacementLiftId = 999L
        )

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        val liftChange = delta.workouts[0].lifts[0]
        assertEquals(5L, liftChange.workoutLiftId)
        assertEquals(999L, liftChange.liftUpdate!!.liftId)

        coVerify { programsRepository.applyDelta(eq(1L), any()) }
    }
}
