
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DeleteWorkoutLiftUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: DeleteWorkoutLiftUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = DeleteWorkoutLiftUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("Deletes the given workout lift inside a transaction")
    fun deletes_workout_lift() = runTest {
        val lift = mockk<GenericWorkoutLift>() {
            coEvery { id } returns 1L
            coEvery { workoutId } returns 1L
        }
        val slot = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(1L, capture(slot)) }

        useCase.invoke(1L, lift)

        coVerify { programsRepository.applyDelta(1L, any()) }
        coVerify { transactionScope.execute(any<suspend () -> Unit>()) }

        assertEquals(1, slot.captured.workouts.size)
        val deletedLiftIds = slot.captured.workouts.single().removedWorkoutLiftIds
        assertEquals(1, deletedLiftIds.size)
        assertEquals(lift.id, deletedLiftIds.single())
    }
}
