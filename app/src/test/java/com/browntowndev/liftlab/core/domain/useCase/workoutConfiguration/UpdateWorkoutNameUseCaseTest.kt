
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateWorkoutNameUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateWorkoutNameUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = UpdateWorkoutNameUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("Patches workout name via ProgramDelta")
    fun updates_name() = runTest {
        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(1L), capture(captured)) }

        useCase.invoke(programId = 1L, workoutId = 42L, newName = "Leg Day++")

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        assertEquals(42L, delta.workouts[0].workoutId)
        assertEquals("Leg Day++", delta.workouts[0].workoutUpdate!!.name)
    }
}
