package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.useCase.utils.WeightCalculationUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetWorkoutCompletionSummaryUseCaseTest {

    private lateinit var useCase: GetWorkoutCompletionSummaryUseCase

    @BeforeAll
    fun beforeAll() {
        mockkObject(WeightCalculationUtils)
    }

    @AfterAll
    fun afterAll() {
        unmockkAll()
    }

    @BeforeEach
    fun setUp() {
        useCase = GetWorkoutCompletionSummaryUseCase()
        // Deterministic 1RM: simple Epley-like calc (ignore RPE for simplicity)
        every { WeightCalculationUtils.getOneRepMax(any(), any(), any()) } answers {
            val w = firstArg<Float>()
            val r = secondArg<Int>()
            (w * (1f + (r / 30f))).toInt()
        }
    }

    @Test
    fun `picks best set, flags PR, includes uncompleted lifts and sorts by position`() {
        val bench = lift(liftId = 1L, name = "Bench", position = 0, setCount = 3)
        val squat = lift(liftId = 2L, name = "Squat", position = 1, setCount = 4)
        val workout = LoggingWorkout(
            id = 42L,
            name = "Upper A",
            lifts = listOf(bench, squat)
        )

        val sets = listOf(
            // bench sets
            result(liftId = 1L, liftPos = 0, setPos = 0, weight = 100f, reps = 5, rpe = 8f),
            result(liftId = 1L, liftPos = 0, setPos = 1, weight = 110f, reps = 4, rpe = 9f), // best 1RM
            result(liftId = 1L, liftPos = 0, setPos = 2, weight = 95f,  reps = 6, rpe = 8f),
            // no squat sets (should appear with zeros)
        )

        // PR lower than best computed 1RM to trigger isNewPersonalRecord = true
        val prs = listOf(PersonalRecord(liftId = 1L, personalRecord = 109))

        val summary = useCase(
            loggingWorkout = workout,
            personalRecords = prs,
            completedSets = sets
        )

        assertEquals("Upper A", summary.workoutName)

        // Sorted by lift position: Bench (0) then Squat (1)
        assertEquals(listOf(0, 1), summary.liftCompletionSummaries.map { it.liftPosition })

        val benchSummary = summary.liftCompletionSummaries.first { it.liftId == 1L }
        assertEquals("Bench", benchSummary.liftName)
        assertEquals(3, benchSummary.setsCompleted)
        assertEquals(3, benchSummary.totalSets) // planned equals completed
        // Best set expected from 110 x 4: 110 * (1 + 4/30) = 110 * 1.1333... ≈ 124
        assertEquals(4, benchSummary.bestSetReps)
        assertEquals(110f, benchSummary.bestSetWeight)
        assertEquals(124, benchSummary.bestSet1RM)
        assertTrue(benchSummary.isNewPersonalRecord)

        val squatSummary = summary.liftCompletionSummaries.first { it.liftId == 2L }
        assertEquals("Squat", squatSummary.liftName)
        assertEquals(0, squatSummary.setsCompleted)
        assertEquals(4, squatSummary.totalSets)
        assertEquals(0, squatSummary.bestSet1RM)
        assertFalse(squatSummary.isNewPersonalRecord)
    }

    @Test
    fun `totalSets equals setsCompleted when completed sets exceed planned (e_g_ myo reps or extras)`() {
        val ohp = lift(liftId = 10L, name = "OHP", position = 0, setCount = 3)
        val workout = LoggingWorkout(id = 1L, name = "Push", lifts = listOf(ohp))

        val sets = (0 until 5).map { i ->
            result(liftId = 10L, liftPos = 0, setPos = i, weight = 60f, reps = 3, rpe = 8f)
        }

        val summary = useCase(workout, emptyList(), sets)
        val ohpSummary = summary.liftCompletionSummaries.single()

        assertEquals(5, ohpSummary.setsCompleted)
        assertEquals(5, ohpSummary.totalSets) // bumped up to actual completed
    }

    @Test
    fun `zero or negative weight uses 1f for 1RM calc but still reports actual weight in bestSet`() {
        val curl = lift(liftId = 5L, name = "Curl", position = 0, setCount = 1)
        val workout = LoggingWorkout(id = 2L, name = "Arms", lifts = listOf(curl))

        val sets = listOf(
            result(liftId = 5L, liftPos = 0, setPos = 0, weight = 0f, reps = 10, rpe = 8f)
        )

        val summary = useCase(workout, emptyList(), sets)
        val curlSummary = summary.liftCompletionSummaries.single()

        // Verify WeightCalculationUtils called with weight coerced to 1f
        verify(exactly = 1) { WeightCalculationUtils.getOneRepMax(1f, 10, 8f) }

        // Displayed best-set weight stays 0f (original), but 1RM computed off 1f
        assertEquals(0f, curlSummary.bestSetWeight)
        assertEquals((1f * (1f + 10f / 30f)).toInt(), curlSummary.bestSet1RM)
    }

    @Test
    fun `handles set for unknown liftId by emitting Unknown Lift with sentinel ids and no PR`() {
        val workout = LoggingWorkout(id = 3L, name = "Misc", lifts = emptyList())

        val sets = listOf(
            result(liftId = 999L, liftPos = 0, setPos = 0, weight = 135f, reps = 5, rpe = 8f)
        )

        val summary = useCase(workout, personalRecords = emptyList(), completedSets = sets)
        val only = summary.liftCompletionSummaries.single()

        assertEquals("Unknown Lift", only.liftName)
        assertEquals(-1L, only.liftId)
        assertEquals(-1, only.liftPosition)
        assertEquals(1, only.setsCompleted)
        assertEquals(1, only.totalSets) // falls back to setsCompleted
        assertFalse(only.isNewPersonalRecord)
    }

    // ---------- helpers ----------

    private fun lift(liftId: Long, name: String, position: Int, setCount: Int): LoggingWorkoutLift =
        LoggingWorkoutLift(
            liftId = liftId,
            liftName = name,
            position = position,
            id = 0L,
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            note = null,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            deloadWeek = null,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            sets = List(setCount) { mockk(relaxed = true) }
        )

    private fun result(
        liftId: Long,
        liftPos: Int,
        setPos: Int,
        weight: Float,
        reps: Int,
        rpe: Float
    ): SetResult = mockk(relaxed = true) {
        every { this@mockk.liftId } returns liftId
        every { this@mockk.liftPosition } returns liftPos
        every { this@mockk.setPosition } returns setPos
        every { this@mockk.weight } returns weight
        every { this@mockk.reps } returns reps
        every { this@mockk.rpe } returns rpe
        // Properties like setType, oneRepMax, isDeload aren't used by the use case.
    }
}
