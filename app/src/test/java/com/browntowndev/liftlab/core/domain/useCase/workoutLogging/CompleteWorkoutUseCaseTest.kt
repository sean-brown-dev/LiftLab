package com.browntowndev.liftlab.core.domain.useCase.workoutLogging
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date
import kotlin.test.assertEquals

class CompleteWorkoutUseCaseTest {

    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var restTimerInProgressRepository: RestTimerInProgressRepository
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var historicalWorkoutNamesRepository: HistoricalWorkoutNamesRepository
    private lateinit var workoutLogRepository: WorkoutLogRepository
    private lateinit var setResultsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var setLogEntryRepository: SetLogEntryRepository
    private lateinit var completeWorkoutUseCase: CompleteWorkoutUseCase
    private lateinit var transactionScope: TransactionScope

    @BeforeEach
    fun setUp() {
        workoutInProgressRepository = mockk(relaxed = true)
        restTimerInProgressRepository = mockk(relaxed = true)
        programsRepository = mockk(relaxed = true)
        historicalWorkoutNamesRepository = mockk(relaxed = true)
        workoutLogRepository = mockk(relaxed = true)
        setResultsRepository = mockk(relaxed = true)
        setLogEntryRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        completeWorkoutUseCase = CompleteWorkoutUseCase(
            workoutInProgressRepository,
            restTimerInProgressRepository,
            programsRepository,
            historicalWorkoutNamesRepository,
            workoutLogRepository,
            setResultsRepository,
            setLogEntryRepository,
            transactionScope
        )

        mockkObject(SettingsManager)
        every { SettingsManager.getSetting(SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING, any()) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SettingsManager)
    }

