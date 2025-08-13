
package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.delta.ProgramUpdate
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SetProgramAsActiveUseCaseTest {
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: SetProgramAsActiveUseCase

    @BeforeEach
    fun setup() {
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = SetProgramAsActiveUseCase(programsRepository, transactionScope)
    }

    @Test
    fun `activates the selected program and deactivates the previously active one`() = runTest {
        val target = Program(id = 100L, name = "B", isActive = false)
        val previouslyActive = Program(id = 50L, name = "A", isActive = true)
        val all = listOf(previouslyActive, target)

        val captured = mutableListOf<Pair<Long, ProgramDelta>>()
        coJustRun { programsRepository.applyDelta(any(), any()) }
        coEvery { programsRepository.applyDelta(capture(slot<Long>()), capture(slot<ProgramDelta>())) } answers {
            captured += (firstArg<Long>() to secondArg<ProgramDelta>())
        }

        useCase.invoke(target.id, all)

        // First: activate target
        val activate = captured.first { it.first == target.id }.second
        assertEquals(ProgramUpdate(isActive = true), activate.programUpdate)

        // Then: deactivate previously active
        val deactivate = captured.first { it.first == previouslyActive.id }.second
        assertEquals(ProgramUpdate(isActive = false), deactivate.programUpdate)
    }
}
