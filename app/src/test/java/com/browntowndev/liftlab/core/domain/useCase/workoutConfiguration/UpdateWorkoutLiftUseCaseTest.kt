
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateWorkoutLiftUseCaseTest {
/*


    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateWorkoutLiftUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = UpdateWorkoutLiftUseCase(programsRepository, transactionScope)
    }

    @Test
    @DisplayName("Updates a custom lift and includes its sets in the delta")
    fun updates_custom_lift_and_sets() = runTest {
        val custom = CustomWorkoutLift(
            id = 11L,
            workoutId = 22L,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            liftId = 33L,
            liftName = "Lift Name",
            liftNote = "Lift Note",
            liftMovementPattern = MovementPattern.HORIZONTAL_PULL,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = 0,
            deloadWeek = null,
            restTime = null,
            restTimerEnabled = true,
            incrementOverride = null,
            customLiftSets = listOf(
                StandardSet(
                    id = 44L,
                    workoutLiftId = 11L,
                    position = 0,
                    rpeTarget = 8.0f,
                    repRangeBottom = 8,
                    repRangeTop = 12
                ),
                StandardSet(
                    id = 45L,
                    workoutLiftId = 11L,
                    position = 0,
                    rpeTarget = 8.0f,
                    repRangeBottom = 8,
                    repRangeTop = 12
                ),
            ),
        )

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(10L), capture(captured)) }

        useCase.invoke(programId = 10L, workoutLift = custom as GenericWorkoutLift)

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        val wc = delta.workouts[0]
        assertEquals(22L, wc.workoutId)
        val lc = wc.lifts[0]
        assertEquals(11L, lc.workoutLiftId)
        assertEquals(2, lc.sets.size) // both custom sets included
    }

*/
}
