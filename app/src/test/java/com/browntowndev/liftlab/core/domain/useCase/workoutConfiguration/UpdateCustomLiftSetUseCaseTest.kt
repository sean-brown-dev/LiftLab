
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateCustomLiftSetUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateCustomLiftSetUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = UpdateCustomLiftSetUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("Upserts a specific set under its workout-lift")
    fun updates_custom_set() = runTest {
        val set = mockk<GenericLiftSet> {
            every { workoutLiftId } returns 88L
        }
        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(7L), capture(captured)) }

        useCase.invoke(programId = 7L, workoutId = 4L, set = set)

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        val wc = delta.workouts[0]
        assertEquals(4L, wc.workoutId)
        val lc = wc.lifts[0]
        assertEquals(88L, lc.workoutLiftId)
        assertEquals(1, lc.sets.size)
        assertSame(set, lc.sets[0].set)
    }
}
