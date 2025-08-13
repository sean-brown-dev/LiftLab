
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
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteProgramUseCaseTest {
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: DeleteProgramUseCase

    @BeforeEach
    fun setup() {
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = DeleteProgramUseCase(programsRepository, transactionScope)
    }

    @Test
    fun `deletes program via applyDelta and promotes newest when no active remains`() = runTest {
        val programId = 77L
        val newest = Program(id = 88L, name = "Newest", isActive = false)

        // After delete there is no active program
        coEvery { programsRepository.getActive() } returns null
        coEvery { programsRepository.getNewest() } returns newest

        val captured = mutableListOf<Pair<Long, ProgramDelta>>()
        coEvery { programsRepository.applyDelta(capture(slot<Long>()), capture(slot<ProgramDelta>())) } answers {
            captured += (firstArg<Long>() to secondArg<ProgramDelta>())
        }

        useCase.invoke(programId)

        // First delta: deleteProgram
        val deleteCall = captured.first { it.first == programId }.second
        assertTrue(deleteCall.deleteProgram)
        assertNull(deleteCall.programUpdate)

        // Second delta: set newest active
        val activateCall = captured.first { it.first == newest.id }.second
        assertEquals(ProgramUpdate(isActive = true), activateCall.programUpdate)
    }

    @Test
    fun `deletes program via applyDelta and does not promote when an active exists`() = runTest {
        val programId = 77L
        coEvery { programsRepository.getActive() } returns Program(id = 1L, name = "Active", isActive = true)

        coJustRun { programsRepository.applyDelta(any(), any()) }

        useCase.invoke(programId)

        // Should only call applyDelta once (delete)
        coVerify(exactly = 1) { programsRepository.applyDelta(eq(programId), match { it.deleteProgram }) }
        coVerify(exactly = 0) { programsRepository.getNewest() }
    }
}
