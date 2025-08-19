package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.seconds

class ReorderLiftsUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var setResultsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var reorderWorkoutLiftsUseCase: ReorderWorkoutLiftsUseCase
    private lateinit var transactionScope: TransactionScope

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)
        setResultsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        reorderWorkoutLiftsUseCase = ReorderWorkoutLiftsUseCase(
            programsRepository,
            workoutLiftsRepository,
            setResultsRepository,
            transactionScope
        )
    }

    @Test
    fun `invoke reorders lifts via program delta and updates set results correctly`() = runTest {
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
                isCustom = false,
                sets = emptyList()
            )
        }
        val workout = LoggingWorkout(
            id = 1,
            name = "Test Workout",
            lifts = loggingWorkoutLifts
        )
        val completedSets = listOf(
            StandardSetResult(
                id = 301, workoutId = 1, liftId = 201, liftPosition = 0, setPosition = 0,
                weight = 100.0f, reps = 5, rpe = 8.0f,
                setType = SetType.STANDARD, isDeload = false
            ),
            MyoRepSetResult(
                id = 302, workoutId = 1, liftId = 202, liftPosition = 1, setPosition = 0,
                weight = 50.0f, reps = 10, rpe = 8.0f,
                setType = SetType.MYOREP, isDeload = false
            ),
            LinearProgressionSetResult(
                id = 303, workoutId = 1, liftId = 201, liftPosition = 0, setPosition = 1,
                weight = 100.0f, reps = 6, rpe = 8.0f,
                isDeload = false, missedLpGoals = 0
            )
        )
        val newWorkoutLiftIndices = mapOf(101L to 1, 102L to 0)

        coEvery { workoutLiftsRepository.getForWorkout(workout.id) } returns workoutLifts

        // Capture ProgramDelta passed to applyDelta
        val programDeltaSlot: CapturingSlot<ProgramDelta> = slot()
        coEvery {
            programsRepository.applyDelta(
                programId = 999L,
                delta = capture(programDeltaSlot)
            )
        } returns Unit

        // When
        reorderWorkoutLiftsUseCase(
            programId = 999L,
            workout = workout,
            completedSets = completedSets,
            newWorkoutLiftIndices = newWorkoutLiftIndices
        )

        // Then: ProgramsRepository is used with ProgramDelta that has correct liftUpdate positions
        coVerify(exactly = 1) { programsRepository.applyDelta(999L, any()) }
        val capturedDelta = programDeltaSlot.captured
        Assertions.assertEquals(1, capturedDelta.workouts.size)
        val workoutChange = capturedDelta.workouts.first()
        Assertions.assertEquals(workout.id, workoutChange.workoutId)

        // Order of updates follows the repository.getForWorkout() order: [101, 102]
        val idOrder = workoutChange.lifts.map { it.workoutLiftId }
        Assertions.assertEquals(listOf(101L, 102L), idOrder)

        val positionByLiftId =
            workoutChange.lifts.associate { it.workoutLiftId to (it.liftUpdate?.position) }
        Assertions.assertEquals(Patch.Set(newWorkoutLiftIndices[101L]), positionByLiftId[101L])
        Assertions.assertEquals(Patch.Set(newWorkoutLiftIndices[102L]), positionByLiftId[102L])

        // Then: completed sets are reindexed by liftPosition
        coVerify {
            setResultsRepository.upsertMany(withArg { updatedSets ->
                Assertions.assertEquals(3, updatedSets.size)
                val set1 = updatedSets.find { it.id == 301L }
                val set2 = updatedSets.find { it.id == 302L }
                val set3 = updatedSets.find { it.id == 303L }

                Assertions.assertEquals(1, set1?.liftPosition)
                Assertions.assertEquals(0, set2?.liftPosition)
                Assertions.assertEquals(1, set3?.liftPosition)
            })
        }
    }

    @Test
    fun `invoke with no completed sets only reorders lifts via program delta`() = runTest {
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
                isCustom = false,
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

        // Capture ProgramDelta
        val programDeltaSlot: CapturingSlot<ProgramDelta> = slot()
        coEvery {
            programsRepository.applyDelta(
                programId = 100L,
                delta = capture(programDeltaSlot)
            )
        } returns Unit

        // When
        reorderWorkoutLiftsUseCase(
            programId = 100L,
            workout = workout,
            completedSets = completedSets,
            newWorkoutLiftIndices = newWorkoutLiftIndices
        )

        // Then
        coVerify(exactly = 1) { programsRepository.applyDelta(100L, any()) }
        val capturedDelta = programDeltaSlot.captured
        val workoutChange = capturedDelta.workouts.first()
        val positionByLiftId =
            workoutChange.lifts.associate { it.workoutLiftId to (it.liftUpdate?.position) }
        Assertions.assertEquals(Patch.Set(0), positionByLiftId[101L])

        // No set results updates
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
                isCustom = false,
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
            reorderWorkoutLiftsUseCase(
                programId = 1L,
                workout = workout,
                completedSets = emptyList(),
                newWorkoutLiftIndices = newWorkoutLiftIndices
            )
        }
    }
}