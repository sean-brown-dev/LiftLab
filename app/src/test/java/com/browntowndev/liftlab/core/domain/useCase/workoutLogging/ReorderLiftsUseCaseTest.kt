package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.seconds

class ReorderLiftsUseCaseTest {

    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var setResultsRepository: PreviousSetResultsRepository
    private lateinit var reorderWorkoutLiftsUseCase: ReorderWorkoutLiftsUseCase
    private lateinit var transactionScope: TransactionScope

    @BeforeEach
    fun setUp() {
        workoutLiftsRepository = mockk(relaxed = true)
        setResultsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        reorderWorkoutLiftsUseCase = ReorderWorkoutLiftsUseCase(workoutLiftsRepository, setResultsRepository, transactionScope)
    }

    @Test
    fun `invoke reorders lifts and updates set results correctly`() = runTest {
        // Given
        val workoutLifts = listOf(
            StandardWorkoutLift(
                id = 101,
                workoutId = 1,
                liftId = 201,
                liftName = "Squat",
                liftMovementPattern = MovementPattern.LEG_PUSH,
                liftVolumeTypes = 1,
                liftSecondaryVolumeTypes = null,
                position = 0,
                setCount = 3,
                progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                incrementOverride = null,
                restTime = 90.seconds,
                restTimerEnabled = true,
                deloadWeek = null,
                liftNote = null,
                rpeTarget = 8.0f,
                repRangeBottom = 5,
                repRangeTop = 8
            ),
            CustomWorkoutLift(
                id = 102,
                workoutId = 1,
                liftId = 202,
                liftName = "Bench Press",
                liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                liftVolumeTypes = 1,
                liftSecondaryVolumeTypes = null,
                position = 1,
                setCount = 4,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                deloadWeek = null,
                incrementOverride = null,
                restTime = 60.seconds,
                restTimerEnabled = true,
                liftNote = null,
                customLiftSets = emptyList()
            )
        )
        val loggingWorkoutLifts = workoutLifts.map {
            LoggingWorkoutLift(
                id = it.id,
                liftId = it.liftId,
                liftName = it.liftName,
                liftMovementPattern = it.liftMovementPattern,
                liftVolumeTypes = it.liftVolumeTypes,
                liftSecondaryVolumeTypes = it.liftSecondaryVolumeTypes,
                note = it.liftNote,
                position = it.position,
                progressionScheme = it.progressionScheme,
                deloadWeek = it.deloadWeek,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                sets = emptyList()
            )
        }
        val workout = LoggingWorkout(
            id = 1,
            name = "Test Workout",
            lifts = loggingWorkoutLifts
        )
        val completedSets = listOf(
            StandardSetResult(id = 301, workoutId = 1, liftId = 201, liftPosition = 0, setPosition = 0, weightRecommendation = null, weight = 100.0f, reps = 5, rpe = 8.0f, mesoCycle = 1, microCycle = 1, setType = SetType.STANDARD, isDeload = false),
            MyoRepSetResult(id = 302, workoutId = 1, liftId = 202, liftPosition = 1, setPosition = 0, weightRecommendation = null, weight = 50.0f, reps = 10, rpe = 8.0f, mesoCycle = 1, microCycle = 1, setType = SetType.MYOREP, isDeload = false),
            LinearProgressionSetResult(id = 303, workoutId = 1, liftId = 201, liftPosition = 0, setPosition = 1, weightRecommendation = null, weight = 100.0f, reps = 6, rpe = 8.0f, mesoCycle = 1, microCycle = 1, isDeload = false, missedLpGoals = 0)
        )
        val newWorkoutLiftIndices = mapOf(101L to 1, 102L to 0)

        coEvery { workoutLiftsRepository.getForWorkout(workout.id) } returns workoutLifts

        // When
        reorderWorkoutLiftsUseCase(workout, completedSets, newWorkoutLiftIndices)

        // Then
        coVerify {
            workoutLiftsRepository.updateMany(withArg { updatedLifts ->
                assertEquals(2, updatedLifts.size)
                val lift1 = updatedLifts.find { it.id == 101L }
                val lift2 = updatedLifts.find { it.id == 102L }
                assertEquals(1, lift1?.position)
                assertEquals(0, lift2?.position)
            })
        }

        coVerify {
            setResultsRepository.upsertMany(withArg { updatedSets ->
                assertEquals(3, updatedSets.size)
                val set1 = updatedSets.find { it.id == 301L }
                val set2 = updatedSets.find { it.id == 302L }
                val set3 = updatedSets.find { it.id == 303L }

                assertEquals(1, set1?.liftPosition)
                assertEquals(0, set2?.liftPosition)
                assertEquals(1, set3?.liftPosition)
            })
        }
    }

