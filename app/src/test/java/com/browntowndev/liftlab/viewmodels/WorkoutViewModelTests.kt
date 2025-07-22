package com.browntowndev.liftlab.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.DropSet
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.StandardSet
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.WorkoutInProgress
import com.browntowndev.liftlab.core.data.repositories.HistoricalWorkoutNamesRepositoryImpl
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.data.repositories.WorkoutInProgressRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.core.domain.progression.StandardProgressionFactory
import com.browntowndev.liftlab.ui.viewmodels.WorkoutViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class WorkoutViewModelTests {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        // Set the main dispatcher to the test dispatcher
        Dispatchers.setMain(testDispatcher)

        mockkObject(SettingsManager)

        every {
            SettingsManager.getSettingFlow(USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS, DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS)
        } returns flowOf(DEFAULT_USE_ALL_WORKOUT_DATA_FOR_RECOMMENDATIONS)

        every {
            SettingsManager.getSettingFlow(ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION, DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION)
        } returns flowOf(DEFAULT_ONLY_USE_RESULTS_FOR_LIFTS_IN_SAME_POSITION)

        every {
            SettingsManager.getSettingFlow(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING)
        } returns flowOf(DEFAULT_LIFT_SPECIFIC_DELOADING)

        every { SettingsManager.getSetting(REST_TIME, DEFAULT_REST_TIME) } returns DEFAULT_REST_TIME
        every { SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT) } returns DEFAULT_INCREMENT_AMOUNT
        every { SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING) } returns DEFAULT_LIFT_SPECIFIC_DELOADING
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun cleanup() {
        // Reset the main dispatcher to the original one after each test
        Dispatchers.resetMain()
    }

    @Test
    fun `Myo rep set is created on completion of activation set that meets criterion`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                            )
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 30)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            val sets = viewModel.workoutState.value.workout!!.lifts[0].sets
            Assert.assertEquals(2, sets.size)
            Assert.assertTrue(sets[1] is LoggingMyoRepSet)
            Assert.assertEquals(0, (sets[1] as LoggingMyoRepSet).myoRepSetPosition)
            Assert.assertEquals(10f, (sets[1] as LoggingMyoRepSet).rpeTarget)
            Assert.assertEquals(100f, (sets[1] as LoggingMyoRepSet).weightRecommendation)
        }

    @Test
    fun `Myo rep set is created on completion of previous myorep set that meets criterion`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 30)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 0, 0, 20)
            viewModel.setWeight(0L, 0, 0, 100f)
            viewModel.setRpe(0L, 0, 0, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    myoRepSetPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 20,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            val sets = viewModel.workoutState.value.workout!!.lifts[0].sets
            Assert.assertEquals(3, sets.size)
            Assert.assertTrue(sets[2] is LoggingMyoRepSet)
            Assert.assertEquals(1, (sets[2] as LoggingMyoRepSet).myoRepSetPosition)
            Assert.assertEquals(10f, (sets[2] as LoggingMyoRepSet).rpeTarget)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSet).weightRecommendation)
        }

    @Test
    fun `Myo rep set is not created on completion of  myorep set that meets criterion if one exists after it already`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery { setResultsRepository.deleteById(any()) } just runs
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 30)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 0, 0, 20)
            viewModel.setWeight(0L, 0, 0, 100f)
            viewModel.setRpe(0L, 0, 0, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    myoRepSetPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 20,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            // Will trigger completeSet again
            viewModel.setWeight(0L, 0, null, 100f)

            // Manually undo and redo to test both paths
            viewModel.undoSetCompletion(0, 0, null)
            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            val sets = viewModel.workoutState.value.workout!!.lifts[0].sets
            Assert.assertEquals(3, sets.size)
            Assert.assertTrue(sets[2] is LoggingMyoRepSet)
            Assert.assertEquals(1, (sets[2] as LoggingMyoRepSet).myoRepSetPosition)
            Assert.assertEquals(10f, (sets[2] as LoggingMyoRepSet).rpeTarget)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSet).weightRecommendation)
        }

    @Test
    fun `Myo rep set is not created when rep floor is hit`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 30)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 0, 0, 20)
            viewModel.setWeight(0L, 0, 0, 100f)
            viewModel.setRpe(0L, 0, 0, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    myoRepSetPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 5,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            val sets = viewModel.workoutState.value.workout!!.lifts[0].sets
            Assert.assertEquals(3, sets.size)
            Assert.assertTrue(sets[2] is LoggingMyoRepSet)
            Assert.assertEquals(1, (sets[2] as LoggingMyoRepSet).myoRepSetPosition)
            Assert.assertEquals(10f, (sets[2] as LoggingMyoRepSet).rpeTarget)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSet).weightRecommendation)
        }

    @Test
    fun `Myo rep set is created when set matching is not completed`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                                setMatching = true,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 30)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 0, 0, 20)
            viewModel.setWeight(0L, 0, 0, 100f)
            viewModel.setRpe(0L, 0, 0, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    myoRepSetPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 20,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            val sets = viewModel.workoutState.value.workout!!.lifts[0].sets
            Assert.assertEquals(3, sets.size)
            Assert.assertTrue(sets[2] is LoggingMyoRepSet)
            Assert.assertEquals(1, (sets[2] as LoggingMyoRepSet).myoRepSetPosition)
            Assert.assertEquals(10f, (sets[2] as LoggingMyoRepSet).rpeTarget)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSet).weightRecommendation)
        }

    @Test
    fun `Myo rep set is not created when set matching is completed`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                                setMatching = true,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 30)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 0, 0, 20)
            viewModel.setWeight(0L, 0, 0, 100f)
            viewModel.setRpe(0L, 0, 0, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    myoRepSetPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 20,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 0, 1, 10)
            viewModel.setWeight(0L, 0, 1, 100f)
            viewModel.setRpe(0L, 0, 1, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    myoRepSetPosition = 1,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 10,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            val sets = viewModel.workoutState.value.workout!!.lifts[0].sets
            Assert.assertEquals(3, sets.size)
            Assert.assertTrue(sets[2] is LoggingMyoRepSet)
            Assert.assertEquals(1, (sets[2] as LoggingMyoRepSet).myoRepSetPosition)
            Assert.assertEquals(10f, (sets[2] as LoggingMyoRepSet).rpeTarget)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSet).weightRecommendation)
        }

    @Test
    fun `Weight recommendation is set on drop set when preceding set is completed`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            StandardSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                            ),
                            DropSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 10)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                StandardSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    setType = SetType.DROP_SET,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 10,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            val sets = viewModel.workoutState.value.workout!!.lifts[0].sets
            Assert.assertEquals(90f, sets[1].weightRecommendation)
        }

    @Test
    fun `Weight recommendation is set to null on drop set when preceding set with no weight recommendation is uncompleted`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            StandardSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                            ),
                            DropSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery { setResultsRepository.deleteById(any()) } just runs
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 10)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                StandardSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    setType = SetType.DROP_SET,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 10,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )
            viewModel.undoSetCompletion(0, 0, null)

            val sets = viewModel.workoutState.value.workout!!.lifts[0].sets
            Assert.assertEquals(null, sets[1].weightRecommendation)
        }

    @Test
    fun `Completing first workout does not change microcycle or mesocycle`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)
            coEvery { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) } coAnswers {
                Assert.assertEquals(0, args[1])
                Assert.assertEquals(0, args[2])
                Assert.assertEquals(1, args[3])
            }

            val historicalWorkoutNamesRepositoryImpl = mockk<HistoricalWorkoutNamesRepositoryImpl>()
            coEvery { historicalWorkoutNamesRepositoryImpl.getIdByProgramAndWorkoutId(any(), any()) } returns 0L

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns 0L
            coEvery { workoutLogRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) } just runs
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(),
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery { workoutInProgressRepositoryImpl.getFlow(any(), any()) } returns flowOf(workoutInProgressMetadata)
            coEvery { workoutInProgressRepositoryImpl.delete() } just runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.deleteAll() } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.deleteAllForPreviousWorkout(any(), any(), any(), any()) } just runs
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = historicalWorkoutNamesRepositoryImpl,
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.finishWorkout()
            coVerify(exactly = 1) { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) }
        }

    @Test
    fun `Completing deload week increments mesocycle`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 3,
                currentMesocycle = 0,
                currentMicrocyclePosition = 3,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)
            coEvery { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) } coAnswers {
                Assert.assertEquals(1, args[1])
                Assert.assertEquals(0, args[2])
                Assert.assertEquals(0, args[3])
            }

            val historicalWorkoutNamesRepositoryImpl = mockk<HistoricalWorkoutNamesRepositoryImpl>()
            coEvery { historicalWorkoutNamesRepositoryImpl.getIdByProgramAndWorkoutId(any(), any()) } returns 0L

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns 0L
            coEvery { workoutLogRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) } just runs
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(),
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery { workoutInProgressRepositoryImpl.getFlow(any(), any()) } returns flowOf(workoutInProgressMetadata)
            coEvery { workoutInProgressRepositoryImpl.delete() } just runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.deleteAll() } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.deleteAllForPreviousWorkout(any(), any(), any(), any()) } just runs
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = historicalWorkoutNamesRepositoryImpl,
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )
            viewModel.toggleCompletionSummary()
            viewModel.finishWorkout()
            coVerify(exactly = 1) { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) }
        }

    @Test
    fun `Completing last workout increments microcycle`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 3,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)
            coEvery { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) } coAnswers {
                Assert.assertEquals(0, args[1])
                Assert.assertEquals(1, args[2])
                Assert.assertEquals(0, args[3])
            }

            val historicalWorkoutNamesRepositoryImpl = mockk<HistoricalWorkoutNamesRepositoryImpl>()
            coEvery { historicalWorkoutNamesRepositoryImpl.getIdByProgramAndWorkoutId(any(), any()) } returns 0L

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns 0L
            coEvery { workoutLogRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) } just runs
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(),
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery { workoutInProgressRepositoryImpl.getFlow(any(), any()) } returns flowOf(workoutInProgressMetadata)
            coEvery { workoutInProgressRepositoryImpl.delete() } just runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.deleteAll() } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery { setResultsRepository.deleteAllForPreviousWorkout(any(), any(), any(), any()) } just runs
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = historicalWorkoutNamesRepositoryImpl,
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.finishWorkout()
            coVerify(exactly = 1) { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) }
        }

    @Test
    fun `Completing workout with failed LP goals increments failure properties`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)
            coEvery {
                programsRepository.updateMesoAndMicroCycle(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just runs

            val historicalWorkoutNamesRepositoryImpl = mockk<HistoricalWorkoutNamesRepositoryImpl>()
            coEvery {
                historicalWorkoutNamesRepositoryImpl.getIdByProgramAndWorkoutId(
                    any(),
                    any()
                )
            } returns 0L

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery {
                workoutLogRepository.insertWorkoutLogEntry(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns 0L
            coEvery { workoutLogRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) } just runs
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    StandardWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 2,
                        deloadWeek = null,
                        liftNote = null,
                        repRangeTop = 10,
                        repRangeBottom = 8,
                        rpeTarget = 8f,
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery { workoutInProgressRepositoryImpl.delete() } just runs
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs
            coEvery { restTimerInProgramsRepository.deleteAll() } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery { setResultsRepository.upsertMany(any()) } coAnswers {
                val results = args[0] as List<LinearProgressionSetResult>
                Assert.assertEquals(2, results.size)
                results.fastForEach {
                    Assert.assertEquals(1, it.missedLpGoals)
                }
                listOf(0L)
            }
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.deleteAllForPreviousWorkout(any(), any(), any(), any()) } just runs
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = historicalWorkoutNamesRepositoryImpl,
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 7)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                LinearProgressionSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 7,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    missedLpGoals = 0,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 1, null, 7)
            viewModel.setWeight(0L, 1, null, 100f)
            viewModel.setRpe(0L, 1, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                LinearProgressionSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 1,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 7,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    missedLpGoals = 0,
                    isDeload = false,
                )
            )
            viewModel.finishWorkout()

            coVerify(exactly = 1) { setResultsRepository.upsertMany(any()) }
        }

    @Test
    fun `Completing workout with successful LP goals resets failure properties`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)
            coEvery {
                programsRepository.updateMesoAndMicroCycle(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just runs

            val historicalWorkoutNamesRepositoryImpl = mockk<HistoricalWorkoutNamesRepositoryImpl>()
            coEvery {
                historicalWorkoutNamesRepositoryImpl.getIdByProgramAndWorkoutId(
                    any(),
                    any()
                )
            } returns 0L

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery {
                workoutLogRepository.insertWorkoutLogEntry(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns 0L
            coEvery { workoutLogRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) } just runs
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    StandardWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 2,
                        deloadWeek = null,
                        repRangeTop = 10,
                        repRangeBottom = 8,
                        rpeTarget = 8f,
                        liftNote = null,
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery { workoutInProgressRepositoryImpl.delete() } just runs
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs
            coEvery { restTimerInProgramsRepository.deleteAll() } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsertMany(any()) } coAnswers {
                val results = args[0] as List<LinearProgressionSetResult>
                Assert.assertEquals(2, results.size)
                results.fastForEach {
                    Assert.assertEquals(0, it.missedLpGoals)
                }
                listOf(0L)
            }
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery { setResultsRepository.deleteAllForPreviousWorkout(any(), any(), any(), any()) } just runs
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(listOf(
                LinearProgressionSetResult(
                    id = 0L,
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 10,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    setType = SetType.STANDARD,
                    missedLpGoals = 2,
                    isDeload = false,
                ),
                LinearProgressionSetResult(
                    id = 0L,
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 1,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 10,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    setType = SetType.STANDARD,
                    missedLpGoals = 2,
                    isDeload = false,
                ),
            ))
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = historicalWorkoutNamesRepositoryImpl,
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 10)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                LinearProgressionSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 7,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    missedLpGoals = 2,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 1, null, 10)
            viewModel.setWeight(0L, 1, null, 100f)
            viewModel.setRpe(0L, 1, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                LinearProgressionSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 1,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 7,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    missedLpGoals = 2,
                    isDeload = false,
                )
            )
            viewModel.finishWorkout()

            coVerify(exactly = 1) { setResultsRepository.upsertMany(any()) }
        }

    @Test
    fun `RPE is set properly for each set type`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)
            coEvery {
                programsRepository.updateMesoAndMicroCycle(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just runs

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery {
                workoutLogRepository.insertWorkoutLogEntry(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns 0L
            coEvery { workoutLogRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) } just runs
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                                setMatching = true,
                            ),
                            StandardSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                            ),
                            DropSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setRpe(0L, 0, null, 8.5f)
            Assert.assertEquals(8.5f, viewModel.workoutState.value.workout!!.lifts[0].sets[0].completedRpe)

            viewModel.setRpe(0L, 1, null, 8.5f)
            Assert.assertEquals(8.5f, viewModel.workoutState.value.workout!!.lifts[0].sets[1].completedRpe)

            viewModel.setRpe(0L, 2, null, 8.5f)
            Assert.assertEquals(8.5f, viewModel.workoutState.value.workout!!.lifts[0].sets[2].completedRpe)
        }

    @Test
    fun `Rest time is set at lift level`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)
            coEvery {
                programsRepository.updateMesoAndMicroCycle(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just runs

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery {
                workoutLogRepository.insertWorkoutLogEntry(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns 0L
            coEvery { workoutLogRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) } just runs
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                                setMatching = true,
                            ),
                            StandardSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                            ),
                            DropSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val liftsRepository = mockk<LiftsRepository>()
            coEvery { liftsRepository.updateRestTime(any(), any(), any()) } just runs

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = liftsRepository,
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.updateRestTime(0L, 120000L.toDuration(DurationUnit.MILLISECONDS), true)
            Assert.assertEquals(120000L.toDuration(DurationUnit.MILLISECONDS).inWholeMilliseconds, viewModel.workoutState.value.workout!!.lifts[0].restTime?.inWholeMilliseconds)
        }

    @Test
    fun `Myorep set is successfully deleted`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)
            coEvery {
                programsRepository.updateMesoAndMicroCycle(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just runs

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery {
                workoutLogRepository.insertWorkoutLogEntry(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns 0L
            coEvery { workoutLogRepository.insertFromPreviousSetResults(any(), any(), any(), any(), any()) } just runs
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            MyoRepSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                setGoal = 5,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)
            coEvery { restTimerInProgramsRepository.insert(any()) } just runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.deleteById(any()) } just runs
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.setReps(0L, 0, null, 30)
            viewModel.setWeight(0L, 0, null, 100f)
            viewModel.setRpe(0L, 0, null, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            viewModel.setReps(0L, 0, 0, 30)
            viewModel.setWeight(0L, 0, 0, 100f)
            viewModel.setRpe(0L, 0, 0, 8f)

            viewModel.completeSet(
                0L,
                true,
                MyoRepSetResult(
                    workoutId = 0L,
                    liftId = 0L,
                    liftPosition = 0,
                    setPosition = 0,
                    myoRepSetPosition = 0,
                    weightRecommendation = null,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                    isDeload = false,
                )
            )

            viewModel.deleteMyoRepSet(0L, 0, 0)
            Assert.assertEquals(2, viewModel.workoutState.value.workout!!.lifts[0].sets.size)
            Assert.assertEquals(null, (viewModel.workoutState.value.workout!!.lifts[0].sets[0] as LoggingMyoRepSet).myoRepSetPosition)
            Assert.assertEquals(0, (viewModel.workoutState.value.workout!!.lifts[0].sets[1] as LoggingMyoRepSet).myoRepSetPosition)

            coVerify(exactly = 1) { setResultsRepository.deleteById(any()) }
        }

    @Test
    fun `Cancelling workout deletes in progress workout and set results`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf()
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            coEvery { workoutInProgressRepositoryImpl.insert(any()) } just runs
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)
            coEvery { workoutInProgressRepositoryImpl.delete() } just runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery { setResultsRepository.deleteAllForWorkout(any(), any(), any()) } just runs
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()

            val workoutLiftsRepositoryImpl = mockk<WorkoutLiftsRepositoryImpl>()

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = workoutLiftsRepositoryImpl,
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = mockk(),
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.startWorkout()
            viewModel.toggleConfirmCancelWorkoutModal()
            viewModel.cancelWorkout()

            coVerify(exactly = 1) { workoutInProgressRepositoryImpl.delete() }
            coVerify(exactly = 1) { setResultsRepository.deleteAllForWorkout(any(), any(), any()) }
        }

    @Test
    fun `Update note updates note for workout lift`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadata(
                programId = 0L,
                name = "Test ProgramEntity",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadataFlow() } returns flowOf(activeProgramMetadata)

            val workoutsRepositoryImpl = mockk<WorkoutsRepositoryImpl>()
            val workoutEntity = Workout(
                id = 0L,
                programId = 0L,
                position = 0,
                name = "Test WorkoutEntity",
                lifts = listOf(
                    CustomWorkoutLift(
                        id = 0L,
                        workoutId = 0L,
                        liftId = 0L,
                        liftName = "Test LiftEntity 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftSecondaryVolumeTypes = null,
                        incrementOverride = 5f,
                        restTimerEnabled = true,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        liftNote = null,
                        customLiftSets = listOf(
                            StandardSet(
                                id = 0L,
                                workoutLiftId = 0L,
                                position = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                            )
                        )
                    )
                )
            )
            coEvery { workoutsRepositoryImpl.getByMicrocyclePosition(any(), any()) } returns flowOf(
                workoutEntity
            )

            val workoutInProgressRepositoryImpl = mockk<WorkoutInProgressRepositoryImpl>()
            val workoutInProgressMetadata = WorkoutInProgress(
                workoutId = 0L,
                startTime = getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepositoryImpl.getFlow(
                    any(),
                    any()
                )
            } returns flowOf(workoutInProgressMetadata)

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getFlow() } returns flowOf(null)

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.getPersonalRecordsForLiftsExcludingWorkout(any(), any(), any(), any()) } returns listOf()
            coEvery {
                setResultsRepository.getByWorkoutIdExcludingGivenMesoAndMicroFlow(any(), any(), any())
            } returns flowOf(emptyList())
            coEvery {
                setResultsRepository.getForWorkoutFlow(any(), any(), any())
            } returns flowOf(emptyList())

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val workoutLogRepository = mockk<com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepositoryImpl>()
            coEvery { workoutLogRepository.getMostRecentSetResultsForLiftIds(any(), any(), any()) } returns listOf()
            coEvery { workoutLogRepository.getPersonalRecordsForLifts(any()) } returns listOf()
            val liftsRepository = mockk<LiftsRepository>()
            coEvery { liftsRepository.updateNote(any(), any()) } just runs

            val viewModel = WorkoutViewModel(
                progressionFactory = StandardProgressionFactory(),
                programsRepository = programsRepository,
                workoutsRepositoryImpl = workoutsRepositoryImpl,
                workoutLiftsRepositoryImpl = mockk(),
                workoutInProgressRepositoryImpl = workoutInProgressRepositoryImpl,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepositoryImpl = mockk(),
                workoutLogRepository = workoutLogRepository,
                liftsRepository = liftsRepository,
                navigateToWorkoutHistory = { },
                cancelRestTimer = {},
                transactionScope = transactionScope,
                eventBus = mockk(),
            )
            val testNote = "This is a test note."
            viewModel.updateNote(0L, testNote)
            Assert.assertEquals(testNote, viewModel.workoutState.value.workout!!.lifts[0].note)
            coVerify(exactly = 1) { liftsRepository.updateNote(0L, testNote) }
        }
}