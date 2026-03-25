package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.mapping.toSetResult
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class GetNewestSetResultsUseCaseTest {

    private lateinit var workoutLogRepository: WorkoutLogRepository
    private lateinit var getNewestSetResultsUseCase: GetNewestSetResultsUseCase

    @BeforeEach
    fun setUp() {
        workoutLogRepository = mockk()
        getNewestSetResultsUseCase = GetNewestSetResultsUseCase(workoutLogRepository)
    }

    @Test
    fun `invoke with empty liftIdsToSearchFor returns existing results`() = runTest {
        // Given
        val workout = Workout(id = 1, programId = 1, name = "Test Workout", position = 0, lifts = emptyList())
        val existingResults = listOf(
            StandardSetResult(id = 1, workoutId = 1, liftId = 101, liftPosition = 0, setPosition = 0,
                weight = 100f, reps = 5, rpe = 8f,
                setType = SetType.STANDARD, isDeload = false)
        )
        val liftIdsToSearchFor = emptyList<Long>()

        // When
        val result = getNewestSetResultsUseCase(workout, liftIdsToSearchFor, existingResults, false)

        // Then
        assertEquals(existingResults, result)
        coVerify(exactly = 0) { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any()) }
    }

    @Test
    fun `invoke returns merged results`() = runTest {
        // Given
        val workout = Workout(id = 1, programId = 1, name = "Test Workout", position = 0, lifts = emptyList())
        val existingResults = listOf(createSetLogEntry(id = 1, liftId = 101, liftPosition = 0)).fastMap {
            it.toSetResult(
                workoutId = workout.id,
                isLinearProgression = false
            )
        }
        val newResults = listOf(createSetLogEntry(id = 2, liftId = 102, liftPosition = 1, weight = 200f))
        val liftIdsToSearchFor = listOf(102L)

        coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any()) } returns newResults

        // When
        val result = getNewestSetResultsUseCase(workout, liftIdsToSearchFor, existingResults, false)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.containsAll(existingResults))
        assertTrue(result.containsAll(newResults.fastMap { it.toSetResult(workoutId = workout.id, isLinearProgression = false) }))
    }

    @Test
    fun `invoke calls repository with correct linear progression lift IDs`() = runTest {
        // Given
        val lpLift = StandardWorkoutLift(id = 1, workoutId = 1, liftId = 101, liftName = "Squat", liftMovementPattern = MovementPattern.LEG_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, position = 0, setCount = 3, progressionScheme = ProgressionScheme.LINEAR_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, liftNote = null, rpeTarget = 8.0f, repRangeBottom = 5, repRangeTop = 8)
        val dpLift = StandardWorkoutLift(id = 2, workoutId = 1, liftId = 102, liftName = "Bench", liftMovementPattern = MovementPattern.HORIZONTAL_PUSH, liftVolumeTypes = 1, liftSecondaryVolumeTypes = null, position = 1, setCount = 3, progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION, incrementOverride = null, restTime = 90.seconds, restTimerEnabled = true, deloadWeek = null, liftNote = null, rpeTarget = 8.0f, repRangeBottom = 5, repRangeTop = 8)

        val workout = Workout(id = 1, programId = 1, name = "Test Workout", position = 0, lifts = listOf(lpLift, dpLift))
        val liftIdsToSearchFor = listOf(101L, 102L)

        coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any()) } returns emptyList()

        // When
        getNewestSetResultsUseCase(workout, liftIdsToSearchFor, emptyList(), false)

        // Then
        coVerify {
            workoutLogRepository.getMostRecentSetResultsForLiftIds(
                liftIds = liftIdsToSearchFor,
                includeDeloads = false
            )
        }
    }

    private fun createSetLogEntry(
        id: Long,
        workoutLogEntryId: Long = 1,
        liftId: Long,
        liftPosition: Int,
        setPosition: Int = 0,
        weight: Float = 100f,
        reps: Int = 5,
        rpe: Float = 8f,
        setType: SetType = SetType.STANDARD,
        isDeload: Boolean = false,
        workoutLiftDeloadWeek: Int? = null,
        liftName: String = "Test Lift",
        liftMovementPattern: MovementPattern = MovementPattern.LEG_PUSH,
        progressionScheme: ProgressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
        myoRepSetPosition: Int? = null,
        repRangeTop: Int? = null,
        repRangeBottom: Int? = null,
        rpeTarget: Float = 8f,
        weightRecommendation: Float? = null,
        persistedOneRepMax: Int? = null,
        isPersonalRecord: Boolean = false,
        setMatching: Boolean = false,
        maxSets: Int? = null,
        repFloor: Int? = null,
        dropPercentage: Float? = null
    ): SetLogEntry {
        return SetLogEntry(
            id = id, workoutLogEntryId = workoutLogEntryId, liftId = liftId, liftPosition = liftPosition, setPosition = setPosition,
            weight = weight, reps = reps, rpe = rpe,
            setType = setType, isDeload = isDeload, workoutLiftDeloadWeek = workoutLiftDeloadWeek, liftName = liftName,
            liftMovementPattern = liftMovementPattern, progressionScheme = progressionScheme, myoRepSetPosition = myoRepSetPosition,
            repRangeTop = repRangeTop, repRangeBottom = repRangeBottom, rpeTarget = rpeTarget, weightRecommendation = weightRecommendation,
            persistedOneRepMax = persistedOneRepMax, isPersonalRecord = isPersonalRecord, setMatching = setMatching, maxSets = maxSets,
            repFloor = repFloor, dropPercentage = dropPercentage
        )
    }
}