    @Test
    fun `invoke with no completed sets only reorders lifts`() = runTest {
        // Given
        val workoutLifts = listOf(
            StandardWorkoutLift(
                id = 101,
                workoutId = 1,
                liftId = 201,
                liftName = "Squat",
                liftMovementPattern = MovementPattern.LEG_PUSH,
                liftVolumeTypes = 1,
                liftSecondaryVolumeTypes = null,
                position = 0,
                setCount = 3,
                progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                incrementOverride = null,
                restTime = 90.seconds,
                restTimerEnabled = true,
                deloadWeek = null,
                liftNote = null,
                rpeTarget = 8.0f,
                repRangeBottom = 5,
                repRangeTop = 8
            )
        )
        val loggingWorkoutLifts = workoutLifts.map {
            LoggingWorkoutLift(
                id = it.id,
                liftId = it.liftId,
                liftName = it.liftName,
                liftMovementPattern = it.liftMovementPattern,
                liftVolumeTypes = it.liftVolumeTypes,
                liftSecondaryVolumeTypes = it.liftSecondaryVolumeTypes,
                note = it.liftNote,
                position = it.position,
                progressionScheme = it.progressionScheme,
                deloadWeek = it.deloadWeek,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                sets = emptyList()
            )
        }
        val workout = LoggingWorkout(
            id = 1,
            name = "Test Workout",
            lifts = loggingWorkoutLifts
        )
        val completedSets = emptyList<SetResult>()
        val newWorkoutLiftIndices = mapOf(101L to 0)

        coEvery { workoutLiftsRepository.getForWorkout(workout.id) } returns workoutLifts

        // When
        reorderWorkoutLiftsUseCase(workout, completedSets, newWorkoutLiftIndices)

        // Then
        coVerify { workoutLiftsRepository.updateMany(any()) }
        coVerify(exactly = 0) { setResultsRepository.upsertMany(any()) }
    }

    @Test
    fun `invoke with missing newWorkoutLiftIndices entry throws exception`() = runTest {
        // Given
        val workoutLifts = listOf(
            StandardWorkoutLift(
                id = 101,
                workoutId = 1,
                liftId = 201,
                liftName = "Squat",
                liftMovementPattern = MovementPattern.LEG_PUSH,
                liftVolumeTypes = 1,
                liftSecondaryVolumeTypes = null,
                position = 0,
                setCount = 3,
                progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                incrementOverride = null,
                restTime = 90.seconds,
                restTimerEnabled = true,
                deloadWeek = null,
                liftNote = null,
                rpeTarget = 8.0f,
                repRangeBottom = 5,
                repRangeTop = 8
            )
        )
        val loggingWorkoutLifts = workoutLifts.map {
            LoggingWorkoutLift(
                id = it.id,
                liftId = it.liftId,
                liftName = it.liftName,
                liftMovementPattern = it.liftMovementPattern,
                liftVolumeTypes = it.liftVolumeTypes,
                liftSecondaryVolumeTypes = it.liftSecondaryVolumeTypes,
                note = it.liftNote,
                position = it.position,
                progressionScheme = it.progressionScheme,
                deloadWeek = it.deloadWeek,
                incrementOverride = it.incrementOverride,
                restTime = it.restTime,
                restTimerEnabled = it.restTimerEnabled,
                sets = emptyList()
            )
        }
        val workout = LoggingWorkout(
            id = 1,
            name = "Test Workout",
            lifts = loggingWorkoutLifts
        )
        val newWorkoutLiftIndices = mapOf<Long, Int>() // Missing entry for 101L

        coEvery { workoutLiftsRepository.getForWorkout(workout.id) } returns workoutLifts

        // When / Then
        assertThrows<NullPointerException> {
            reorderWorkoutLiftsUseCase(workout, emptyList(), newWorkoutLiftIndices)
        }
    }
}