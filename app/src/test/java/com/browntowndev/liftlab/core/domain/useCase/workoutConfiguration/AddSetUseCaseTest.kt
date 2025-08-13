
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AddSetUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: AddSetUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = AddSetUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("Adds one StandardSet at the next position with 8–10 reps @ RPE 8 and applies delta")
    fun adds_set_at_next_position() = runTest {
        val workoutLift = mockk<CustomWorkoutLift> {
            every { id } returns 200L
            every { workoutId } returns 20L
            every { customLiftSets } returns listOf(mockk(), mockk()) // size = 2
        }

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(1L), capture(captured)) }

        useCase.invoke(programId = 1L, workoutLift = workoutLift)

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        val wc = delta.workouts[0]
        assertEquals(20L, wc.workoutId)

        assertEquals(1, wc.lifts.size)
        val lc = wc.lifts[0]
        assertFalse(lc.removeAllSets)
        assertTrue(lc.removedSetIds.isEmpty())
        assertEquals(1, lc.sets.size)

        val std = lc.sets[0].set as StandardSet
        assertEquals(200L, std.workoutLiftId)
        assertEquals(2, std.position) // appended after existing two sets
        assertEquals(8, std.repRangeBottom)
        assertEquals(10, std.repRangeTop)
        assertEquals(8f, std.rpeTarget)

        coVerify(exactly = 1) { programsRepository.applyDelta(eq(1L), any()) }
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
    }
}
