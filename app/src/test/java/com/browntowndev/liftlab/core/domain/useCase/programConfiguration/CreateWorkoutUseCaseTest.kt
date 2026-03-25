
package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.delta.WorkoutChange
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateWorkoutUseCaseTest {
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: CreateWorkoutUseCase

    @BeforeEach
    fun setup() {
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = CreateWorkoutUseCase(programsRepository, transactionScope)
    }

    @Test
    fun `creates workout via applyDelta insert under program`() = runTest {
        val program = Program(id = 5L, name = "P", isActive = false)
        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(program.id), capture(captured)) }

        useCase.invoke(program, name = "W")

        val delta = captured.captured
        assertFalse(delta.deleteProgram)
        assertTrue(delta.removedWorkoutIds.isEmpty())
        assertEquals(1, delta.workouts.size)

        val wc: WorkoutChange = delta.workouts.single()
        // insert path => workoutId == 0L and workoutInsert provided
        assertEquals(0L, wc.workoutId)
        assertNotNull(wc.workoutInsert)
        assertEquals("W", wc.workoutInsert?.name)
        assertEquals(program.id, wc.workoutInsert?.programId)
        assertEquals(0, wc.workoutInsert?.position)
        assertTrue(wc.lifts.isEmpty())
    }
}
