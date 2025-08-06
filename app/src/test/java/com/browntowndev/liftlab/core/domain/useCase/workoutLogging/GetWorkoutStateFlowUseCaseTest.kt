package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import app.cash.turbine.test
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.metadata.WorkoutMetadata
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.progression.CalculateLoggingWorkoutUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetWorkoutStateFlowUseCaseTest {

    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var workoutLogRepository: WorkoutLogRepository
    private lateinit var liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var liftsRepository: LiftsRepository
    private lateinit var calculateLoggingWorkoutUseCase: CalculateLoggingWorkoutUseCase
    private lateinit var hydrateLoggingWorkoutWithCompletedSetsUseCase: HydrateLoggingWorkoutWithCompletedSetsUseCase
    private lateinit var hydrateLoggingWorkoutWithExistingLiftDataUseCase: HydrateLoggingWorkoutWithExistingLiftDataUseCase
    private lateinit var getPersonalRecordsUseCase: GetPersonalRecordsUseCase
    private lateinit var getWorkoutStateFlowUseCase: GetWorkoutStateFlowUseCase

    @BeforeEach
    fun setUp() {
        workoutsRepository = mockk(relaxed = true)
        workoutLogRepository = mockk(relaxed = true)
        liveWorkoutCompletedSetsRepository = mockk(relaxed = true)
        liftsRepository = mockk(relaxed = true)
        calculateLoggingWorkoutUseCase = mockk(relaxed = true)
        hydrateLoggingWorkoutWithCompletedSetsUseCase = mockk(relaxed = true)
        hydrateLoggingWorkoutWithExistingLiftDataUseCase = mockk(relaxed = true)
        getPersonalRecordsUseCase = mockk(relaxed = true)
        getWorkoutStateFlowUseCase = GetWorkoutStateFlowUseCase(
            workoutsRepository,
            workoutLogRepository,
            liveWorkoutCompletedSetsRepository,
            liftsRepository,
            calculateLoggingWorkoutUseCase,
            hydrateLoggingWorkoutWithCompletedSetsUseCase,
            hydrateLoggingWorkoutWithExistingLiftDataUseCase,
            getPersonalRecordsUseCase
        )

        mockkObject(SettingsManager)
        every { SettingsManager.getSettingFlow(SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS, any()) } returns flowOf(false)
        every { SettingsManager.getSettingFlow(SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING, any()) } returns flowOf(false)
        every { SettingsManager.getSettingFlow(SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION, any()) } returns flowOf(true)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SettingsManager)
    }

    @Test
    fun `when no workout found, emits default data`() = runTest {
        // Given
        val programMetadata = ActiveProgramMetadata(1, "Test", 4, 1, 1, 0, 3)
        every { workoutsRepository.getByMicrocyclePositionForCalculation(any(), any()) } returns flowOf(null)

        // When
        val flow = getWorkoutStateFlowUseCase(programMetadata)

        // Then
        flow.test {
            val item = awaitItem()
            assertNull(item.calculatedWorkoutPlan)
            assertEquals(emptyList<SetResult>(), item.completedSetsForSession)
            assertEquals(emptyMap<Long, PersonalRecord>(), item.personalRecords)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `happy path - emits fully calculated workout state`() = runTest {
        // Given
        val programMetadata = ActiveProgramMetadata(1, "Test", 4, 1, 1, 0, 3)
        val workout = CalculationWorkout(1, emptyList())
        val calculatedWorkout = LoggingWorkout(1, "Workout A", emptyList())
        val hydratedWorkout = calculatedWorkout.copy(name = "Hydrated Workout")
        val finalWorkout = hydratedWorkout.copy(name = "Final Workout")

        every { workoutsRepository.getByMicrocyclePositionForCalculation(any(), any()) } returns flowOf(workout)
        coEvery { getPersonalRecordsUseCase(any(), any(), any(), any()) } returns emptyMap<Long, PersonalRecord>()
        every { liveWorkoutCompletedSetsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any()) } returns flowOf(emptyList())
        coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns emptyList()
        coEvery { calculateLoggingWorkoutUseCase(any(), any(), any(), any(), any(), any(), any()) } returns calculatedWorkout
        every { liveWorkoutCompletedSetsRepository.getForWorkoutFlow(any(), any(), any()) } returns flowOf(emptyList())
        every { hydrateLoggingWorkoutWithCompletedSetsUseCase(any(), any(), any()) } returns emptyList()
        every { hydrateLoggingWorkoutWithExistingLiftDataUseCase(any(), any()) } returns hydratedWorkout
        every { workoutsRepository.getMetadataFlow(any()) } returns flowOf(WorkoutMetadata(id = 1L, name = "Final Workout"))
        every { liftsRepository.getManyMetadataFlow(any()) } returns flowOf(emptyList())

        // When
        val flow = getWorkoutStateFlowUseCase(programMetadata)

        // Then
        flow.test {
            awaitItem()
            val finalState = awaitItem()
            assertEquals(finalWorkout, finalState.calculatedWorkoutPlan)
            assertEquals(emptyMap<Long, PersonalRecord>(), finalState.personalRecords)
            assertEquals(emptyList<SetResult>(), finalState.completedSetsForSession)

            cancelAndConsumeRemainingEvents()
        }
    }
}
