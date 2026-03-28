package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HydrateLoggingWorkoutWithExistingLiftDataUseCaseTest {
/*


    private lateinit var useCase: HydrateLoggingWorkoutWithExistingLiftDataUseCase

    @BeforeEach
    fun setUp() {
        useCase = HydrateLoggingWorkoutWithExistingLiftDataUseCase()
    }

    @Test
    fun `given empty lifts to update from when invoke then returns original logging workout`() {
        val loggingWorkout = createLoggingWorkout(id = 1L, lifts = listOf(createLoggingWorkoutLift(id = 1, liftId = 1, sets = listOf(createLoggingStandardSet(position = 0)))))
        val liftsToUpdateFrom = emptyList<LoggingWorkoutLift>()

        val result = useCase(loggingWorkout, liftsToUpdateFrom)

        assertEquals(loggingWorkout, result)
    }

    @Test
    fun `given modified set when invoke then returns updated logging workout`() {
        val loggingWorkout = createLoggingWorkout(id = 1L, lifts = listOf(createLoggingWorkoutLift(id = 1, liftId = 1, position = 0, sets = listOf(createLoggingStandardSet(position = 0)))))
        val liftsToUpdateFrom = listOf(
            createLoggingWorkoutLift(
                id = 1,
                liftId = 1,
                position = 0,
                sets = listOf(
                    createLoggingStandardSet(
                        position = 0,
                        completedReps = 10,
                        completedWeight = 100.0f,
                        completedRpe = 8.5f,
                        complete = true
                    )
                )
            )
        )

        val result = useCase(loggingWorkout, liftsToUpdateFrom)

        assertNotEquals(loggingWorkout, result)
        val updatedLift = result.lifts.first()
        assertNotNull(updatedLift)
        val updatedSet = updatedLift.sets.first()
        assertNotNull(updatedSet)
        assertEquals(10, updatedSet.completedReps)
        assertEquals(100.0f, updatedSet.completedWeight)
        assertEquals(8.5f, updatedSet.completedRpe)
        assertTrue(updatedSet.complete)
    }

    @Test
    fun `given modified myo rep set when invoke then returns updated logging workout`() {
        val loggingWorkout = createLoggingWorkout(id = 1L, lifts = listOf(createLoggingWorkoutLift(id = 1, liftId = 1, position = 0, sets = listOf(createLoggingMyoRepSet(position = 0, myoRepSetPosition = 0)))))
        val liftsToUpdateFrom = listOf(
            createLoggingWorkoutLift(
                id = 1,
                liftId = 1,
                position = 0,
                sets = listOf(
                    createLoggingMyoRepSet(
                        position = 0,
                        myoRepSetPosition = 0,
                        completedReps = 10,
                        completedWeight = 100.0f,
                        completedRpe = 8.5f,
                        complete = true
                    )
                )
            )
        )

        val result = useCase(loggingWorkout, liftsToUpdateFrom)

        assertNotEquals(loggingWorkout, result)
        val updatedLift = result.lifts.first()
        assertNotNull(updatedLift)
        val updatedSet = updatedLift.sets.first() as LoggingMyoRepSet
        assertNotNull(updatedSet)
        assertEquals(10, updatedSet.completedReps)
        assertEquals(100.0f, updatedSet.completedWeight)
        assertEquals(8.5f, updatedSet.completedRpe)
        assertTrue(updatedSet.complete)
    }

    private fun createLoggingWorkout(
        id: Long,
        lifts: List<LoggingWorkoutLift>,
        name: String = "",
    ) = LoggingWorkout(
        id = id,
        lifts = lifts,
        name = name,
    )

    private fun createLoggingWorkoutLift(
        id: Long,
        liftId: Long,
        sets: List<GenericLoggingSet>,
        position: Int = 0,
        note: String = ""
    ) = LoggingWorkoutLift(
        id = id,
        liftId = liftId,
        sets = sets,
        position = position,
        note = note,
        liftName = "",
        liftMovementPattern = MovementPattern.LEG_PUSH,
        liftVolumeTypes = VolumeType.QUAD.bitMask,
        liftSecondaryVolumeTypes = null,
        progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
        incrementOverride = null,
        deloadWeek = null,
        restTime = null,
        restTimerEnabled = true,
        isCustom = false
    )

    private fun createLoggingStandardSet(
        position: Int,
        completedReps: Int? = null,
        completedWeight: Float? = null,
        completedRpe: Float? = null,
        complete: Boolean = false
    ) = LoggingStandardSet(
        position = position,
        completedReps = completedReps,
        completedWeight = completedWeight,
        completedRpe = completedRpe,
        complete = complete,
        initialWeightRecommendation = 0.0f, initialWeightRecommendation = 0.0f, weightRecommendation = 0.0f,
        repRangeBottom = 0,
        repRangeTop = 0,


        previousSetResultLabel = "",
        rpeTarget = 8f,
        setNumberLabel = ""
    )

    private fun createLoggingMyoRepSet(
        position: Int,
        myoRepSetPosition: Int,
        completedReps: Int? = null,
        completedWeight: Float? = null,
        completedRpe: Float? = null,
        complete: Boolean = false,
    ) = LoggingMyoRepSet(
        position = position,
        myoRepSetPosition = myoRepSetPosition,
        completedReps = completedReps,
        completedWeight = completedWeight,
        completedRpe = completedRpe,
        complete = complete,
        initialWeightRecommendation = 0.0f, initialWeightRecommendation = 0.0f, weightRecommendation = 0.0f,
        repRangeBottom = 0,
        repRangeTop = 0,


        previousSetResultLabel = "",
        rpeTarget = 8f,
        setNumberLabel = ""
    )

*/
}