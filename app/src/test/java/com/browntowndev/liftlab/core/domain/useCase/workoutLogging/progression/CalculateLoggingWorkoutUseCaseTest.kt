package com.browntowndev.liftlab.core.domain.useCase.workoutLogging.progression

import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CalculateLoggingWorkoutUseCaseTest {

    private lateinit var useCase: CalculateLoggingWorkoutUseCase

    fun getCalculationStandardWorkoutLift(
        id: Long,
        liftId: Long,
        progressionScheme: ProgressionScheme,
        setCount: Int = 3,
        deloadWeek: Int? = null,
        position: Int = 0,
        repRangeTop: Int = 8,
        repRangeBottom: Int = 6,
        rpeTarget: Float = 8f,
        stepSize: Int = 1
    ) = CalculationStandardWorkoutLift(
        id = id,
        liftId = liftId,
        progressionScheme = progressionScheme,
        setCount = setCount,
        deloadWeek = deloadWeek,
        position = position,
        incrementOverride = null,
        repRangeTop = repRangeTop,
        repRangeBottom = repRangeBottom,
        rpeTarget = rpeTarget,
        stepSize = stepSize,
    )

    @BeforeEach
    fun setUp() {
        useCase = CalculateLoggingWorkoutUseCase()
    }

    @Test
    fun `given double progression when invoke then returns logging workout with double progression`() {
        val workout = CalculationWorkout(
            id = 0L,
            lifts = listOf(
                getCalculationStandardWorkoutLift(
                    id = 0L,
                    liftId = 0L,
                    progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                )
            )
        )
        val previousSetResults = emptyList<SetResult>()
        val previousResultsForDisplay = emptyList<SetResult>()

        val result = useCase(
            workout = workout,
            previousSetResults = previousSetResults,
            previousResultsForDisplay = previousResultsForDisplay,
            microCycle = 0,
            programDeloadWeek = 4,
            useLiftSpecificDeloading = false,
            onlyUseResultsForLiftsInSamePosition = false
        )

        assertNotNull(result)
        assertEquals(1, result.lifts.size)
        assertEquals(ProgressionScheme.DOUBLE_PROGRESSION, result.lifts.first().progressionScheme)
    }

    @Test
    fun `given linear progression when invoke then returns logging workout with linear progression`() {
        val workout = CalculationWorkout(
            id = 0L,
            lifts = listOf(
                getCalculationStandardWorkoutLift(
                    id = 0L,
                    liftId = 0L,
                    progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                )
            )
        )
        val previousSetResults = emptyList<SetResult>()
        val previousResultsForDisplay = emptyList<SetResult>()

        val result = useCase(
            workout = workout,
            previousSetResults = previousSetResults,
            previousResultsForDisplay = previousResultsForDisplay,
            microCycle = 0,
            programDeloadWeek = 4,
            useLiftSpecificDeloading = false,
            onlyUseResultsForLiftsInSamePosition = false
        )

        assertNotNull(result)
        assertEquals(1, result.lifts.size)
        assertEquals(ProgressionScheme.LINEAR_PROGRESSION, result.lifts.first().progressionScheme)
    }

    @Test
    fun `given dynamic double progression when invoke then returns logging workout with dynamic double progression`() {
        val workout = CalculationWorkout(
            id = 0L,
            lifts = listOf(
                getCalculationStandardWorkoutLift(
                    id = 0L,
                    liftId = 0L,
                    progressionScheme = ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION,
                )
            )
        )
        val previousSetResults = emptyList<SetResult>()
        val previousResultsForDisplay = emptyList<SetResult>()

        val result = useCase(
            workout = workout,
            previousSetResults = previousSetResults,
            previousResultsForDisplay = previousResultsForDisplay,
            microCycle = 0,
            programDeloadWeek = 4,
            useLiftSpecificDeloading = false,
            onlyUseResultsForLiftsInSamePosition = false
        )

        assertNotNull(result)
        assertEquals(1, result.lifts.size)
        assertEquals(ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION, result.lifts.first().progressionScheme)
    }

    @Test
    fun `given wave loading progression when invoke then returns logging workout with wave loading progression`() {
        val workout = CalculationWorkout(
            id = 0L,
            lifts = listOf(
                getCalculationStandardWorkoutLift(
                    id = 0L,
                    liftId = 0L,
                    progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                )
            )
        )
        val previousSetResults = emptyList<SetResult>()
        val previousResultsForDisplay = emptyList<SetResult>()

        val result = useCase(
            workout = workout,
            previousSetResults = previousSetResults,
            previousResultsForDisplay = previousResultsForDisplay,
            microCycle = 0,
            programDeloadWeek = 4,
            useLiftSpecificDeloading = false,
            onlyUseResultsForLiftsInSamePosition = false
        )

        assertNotNull(result)
        assertEquals(1, result.lifts.size)
        assertEquals(ProgressionScheme.WAVE_LOADING_PROGRESSION, result.lifts.first().progressionScheme)
    }

    @Test
    fun `given deload week when invoke then deload week is set correctly`() {
        val workout = CalculationWorkout(
            id = 0L,
            lifts = listOf(
                getCalculationStandardWorkoutLift(
                    id = 0L,
                    liftId = 0L,
                    progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                )
            )
        )
        val previousSetResults = emptyList<SetResult>()
        val previousResultsForDisplay = emptyList<SetResult>()

        val result = useCase(
            workout = workout,
            previousSetResults = previousSetResults,
            previousResultsForDisplay = previousResultsForDisplay,
            microCycle = 2,
            programDeloadWeek = 4,
            useLiftSpecificDeloading = false,
            onlyUseResultsForLiftsInSamePosition = false
        )
        assertNotNull(result)
        assertEquals(1, result.lifts.size)
        assertEquals(4, result.lifts.first().deloadWeek)
    }
}