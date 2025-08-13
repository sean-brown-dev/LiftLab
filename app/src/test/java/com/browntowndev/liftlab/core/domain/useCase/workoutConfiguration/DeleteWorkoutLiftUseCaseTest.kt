
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DeleteWorkoutLiftUseCaseTest {

    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: DeleteWorkoutLiftUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        workoutLiftsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = DeleteWorkoutLiftUseCase(workoutLiftsRepository, transactionScope)
    }

    @Test
    @DisplayName("Deletes the given workout lift inside a transaction")
    fun deletes_workout_lift() = runTest {
        val lift = mockk<GenericWorkoutLift>()
        coJustRun { workoutLiftsRepository.delete(lift) }

        useCase.invoke(lift)

        coVerify { workoutLiftsRepository.delete(lift) }
        coVerify { transactionScope.execute(any<suspend () -> Unit>()) }
    }
}
