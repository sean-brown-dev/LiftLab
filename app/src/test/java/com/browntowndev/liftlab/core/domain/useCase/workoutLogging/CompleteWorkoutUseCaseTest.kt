package com.browntowndev.liftlab.core.domain.useCase.workoutLogging
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
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
        coVerify { workoutInProgressRepository.delete() }
        coVerify { restTimerInProgressRepository.delete() }
        coVerify { programsRepository.applyDelta(1L, any()) }
        coVerify { historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(programMetadata.programId, workout.id) }
        coVerify(exactly = 0) { historicalWorkoutNamesRepository.insert(any()) }
        coVerify { workoutLogRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify { setLogEntryRepository.insertFromLiveWorkoutCompletedSets(any(), any()) }
        coVerify { setResultsRepository.deleteAll() }
        coVerify(exactly = 0) { setResultsRepository.upsertMany(any()) }
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
