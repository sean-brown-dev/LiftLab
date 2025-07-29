package com.browntowndev.liftlab.core.domain.useCase.workout
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
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
    private lateinit var setResultsRepository: PreviousSetResultsRepository
    private lateinit var setLogEntryRepository: SetLogEntryRepository
    private lateinit var completeWorkoutUseCase: CompleteWorkoutUseCase

    @BeforeEach
    fun setUp() {
        workoutInProgressRepository = mockk(relaxed = true)
        restTimerInProgressRepository = mockk(relaxed = true)
        programsRepository = mockk(relaxed = true)
        historicalWorkoutNamesRepository = mockk(relaxed = true)
        workoutLogRepository = mockk(relaxed = true)
        setResultsRepository = mockk(relaxed = true)
        setLogEntryRepository = mockk(relaxed = true)
        completeWorkoutUseCase = CompleteWorkoutUseCase(
            workoutInProgressRepository,
            restTimerInProgressRepository,
            programsRepository,
            historicalWorkoutNamesRepository,
            workoutLogRepository,
            setResultsRepository,
            setLogEntryRepository
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
        coVerify { programsRepository.updateMesoAndMicroCycle(programMetadata.programId, 1, 1, 1) }
        coVerify { historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(programMetadata.programId, workout.id) }
        coVerify(exactly = 0) { historicalWorkoutNamesRepository.insert(any()) }
        coVerify { workoutLogRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify { setLogEntryRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) }
        coVerify { setResultsRepository.deleteAllForPreviousWorkout(any(), any(), any(), any()) }
        coVerify(exactly = 0) { setResultsRepository.upsertMany(any()) }
    }
}
