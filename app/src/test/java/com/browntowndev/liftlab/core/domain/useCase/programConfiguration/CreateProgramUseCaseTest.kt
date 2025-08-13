
package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.delta.ProgramUpdate
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CreateProgramUseCaseTest {
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: CreateProgramUseCase

    @BeforeEach
    fun setup() {
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = CreateProgramUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("CreateProgramUseCase: inserts new program; if isActive and currentActiveProgram provided -> applyDelta(isActive=false)")
    fun createsProgram_and_deactivates_current_when_needed() = runTest {
        val currentActive = Program(id = 10L, name = "Old", isActive = true)
        coEvery { programsRepository.insert(any()) } returns 123L

        val captured = mutableListOf<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(currentActive.id), capture(captured)) }

        useCase.invoke(name = "New Program", isActive = true, currentActiveProgram = currentActive)

        coVerify { programsRepository.insert(match { it.name == "New Program" && it.isActive }) }
        assertEquals(1, captured.size)
        val delta = captured.single()
        assertFalse(delta.deleteProgram)
        assertEquals(ProgramUpdate(isActive = false), delta.programUpdate)
        assertTrue(delta.workouts.isEmpty())
        assertTrue(delta.removedWorkoutIds.isEmpty())
    }

    @Test
    @DisplayName("CreateProgramUseCase: inserts new program; if not active -> no applyDelta for current program")
    fun createsProgram_without_deactivating_when_not_active() = runTest {
        coEvery { programsRepository.insert(any()) } returns 456L

        useCase.invoke(name = "New Program", isActive = false, currentActiveProgram = null)

        coVerify(exactly = 1) { programsRepository.insert(match { it.name == "New Program" && !it.isActive }) }
        coVerify(exactly = 0) { programsRepository.applyDelta(any(), any()) }
    }
}
