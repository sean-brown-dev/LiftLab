package com.browntowndev.liftlab.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.TransactionScope
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.LinearProgressionSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutInProgressDto
import com.browntowndev.liftlab.core.persistence.entities.LoggingRepository
import com.browntowndev.liftlab.core.persistence.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.persistence.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository
import com.browntowndev.liftlab.ui.viewmodels.WorkoutViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
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
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            )
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = mockk(),
                loggingRepository = mockk(),
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.completeSet(
                0L,
                MyoRepSetResultDto(
                    workoutId = 0L,
                    liftId = 0L,
                    setPosition = 0,
                    weight = 100f,
                    reps = 30,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                )
            )

            val sets = viewModel.state.value.workout!!.lifts[0].sets
            Assert.assertEquals(2, sets.size)
            Assert.assertTrue(sets[1] is LoggingMyoRepSetDto)
            Assert.assertEquals(0, (sets[1] as LoggingMyoRepSetDto).myoRepSetPosition)
            Assert.assertEquals(8f, (sets[1] as LoggingMyoRepSetDto).rpeTarget)
            Assert.assertEquals(100f, (sets[1] as LoggingMyoRepSetDto).weightRecommendation)
        }

    @Test
    fun `Myo rep set is created on completion of previous myorep set that meets criterion`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                completedReps = 30,
                                completedWeight = 100f,
                                completedRpe = 8f,
                            ),
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                myoRepSetPosition = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = mockk(),
                loggingRepository = mockk(),
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.completeSet(
                0L,
                MyoRepSetResultDto(
                    workoutId = 0L,
                    liftId = 0L,
                    setPosition = 0,
                    myoRepSetPosition = 0,
                    weight = 100f,
                    reps = 20,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                )
            )

            val sets = viewModel.state.value.workout!!.lifts[0].sets
            Assert.assertEquals(3, sets.size)
            Assert.assertTrue(sets[2] is LoggingMyoRepSetDto)
            Assert.assertEquals(1, (sets[2] as LoggingMyoRepSetDto).myoRepSetPosition)
            Assert.assertEquals(8f, (sets[2] as LoggingMyoRepSetDto).rpeTarget)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSetDto).weightRecommendation)
        }

    @Test
    fun `Myo rep set is not created when rep floor is hit`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                completedReps = 30,
                                completedWeight = 100f,
                                completedRpe = 8f,
                            ),
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                myoRepSetPosition = 0,
                                repFloor = 5,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = mockk(),
                loggingRepository = mockk(),
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.completeSet(
                0L,
                MyoRepSetResultDto(
                    workoutId = 0L,
                    liftId = 0L,
                    setPosition = 0,
                    myoRepSetPosition = 0,
                    weight = 100f,
                    reps = 5,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                )
            )

            val sets = viewModel.state.value.workout!!.lifts[0].sets
            Assert.assertEquals(2, sets.size)
            Assert.assertTrue(sets[1] is LoggingMyoRepSetDto)
            Assert.assertEquals(0, (sets[1] as LoggingMyoRepSetDto).myoRepSetPosition)
            Assert.assertEquals(8f, (sets[1] as LoggingMyoRepSetDto).rpeTarget)
            Assert.assertEquals(100f, (sets[1] as LoggingMyoRepSetDto).weightRecommendation)
        }

    @Test
    fun `Myo rep set is created when set matching is not completed`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                setMatching = true,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                completedReps = 30,
                                completedWeight = 100f,
                                completedRpe = 8f,
                            ),
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                myoRepSetPosition = 0,
                                setMatching = true,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                completedReps = 20,
                                completedWeight = 100f,
                                completedRpe = 8f,
                            ),
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                myoRepSetPosition = 1,
                                setMatching = true,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = mockk(),
                loggingRepository = mockk(),
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.completeSet(
                0L,
                MyoRepSetResultDto(
                    workoutId = 0L,
                    liftId = 0L,
                    setPosition = 0,
                    myoRepSetPosition = 1,
                    weight = 100f,
                    reps = 5,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                )
            )

            val sets = viewModel.state.value.workout!!.lifts[0].sets
            Assert.assertEquals(4, sets.size)
            Assert.assertTrue(sets[3] is LoggingMyoRepSetDto)

            // Completed set
            Assert.assertEquals(1, (sets[2] as LoggingMyoRepSetDto).myoRepSetPosition)
            Assert.assertEquals(8f, (sets[2] as LoggingMyoRepSetDto).rpeTarget)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSetDto).weightRecommendation)
            Assert.assertEquals(8f, (sets[2] as LoggingMyoRepSetDto).completedRpe)
            Assert.assertEquals(5, (sets[2] as LoggingMyoRepSetDto).completedReps)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSetDto).completedWeight)

            // New set
            Assert.assertEquals(2, (sets[3] as LoggingMyoRepSetDto).myoRepSetPosition)
            Assert.assertEquals(8f, (sets[3] as LoggingMyoRepSetDto).rpeTarget)
            Assert.assertEquals(100f, (sets[3] as LoggingMyoRepSetDto).weightRecommendation)
        }

    @Test
    fun `Myo rep set is not created when set matching is completed`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                setMatching = true,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                completedReps = 30,
                                completedWeight = 100f,
                                completedRpe = 8f,
                            ),
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                myoRepSetPosition = 0,
                                setMatching = true,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                completedReps = 25,
                                completedWeight = 100f,
                                completedRpe = 8f,
                            ),
                            LoggingMyoRepSetDto(
                                setPosition = 0,
                                myoRepSetPosition = 1,
                                setMatching = true,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = mockk(),
                loggingRepository = mockk(),
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.completeSet(
                0L,
                MyoRepSetResultDto(
                    workoutId = 0L,
                    liftId = 0L,
                    setPosition = 0,
                    myoRepSetPosition = 1,
                    weight = 100f,
                    reps = 5,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                )
            )

            val sets = viewModel.state.value.workout!!.lifts[0].sets
            Assert.assertEquals(3, sets.size)
            Assert.assertTrue(sets[2] is LoggingMyoRepSetDto)
            Assert.assertEquals(1, (sets[2] as LoggingMyoRepSetDto).myoRepSetPosition)
            Assert.assertEquals(8f, (sets[2] as LoggingMyoRepSetDto).rpeTarget)
            Assert.assertEquals(100f, (sets[2] as LoggingMyoRepSetDto).weightRecommendation)
        }

    @Test
    fun `Weight recommendation is set on drop set when preceding set is completed`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingStandardSetDto(
                                setPosition = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            ),
                            LoggingDropSetDto(
                                setPosition = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                weightRecommendation = null,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = mockk(),
                loggingRepository = mockk(),
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.completeSet(
                0L,
                StandardSetResultDto(
                    workoutId = 0L,
                    liftId = 0L,
                    setPosition = 0,
                    setType = SetType.STANDARD,
                    weight = 100f,
                    reps = 10,
                    rpe = 8f,
                    mesoCycle = 0,
                    microCycle = 0,
                )
            )

            val sets = viewModel.state.value.workout!!.lifts[0].sets
            Assert.assertEquals(90f, sets[1].weightRecommendation)
        }

    @Test
    fun `Weight recommendation is set to null on drop set when preceding set with no weight recommendation is uncompleted`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingStandardSetDto(
                                setPosition = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = null,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                complete = true,
                                completedRpe = 8f,
                                completedReps = 10,
                                completedWeight = 100f,
                            ),
                            LoggingDropSetDto(
                                setPosition = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                weightRecommendation = 90f,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.delete(any(), any(), any(), any()) } just Runs

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = mockk(),
                loggingRepository = mockk(),
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.undoSetCompletion(
                0L,
                0,
                null,
            )

            val sets = viewModel.state.value.workout!!.lifts[0].sets
            Assert.assertNull(sets[1].weightRecommendation)
        }

    @Test
    fun `Completing first workout does not change microcycle or mesocycle`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata
            coEvery { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) } coAnswers {
                Assert.assertEquals(0, args[1])
                Assert.assertEquals(0, args[2])
                Assert.assertEquals(1, args[3])
            }

            val historicalWorkoutNamesRepository = mockk<HistoricalWorkoutNamesRepository>()
            coEvery { historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(any(), any()) } returns 0L

            val loggingRepository = mockk<LoggingRepository>()
            coEvery { loggingRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any()) } returns 0L
            coEvery { loggingRepository.insertFromPreviousSetResults(any()) } just Runs

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingStandardSetDto(
                                setPosition = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            ),
                            LoggingDropSetDto(
                                setPosition = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                weightRecommendation = null,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery { workoutInProgressRepository.get(any(), any()) } returns workoutInProgressMetadata
            coEvery { workoutInProgressRepository.delete() } just Runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.deleteAll() } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.deleteAllNotForWorkout(any(), any(), any()) } just Runs

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = historicalWorkoutNamesRepository,
                loggingRepository = loggingRepository,
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
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
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 3,
                currentMesocycle = 0,
                currentMicrocyclePosition = 3,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata
            coEvery { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) } coAnswers {
                Assert.assertEquals(1, args[1])
                Assert.assertEquals(0, args[2])
                Assert.assertEquals(0, args[3])
            }

            val historicalWorkoutNamesRepository = mockk<HistoricalWorkoutNamesRepository>()
            coEvery { historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(any(), any()) } returns 0L

            val loggingRepository = mockk<LoggingRepository>()
            coEvery { loggingRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any()) } returns 0L
            coEvery { loggingRepository.insertFromPreviousSetResults(any()) } just Runs

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingStandardSetDto(
                                setPosition = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            ),
                            LoggingDropSetDto(
                                setPosition = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                weightRecommendation = null,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery { workoutInProgressRepository.get(any(), any()) } returns workoutInProgressMetadata
            coEvery { workoutInProgressRepository.delete() } just Runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.deleteAll() } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.deleteAllNotForWorkout(any(), any(), any()) } just Runs

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = historicalWorkoutNamesRepository,
                loggingRepository = loggingRepository,
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.finishWorkout()
            coVerify(exactly = 1) { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) }
        }

    @Test
    fun `Completing last workout increments microcycle`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 3,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata
            coEvery { programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any()) } coAnswers {
                Assert.assertEquals(0, args[1])
                Assert.assertEquals(1, args[2])
                Assert.assertEquals(0, args[3])
            }

            val historicalWorkoutNamesRepository = mockk<HistoricalWorkoutNamesRepository>()
            coEvery { historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(any(), any()) } returns 0L

            val loggingRepository = mockk<LoggingRepository>()
            coEvery { loggingRepository.insertWorkoutLogEntry(any(), any(), any(), any(), any()) } returns 0L
            coEvery { loggingRepository.insertFromPreviousSetResults(any()) } just Runs

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingStandardSetDto(
                                setPosition = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                            ),
                            LoggingDropSetDto(
                                setPosition = 1,
                                dropPercentage = .1f,
                                rpeTarget = 8f,
                                repRangeBottom = 25,
                                repRangeTop = 30,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                weightRecommendation = null,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery { workoutInProgressRepository.get(any(), any()) } returns workoutInProgressMetadata
            coEvery { workoutInProgressRepository.delete() } just Runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.deleteAll() } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.deleteAllNotForWorkout(any(), any(), any()) } just Runs

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = historicalWorkoutNamesRepository,
                loggingRepository = loggingRepository,
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
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
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata
            coEvery {
                programsRepository.updateMesoAndMicroCycle(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just Runs

            val historicalWorkoutNamesRepository = mockk<HistoricalWorkoutNamesRepository>()
            coEvery {
                historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(
                    any(),
                    any()
                )
            } returns 0L

            val loggingRepository = mockk<LoggingRepository>()
            coEvery {
                loggingRepository.insertWorkoutLogEntry(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns 0L
            coEvery { loggingRepository.insertFromPreviousSetResults(any()) } just Runs

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingStandardSetDto(
                                setPosition = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                complete = true,
                                completedRpe = 8f,
                                completedWeight = 100f,
                                completedReps = 7,
                            ),
                            LoggingStandardSetDto(
                                setPosition = 1,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                completedRpe = 8f,
                                completedWeight = 100f,
                                completedReps = 7
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata
            coEvery { workoutInProgressRepository.delete() } just Runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.deleteAll() } just Runs
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.deleteAllNotForWorkout(any(), any(), any()) } just Runs
            coEvery { setResultsRepository.upsertMany(any()) } coAnswers {
                val results = args[0] as List<LinearProgressionSetResultDto>
                Assert.assertEquals(2, results.size)
                results.fastForEach {
                    Assert.assertEquals(1, it.missedLpGoals)
                }
                listOf(0L)
            }

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = historicalWorkoutNamesRepository,
                loggingRepository = loggingRepository,
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.completeSet(
                0L,
                LinearProgressionSetResultDto(0L, 0L, 0L, 0, 100f, 7, 8f, 0, 0, 0)
            )

            viewModel.completeSet(
                0L,
                LinearProgressionSetResultDto(0L, 0L, 0L, 1, 100f, 7, 8f, 0, 0, 0)
            )
            viewModel.finishWorkout()

            coVerify(exactly = 1) { setResultsRepository.upsertMany(any()) }
        }

    @Test
    fun `Completing workout with successful LP goals resets failure properties`() =
        runTest(EmptyCoroutineContext, timeout = 5000.milliseconds) {
            val programsRepository = mockk<ProgramsRepository>()
            val activeProgramMetadata = ActiveProgramMetadataDto(
                programId = 0L,
                name = "Test Program",
                currentMicrocycle = 0,
                currentMesocycle = 0,
                currentMicrocyclePosition = 0,
                workoutCount = 4,
                deloadWeek = 4,
            )
            coEvery { programsRepository.getActiveProgramMetadata() } returns activeProgramMetadata
            coEvery {
                programsRepository.updateMesoAndMicroCycle(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } just Runs

            val historicalWorkoutNamesRepository = mockk<HistoricalWorkoutNamesRepository>()
            coEvery {
                historicalWorkoutNamesRepository.getIdByProgramAndWorkoutId(
                    any(),
                    any()
                )
            } returns 0L

            val loggingRepository = mockk<LoggingRepository>()
            coEvery {
                loggingRepository.insertWorkoutLogEntry(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns 0L
            coEvery { loggingRepository.insertFromPreviousSetResults(any()) } just Runs

            val workoutsRepository = mockk<WorkoutsRepository>()
            val workout = LoggingWorkoutDto(
                id = 0L,
                name = "Test Workout",
                lifts = listOf(
                    LoggingWorkoutLiftDto(
                        id = 0L,
                        liftId = 0L,
                        liftName = "Test Lift 1",
                        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
                        progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                        liftVolumeTypes = 1,
                        liftRestTime = null,
                        liftIncrementOverride = null,
                        incrementOverride = 5f,
                        restTime = null,
                        position = 0,
                        setCount = 1,
                        deloadWeek = null,
                        sets = listOf(
                            LoggingStandardSetDto(
                                setPosition = 0,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                complete = true,
                                completedRpe = 8f,
                                completedWeight = 100f,
                                completedReps = 10,
                            ),
                            LoggingStandardSetDto(
                                setPosition = 1,
                                rpeTarget = 8f,
                                repRangeBottom = 8,
                                repRangeTop = 10,
                                weightRecommendation = 100f,
                                previousSetResultLabel = "",
                                repRangePlaceholder = "",
                                completedRpe = 8f,
                                completedWeight = 100f,
                                completedReps = 10,
                            ),
                        )
                    )
                )
            )
            coEvery { workoutsRepository.getNextToPerform(any()) } returns flowOf(
                workout
            ).asLiveData()

            val workoutInProgressRepository = mockk<WorkoutInProgressRepository>()
            val workoutInProgressMetadata = WorkoutInProgressDto(
                workoutId = 0L,
                startTime = Utils.getCurrentDate(),
                completedSets = listOf()
            )
            coEvery {
                workoutInProgressRepository.get(
                    any(),
                    any()
                )
            } returns workoutInProgressMetadata
            coEvery { workoutInProgressRepository.delete() } just Runs

            val restTimerInProgramsRepository = mockk<RestTimerInProgressRepository>()
            coEvery { restTimerInProgramsRepository.getLive() } returns flowOf(null).asLiveData()
            coEvery { restTimerInProgramsRepository.deleteAll() } just Runs
            coEvery { restTimerInProgramsRepository.insert(any()) } just Runs

            val setResultsRepository = mockk<PreviousSetResultsRepository>()
            coEvery { setResultsRepository.upsert(any()) } returns 0L
            coEvery { setResultsRepository.deleteAllNotForWorkout(any(), any(), any()) } just Runs
            coEvery { setResultsRepository.upsertMany(any()) } coAnswers {
                val results = args[0] as List<LinearProgressionSetResultDto>
                Assert.assertEquals(2, results.size)
                results.fastForEach {
                    Assert.assertEquals(0, it.missedLpGoals)
                }
                listOf(0L)
            }

            val transactionScope = mockk<TransactionScope>()
            coEvery { transactionScope.execute(any()) } coAnswers {
                val function = args[0] as (suspend () -> Unit)
                function()
            }

            val viewModel = WorkoutViewModel(
                programsRepository = programsRepository,
                workoutsRepository = workoutsRepository,
                workoutInProgressRepository = workoutInProgressRepository,
                restTimerInProgressRepository = restTimerInProgramsRepository,
                setResultsRepository = setResultsRepository,
                historicalWorkoutNamesRepository = historicalWorkoutNamesRepository,
                loggingRepository = loggingRepository,
                liftsRepository = mockk(),
                workoutLiftsRepository = mockk(),
                transactionScope = transactionScope,
                eventBus = mockk(),
            )

            viewModel.completeSet(
                0L,
                LinearProgressionSetResultDto(0L, 0L, 0L, 0, 100f, 10, 8f, 0, 0, 2)
            )

            viewModel.completeSet(
                0L,
                LinearProgressionSetResultDto(0L, 0L, 0L, 1, 100f, 10, 8f, 0, 0, 2)
            )
            viewModel.finishWorkout()

            coVerify(exactly = 1) { setResultsRepository.upsertMany(any()) }
        }
}