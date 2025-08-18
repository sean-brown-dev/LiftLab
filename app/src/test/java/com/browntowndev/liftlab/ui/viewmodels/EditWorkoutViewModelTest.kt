
package com.browntowndev.liftlab.ui.viewmodels

import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_REST_TIME
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.TopAppBarAction
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.CompleteSetUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.DeleteSetLogEntryByIdUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.GetCompletedWorkoutStateFlowUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UndoSetCompletionUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertExistingSetResultUseCase
import com.browntowndev.liftlab.core.domain.useCase.workoutLogging.UpsertSetLogEntriesFromSetResultsUseCase
import com.browntowndev.liftlab.ui.models.controls.TopAppBarEvent
import com.browntowndev.liftlab.ui.models.workoutLogging.ActiveProgramMetadataUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutUiModel
import com.browntowndev.liftlab.ui.viewmodels.workout.BaseWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.workout.EditWorkoutViewModel
import com.browntowndev.liftlab.ui.viewmodels.workout.WorkoutState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.greenrobot.eventbus.EventBus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
class EditWorkoutViewModelTest {

    @RelaxedMockK lateinit var upsertSetLogEntriesFromSetResultsUseCase: UpsertSetLogEntriesFromSetResultsUseCase
    @RelaxedMockK lateinit var upsertExistingSetResultUseCase: UpsertExistingSetResultUseCase
    @RelaxedMockK lateinit var deleteSetLogEntryByIdUseCase: DeleteSetLogEntryByIdUseCase
    @RelaxedMockK lateinit var getCompletedWorkoutStateFlowUseCase: GetCompletedWorkoutStateFlowUseCase
    @RelaxedMockK lateinit var undoSetCompletionUseCase: UndoSetCompletionUseCase
    @RelaxedMockK lateinit var completeSetUseCase: CompleteSetUseCase
    @RelaxedMockK lateinit var eventBus: EventBus

    private val mainDispatcher = StandardTestDispatcher()
    private var navigatedBack = false

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        every { eventBus.register(any()) } just Runs
        every { eventBus.unregister(any()) } just Runs

        // Quiet init
        every { getCompletedWorkoutStateFlowUseCase(any()) } returns flowOf()

        navigatedBack = false
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
        Dispatchers.resetMain()
    }

    private fun newViewModel(): EditWorkoutViewModel {
        return EditWorkoutViewModel(
            workoutLogEntryId = 123L,
            upsertSetLogEntriesFromSetResultsUseCase = upsertSetLogEntriesFromSetResultsUseCase,
            upsertExistingSetResultUseCase = upsertExistingSetResultUseCase,
            deleteSetLogEntryByIdUseCase = deleteSetLogEntryByIdUseCase,
            onNavigateBack = { navigatedBack = true },
            getCompletedWorkoutStateFlowUseCase = getCompletedWorkoutStateFlowUseCase,
            undoSetCompletionUseCase = undoSetCompletionUseCase,
            completeSetUseCase = completeSetUseCase,
            eventBus = eventBus
        )
    }

    private fun injectWorkoutState(vm: BaseWorkoutViewModel, workout: LoggingWorkoutUiModel?, program: ActiveProgramMetadataUiModel?) {
        val field = BaseWorkoutViewModel::class.java.getDeclaredField("mutableWorkoutState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(vm) as MutableStateFlow<WorkoutState>
        flow.update { it.copy(workout = workout, programMetadata = program) }
    }

    private fun sampleWorkout(withLiftCount: Int = 1): LoggingWorkoutUiModel {
        val lifts = (0 until withLiftCount).map { idx ->
            LoggingWorkoutLiftUiModel(
                id = 1000L + idx,
                liftId = 2000L + idx,
                position = idx,
                progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                deloadWeek = null,
                incrementOverride = null,
                sets = emptyList(),
                restTime = DEFAULT_REST_TIME.toDuration(DurationUnit.MILLISECONDS),
            )
        }
        return LoggingWorkoutUiModel(id = 33L, name = "Workout", lifts = lifts)
    }

    private fun sampleProgram(): ActiveProgramMetadataUiModel =
        ActiveProgramMetadataUiModel(
            programId = 7L,
            name = "P",
            deloadWeek = 2,
            currentMesocycle = 1,
            currentMicrocycle = 1,
            currentMicrocyclePosition = 1,
            workoutCount = 3
        )

    // -------- tests ---------
    @Test
    fun completeSet_invokesUpsertExistingSetResult_viaCallback() = runTest {
        val vm = newViewModel()
        injectWorkoutState(vm, sampleWorkout(2), sampleProgram())

        // When completeSetUseCase is called, immediately call the provided onUpsertSetResult lambda
        coEvery {
            completeSetUseCase(
                restTime = any(),
                restTimerEnabled = any(),
                result = any(),
                existingSetResults = any(),
                onUpsertSetResult = any()
            )
        } coAnswers {
            val cb = arg<suspend (SetResult) -> Long>(4)
            val domainSet: SetResult = mockk {
                every { liftPosition } returns 1 // should pick the second lift from the sample workout
            }
            // invoke the VM's callback (which will call upsertExistingSetResultUseCase internally)
            cb.invoke(domainSet)
        }

        coEvery { upsertExistingSetResultUseCase(any(), any(), any()) } returns 99L

        vm.completeSet(restTime = 0L, restTimerEnabled = false, onBuildSetResult =  {
            vm.buildSetResult(
                liftId = 2000L,
                setType = SetType.STANDARD,
                progressionScheme = ProgressionScheme.LINEAR_PROGRESSION,
                liftPosition = 0,
                setPosition = 1,
                myoRepSetPosition = null,
                weight = 100f,
                reps = 5,
                rpe = 8f
            )
        })

        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { upsertExistingSetResultUseCase(123L, any(), any()) }
    }

    @Test
    fun undoSetCompletion_invokesDeleteSetLogEntry_viaCallback() = runTest {
        val vm = newViewModel()
        injectWorkoutState(vm, sampleWorkout(2), sampleProgram())

        // When undoSetCompletionUseCase is called, immediately call provided onDeleteSetResult lambda
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

        coEvery { deleteSetLogEntryByIdUseCase(any()) } returns 1

        vm.undoSetCompletion(liftPosition = 1, setPosition = 2, myoRepSetPosition = null)
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { deleteSetLogEntryByIdUseCase(77L) }
    }

    @Test
    fun navigatedBack_triggersNavigateBack() = runTest {
        val vm = EditWorkoutViewModel(
            workoutLogEntryId = 123L,
            upsertSetLogEntriesFromSetResultsUseCase = upsertSetLogEntriesFromSetResultsUseCase,
            upsertExistingSetResultUseCase = upsertExistingSetResultUseCase,
            deleteSetLogEntryByIdUseCase = deleteSetLogEntryByIdUseCase,
            onNavigateBack = { navigatedBack = true },
            getCompletedWorkoutStateFlowUseCase = getCompletedWorkoutStateFlowUseCase,
            undoSetCompletionUseCase = undoSetCompletionUseCase,
            completeSetUseCase = completeSetUseCase,
            eventBus = eventBus
        )

        vm.handleActionBarEvents(TopAppBarEvent.ActionEvent(TopAppBarAction.NavigatedBack))
        mainDispatcher.scheduler.advanceUntilIdle()

        assertTrue(navigatedBack)
    }
}