    @Test
    fun `invoke with valid data completes workout`() = runTest {
        // Given
        val inProgressWorkout = WorkoutInProgressUiModel(startTime = Date(System.currentTimeMillis() - 3600000))
        val programMetadata = ActiveProgramMetadata(
            programId = 1L,
            name = "Test Program",
            deloadWeek = 4,
            currentMesocycle = 1,
            currentMicrocycle = 1,
            currentMicrocyclePosition = 0,
            workoutCount = 3
        )
        val workout = LoggingWorkout(id = 101L, name = "Test Workout", lifts = emptyList())
        val completedSets = emptyList<SetResult>()
        val isDeloadWeek = false
        val historicalWorkoutNameId = 201L

        coEvery { historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(any(), any()) } returns historicalWorkoutNameId
        coEvery { workoutLogRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns 301L

        // When
        completeWorkoutUseCase(inProgressWorkout, programMetadata, workout, completedSets, isDeloadWeek)

        // Then
        coVerify { workoutInProgressRepository.deleteAll() }
        coVerify { restTimerInProgressRepository.deleteAll() }
        coVerify { programsRepository.applyDelta(1L, any()) }
        coVerify { historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(programMetadata.programId, workout.id) }
        coVerify(exactly = 0) { historicalWorkoutNamesRepository.insert(any()) }
        coVerify { workoutLogRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify { setLogEntryRepository.insertFromLiveWorkoutCompletedSets(any(), any(), any()) }
        coVerify { setResultsRepository.deleteAll() }
        coVerify(exactly = 0) { setResultsRepository.upsertMany(any()) }
    }

    @Test
    fun `invoke - synchronizes myo-rep set positions and upserts corrected results`() = runTest {
        // myo-rep results for the same (liftId, liftPosition) but with a gap in myoRepSetPosition
        // First set has null (primer), second set is incorrectly "2" instead of "0".
        val r1 = MyoRepSetResult(
            id = 10L,
            workoutId = 101L,
            liftId = 11L,
            liftPosition = 0,
            setPosition = 0,
            weight = 100f,
            reps = 10,
            rpe = 10f,
            isDeload = false,
        )
        val r2 = MyoRepSetResult(
            id = 11L,
            workoutId = 101L,
            liftId = 11L,
            liftPosition = 0,
            setPosition = 0,
            myoRepSetPosition = 2,
            weight = 100f,
            reps = 10,
            rpe = 10f,
            isDeload = false,
        )

        // Given a workout whose lift positions match the completed sets
        val workout = LoggingWorkout(
            id = 101L,
            name = "Push A",
            lifts = listOf(
                LoggingWorkoutLift(
                    id = 1L,
                    liftId = 11L,
                    position = 0,
                    progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                    deloadWeek = null,
                    incrementOverride = null,
                    sets = listOf(
                        LoggingMyoRepSet(
                            position = 0,
                            rpeTarget = 10f,
                            repRangeBottom = 10,
                            repRangeTop = 12,
                            weightRecommendation = null,
                            hadInitialWeightRecommendation = false,
                            previousSetResultLabel = "",
                            repRangePlaceholder = "",
                            setNumberLabel = ""
                        )
                    )
                )
            )
        )

        // Make sure the workout's lift-position mapping matches the results so they are NOT excludedFromCopy
        // (If your lift type is different, set liftId=11 and position=0 accordingly.)
        // If your constructor differs, replace this with a mock for workout.lifts that returns a (liftId=11, position=0).
        // The key is that the mapping { 11L -> 0 } exists so neither r1 nor r2 are excluded.

        // Historical name exists so we don't insert a new one
        coEvery { historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(any(), any()) } returns 200L
        coEvery { workoutLogRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns 300L

        val inProgress = WorkoutInProgressUiModel(startTime = Date(System.currentTimeMillis() - 1_000))
        val meta = ActiveProgramMetadata(
            programId = 1L,
            name = "Test",
            deloadWeek = 0,
            currentMesocycle = 0,
            currentMicrocycle = 0,
            currentMicrocyclePosition = 0,
            workoutCount = 1
        )

        val capturedFixes = slot<List<MyoRepSetResult>>()
        coEvery { setResultsRepository.upsertMany(capture(capturedFixes)) } returns emptyList()

        // When
        completeWorkoutUseCase(
            inProgressWorkout = inProgress,
            programMetadata = meta,
            workout = workout,
            completedSets = listOf(r1, r2),
            isDeloadWeek = false
        )

        // Then: we should have upserted the corrected myo-rep position
        coVerify { setResultsRepository.upsertMany(any()) }
        assert(capturedFixes.captured.size == 1)
        assertEquals(0, capturedFixes.captured[0].myoRepSetPosition)

        // And we still copy to history and clear live results
        coVerify { setLogEntryRepository.insertFromLiveWorkoutCompletedSets(any(), any(), any()) }
        coVerify { setResultsRepository.deleteAll() }
    }

    @Test
    fun `updateLinearProgressionFailures - increments on failure and resets on success`() = runTest {
        // Build a minimal workout with one LP lift and two sets (positions 0 and 1)
        val lpLift = mockk<LoggingWorkoutLift>(relaxed = true) // Replace Any with your lift model if needed
        // Stub required properties on the lift via relaxed mocks:
        // - liftId = 42L
        // - progressionScheme = ProgressionScheme.LINEAR_PROGRESSION
        // - sets = two items with positions 0 and 1 and targets that will drive one fail and one pass
        // If your types differ, replace these `every { ... }` lines with the correct ones.
        every { lpLift.liftId } returns 42L
        every { lpLift.progressionScheme } returns ProgressionScheme.LINEAR_PROGRESSION

        val set0 = mockk<LoggingStandardSet>(relaxed = true)
        val set1 = mockk<LoggingStandardSet>(relaxed = true)
        every { set0.position } returns 0
        every { set0.repRangeBottom } returns 8
        every { set0.completedReps } returns 6  // FAIL reps < bottom
        every { set0.rpeTarget } returns 8.5f
        every { set0.completedRpe } returns 9.0f

        every { set1.position } returns 1
        every { set1.repRangeBottom } returns 8
        every { set1.completedReps } returns 8  // PASS meets bottom
        every { set1.rpeTarget } returns 8.5f
        every { set1.completedRpe } returns 8.0f

        every { lpLift.sets } returns listOf(set0, set1)

        val workout = mockk<LoggingWorkout>()
        every { workout.lifts } returns listOf(lpLift)

        // Completed results for those two set positions
        val res0 = mockk<LinearProgressionSetResult>()
        val res1 = mockk<LinearProgressionSetResult>()
        every { res0.liftId } returns 42L
        every { res0.setPosition } returns 0
        every { res0.missedLpGoals } returns 0
        val res0Inc = mockk<LinearProgressionSetResult>()
        every { res0.copy(missedLpGoals = 1) } returns res0Inc

        every { res1.liftId } returns 42L
        every { res1.setPosition } returns 1
        every { res1.missedLpGoals } returns 2  // should be reset to 0 because set1 meets goals
        val res1Reset = mockk<LinearProgressionSetResult>()
        every { res1.copy(missedLpGoals = 0) } returns res1Reset

        val capturedLpUpdates = slot<List<SetResult>>()
        coEvery { setResultsRepository.upsertMany(capture(capturedLpUpdates)) } returns emptyList()

        // When
        completeWorkoutUseCase.updateLinearProgressionFailures(
            completedSets = listOf(res0, res1),
            workout = workout
        )

        // Then
        coVerify { setResultsRepository.upsertMany(any()) }
        // We expect exactly two changes: increment res0 and reset res1
        assert(capturedLpUpdates.captured.size == 2)
    }
}
