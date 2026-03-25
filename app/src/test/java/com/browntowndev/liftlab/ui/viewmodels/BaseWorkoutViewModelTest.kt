
package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.REST_TIME
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.ui.models.workoutLogging.ActiveProgramMetadataUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingStandardSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutUiModel
import com.browntowndev.liftlab.ui.viewmodels.workout.BaseWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.workout.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
class BaseWorkoutViewModelTest {

    @RelaxedMockK lateinit var completeSetUseCase: CompleteSetUseCase
    @RelaxedMockK lateinit var undoSetCompletionUseCase: UndoSetCompletionUseCase
    @RelaxedMockK lateinit var eventBus: EventBus

    private val mainDispatcher = StandardTestDispatcher()

    private class TestVM(
        completeSetUseCase: CompleteSetUseCase,
        undoSetCompletionUseCase: UndoSetCompletionUseCase,
        eventBus: EventBus
    ) : BaseWorkoutViewModel(completeSetUseCase, undoSetCompletionUseCase, eventBus, false) {

        var lastUpsertManyCount: Int? = null
        var lastUpsertOne: SetResult? = null
        var lastDeletedId: Long? = null

        override suspend fun upsertManySetResults(updatedResults: List<SetResult>): List<Long> {
            lastUpsertManyCount = updatedResults.size
            return listOf(1L, 2L)
        }

        override suspend fun upsertSetResult(updatedResult: SetResult): Long {
            lastUpsertOne = updatedResult
            return 42L
        }

        override suspend fun deleteSetResult(id: Long) {
            lastDeletedId = id
        }
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)
        every { eventBus.register(any()) } just Runs
        every { eventBus.unregister(any()) } just Runs

        mockkObject(SettingsManager)
        every { SettingsManager.getSetting(REST_TIME, DEFAULT_REST_TIME) } returns DEFAULT_REST_TIME
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
        unmockkObject(SettingsManager::class)
        Dispatchers.resetMain()
    }

    private fun injectWorkoutState(
        vm: BaseWorkoutViewModel,
        workout: LoggingWorkoutUiModel?,
        program: ActiveProgramMetadataUiModel?
    ) {
        val field = BaseWorkoutViewModel::class.java.getDeclaredField("mutableWorkoutState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(vm) as MutableStateFlow<WorkoutState>
        flow.update { it.copy(workout = workout, programMetadata = program) }
    }

    private fun sampleWorkout(): LoggingWorkoutUiModel {
        val lifts = listOf(
            LoggingWorkoutLiftUiModel(
                id = 1L,
                liftId = 100L,
                position = 0,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                deloadWeek = null,
                incrementOverride = null,
                sets = emptyList(),
                restTime = DEFAULT_REST_TIME.toDuration(DurationUnit.MILLISECONDS),
                isCustom = false,
            ),
            LoggingWorkoutLiftUiModel(
                id = 2L,
                liftId = 200L,
                position = 1,
                progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                deloadWeek = 2,
                incrementOverride = null,
                sets = emptyList(),
                restTime = DEFAULT_REST_TIME.toDuration(DurationUnit.MILLISECONDS),
                isCustom = false,
            ),
        )
        return LoggingWorkoutUiModel(id = 10L, name = "W", lifts = lifts)
    }

    private fun sampleProgram(): ActiveProgramMetadataUiModel =
        ActiveProgramMetadataUiModel(
            programId = 7L,
            name = "P",
            deloadWeek = 3,
            currentMesocycle = 1,
            currentMicrocycle = 0,
            currentMicrocyclePosition = 1,
            workoutCount = 3
        )

    @Test
    fun buildSetResult_standard_buildsExpectedFields() = runTest {
        val vm = TestVM(completeSetUseCase, undoSetCompletionUseCase, eventBus)
        injectWorkoutState(vm, sampleWorkout(), sampleProgram())

        val res = vm.buildSetResult(
            liftId = 100L,
            setType = SetType.STANDARD,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            liftPosition = 0,
            setPosition = 1,
            myoRepSetPosition = null,
            weight = 100f,
            reps = 5,
            rpe = 8f
        )

        assertEquals(10L, res.workoutId)
        assertEquals(100L, res.liftId)
        assertEquals(0, res.liftPosition)
        assertEquals(1, res.setPosition)
        assertEquals(SetType.STANDARD, res.setType)
    }

    @Test
    fun completeSet_delegatesToUseCase_andInvokesCallback() = runTest {
        val vm = TestVM(completeSetUseCase, undoSetCompletionUseCase, eventBus)
        injectWorkoutState(vm, sampleWorkout(), sampleProgram())

        // Cause the onUpsertSetResult callback to be invoked
        coEvery {
            completeSetUseCase(
                restTimeInMillis = any(),
                restTimerEnabled = any(),
                result = any(),
                existingSetResults = any(),
                onUpsertSetResult = any()
            )
        } coAnswers {
            val cb = arg<suspend (SetResult) -> Long>(4)
            cb.invoke(mockk(relaxed = true))
        }

        vm.completeSet(restTime = 0L, restTimerEnabled = false, onBuildSetResult =  {
            vm.buildSetResult(
                liftId = 100L,
                setType = SetType.STANDARD,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                liftPosition = 0,
                setPosition = 1,
                myoRepSetPosition = null,
                weight = 100f,
                reps = 5,
                rpe = 8f
            )
        })

        mainDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.lastUpsertOne) // callback went through to VM's implementation
    }

    @Test
    fun undoSetCompletion_delegatesToUseCase_andInvokesDeleteCallback() = runTest {
        val vm = TestVM(completeSetUseCase, undoSetCompletionUseCase, eventBus)
        val workout = sampleWorkout().let { workoutUiModel ->
            workoutUiModel.copy(
                lifts = workoutUiModel.lifts.mapIndexed { index, liftUiModel ->
                    if (index == 0) {
                        liftUiModel.copy(
                            sets = listOf(
                                LoggingStandardSetUiModel(
                                    position = 0,
                                    complete = true,
                                    completedWeight = 100f,
                                    completedReps = 5,
                                    completedRpe = 8f,
                                    rpeTarget = 8f,
                                    repRangeTop = 12,
                                    repRangeBottom = 4,
                                    rpeTargetPlaceholder = "RPE",
                                    weightRecommendation = 100f,
                                    hadInitialWeightRecommendation = true,
                                    previousSetResultLabel = "Previous",
                                    repRangePlaceholder = "Reps",
                                    setNumberLabel = "Set 1",
                                    isNew = false,
                                )
                            )
                        )
                    } else {
                        liftUiModel
                    }
                }
            )
        }
        injectWorkoutState(vm, workout, sampleProgram())

        coEvery {
            undoSetCompletionUseCase(
                liftPosition = any(),
                setPosition = any(),
                myoRepSetPosition = any(),
                setResults = any(),
                onDeleteSetResult = any()
            )
        } coAnswers {
            val cb = arg<suspend (Long) -> Unit>(4)
            cb.invoke(77L)
        }

        vm.undoSetCompletion(liftPosition = 0, setPosition = 0, myoRepSetPosition = null)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(77L, vm.lastDeletedId)
        assertEquals(false, vm.workoutState.value.workout!!.lifts[0].sets[0].complete)
        assertEquals(5, vm.workoutState.value.workout!!.lifts[0].sets[0].completedReps)
        assertEquals(100f, vm.workoutState.value.workout!!.lifts[0].sets[0].completedWeight)
        assertEquals(8f, vm.workoutState.value.workout!!.lifts[0].sets[0].completedRpe)
    }
}
