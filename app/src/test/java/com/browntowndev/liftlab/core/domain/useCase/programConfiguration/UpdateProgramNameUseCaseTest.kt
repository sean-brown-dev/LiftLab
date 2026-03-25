
package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.delta.ProgramUpdate
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateProgramNameUseCaseTest {
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateProgramNameUseCase

    @BeforeEach
    fun setup() {
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = UpdateProgramNameUseCase(programsRepository, transactionScope)
    }

    @Test
    fun `updates name via applyDelta`() = runTest {
        val programId = 9L
        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(programId), capture(captured)) }

        useCase.invoke(programId, "Renamed")

        val delta = captured.captured
        assertEquals(ProgramUpdate(name = Patch.Set("Renamed")), delta.programUpdate)
        assertFalse(delta.deleteProgram)
        assertTrue(delta.workouts.isEmpty())
        assertTrue(delta.removedWorkoutIds.isEmpty())
    }
}
