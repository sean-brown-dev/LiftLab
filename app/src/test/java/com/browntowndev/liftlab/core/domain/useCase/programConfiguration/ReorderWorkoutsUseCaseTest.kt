
package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta.WorkoutChange.WorkoutUpdate
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReorderWorkoutsUseCaseTest {
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: ReorderWorkoutsUseCase

    @BeforeEach
    fun setup() {
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = ReorderWorkoutsUseCase(programsRepository, transactionScope)
    }

    @Test
    fun `reorders workouts by building updates for each id`() = runTest {
        val w1 = Workout(id = 10L, programId = 2L, name = "W1", position = 0, lifts = emptyList())
        val w2 = Workout(id = 11L, programId = 2L, name = "W2", position = 1, lifts = emptyList())
        val w3 = Workout(id = 12L, programId = 2L, name = "W3", position = 2, lifts = emptyList())

        val newOrders = mapOf(10L to 2, 11L to 0, 12L to 1)

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(2L), capture(captured)) }

        useCase.invoke(listOf(w1, w2, w3), newOrders)

        val delta = captured.captured
        assertEquals(3, delta.workouts.size)
        delta.workouts.forEach { wc ->
            assertTrue(wc.workoutId in newOrders.keys)
            val expectedPos = newOrders.getValue(wc.workoutId)
            assertEquals(WorkoutUpdate(position = Patch.Set(expectedPos)), wc.workoutUpdate)
        }
    }
}
