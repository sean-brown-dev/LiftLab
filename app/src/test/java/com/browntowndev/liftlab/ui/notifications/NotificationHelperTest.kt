
package com.browntowndev.liftlab.ui.notifications

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.LIFT_SPECIFIC_DELOADING
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.RestTimerInProgress
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutInProgress
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.util.Date

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@ExtendWith(RobolectricExtension::class)
@Config(sdk = [34], application = Application::class) // Your min/compile SDK
class NotificationHelperTest {

    private lateinit var helper: NotificationHelper

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var setResultsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var restTimerInProgressRepository: RestTimerInProgressRepository

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        programsRepository = mockk(relaxed = true)
        workoutsRepository = mockk(relaxed = true)
        workoutInProgressRepository = mockk(relaxed = true)
        setResultsRepository = mockk(relaxed = true)
        restTimerInProgressRepository = mockk(relaxed = true)

        helper = NotificationHelper(
            programRepository = programsRepository,
            workoutsRepository = workoutsRepository,
            workoutInProgressRepository = workoutInProgressRepository,
            setResultsRepository = setResultsRepository,
            restTimerInProgressRepository = restTimerInProgressRepository
        )

        mockkObject(SettingsManager)
        every { SettingsManager.getSetting(LIFT_SPECIFIC_DELOADING, DEFAULT_LIFT_SPECIFIC_DELOADING) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // -------------------- Logic: getActiveWorkoutMetadata --------------------

    @Test
    fun `getActiveWorkoutMetadata returns null when no active program`() = runTest {
        coEvery { programsRepository.getActive() } returns null

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNull(result)
    }

    @Test
    fun `getActiveWorkoutMetadata returns null when no workout in progress`() = runTest {
        coEvery { programsRepository.getActive() } returns mockProgram(1, 0, 3)
        every { workoutInProgressRepository.getFlow() } returns flowOf(null)

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNull(result)
    }

    @Test
    fun `getActiveWorkoutMetadata returns null when workout not found`() = runTest {
        coEvery { programsRepository.getActive() } returns mockProgram(2, 1, 4)
        every { workoutInProgressRepository.getFlow() } returns flowOf(mockWorkoutInProgress(42L))
        coEvery { workoutsRepository.getById(42L) } returns null

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNull(result)
    }

    @Test
    fun `first set of standard lift shows rep range with RPE`() = runTest {
        coEvery { programsRepository.getActive() } returns mockProgram(0, 0, 3)
        every { workoutInProgressRepository.getFlow() } returns flowOf(mockWorkoutInProgress(1L))

        val lift = standardLift(
            position = 0,
            setCount = 3,
            name = "Bench Press",
            repBottom = 8,
            repTop = 10,
            rpeTarget = 8f,
            progression = ProgressionScheme.DOUBLE_PROGRESSION
        )
        val workout = mockWorkout("PUSH A", listOf(lift))
        coEvery { workoutsRepository.getById(1L) } returns workout
        coEvery { setResultsRepository.getAll() } returns emptyList()

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNotNull(result)
        Assertions.assertEquals("PUSH A", result!!.workoutName)
        Assertions.assertTrue(result.nextSet.startsWith("Bench Press"))
        Assertions.assertTrue(result.nextSet.contains("8-10 reps"))
        Assertions.assertTrue(result.nextSet.contains("@8")) // robust to "8.0"
    }

    @Test
    fun `wave loading - first set includes RPE`() = runTest {
        coEvery { programsRepository.getActive() } returns mockProgram(1, 0, 3)
        every { workoutInProgressRepository.getFlow() } returns flowOf(mockWorkoutInProgress(2L))

        val lift = standardLift(
            position = 0,
            setCount = 4,
            name = "Overhead Press",
            repBottom = 5,
            repTop = 10,
            rpeTarget = 8f,
            progression = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            stepSize = 1,
            deloadWeek = 3
        )
        val workout = mockWorkout("PUSH B", listOf(lift))
        coEvery { workoutsRepository.getById(2L) } returns workout
        coEvery { setResultsRepository.getAll() } returns emptyList()

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNotNull(result)
        Assertions.assertTrue(result!!.nextSet.startsWith("Overhead Press"))
        Assertions.assertTrue(result.nextSet.contains("reps"))
        Assertions.assertTrue(result.nextSet.contains("@8"))
    }

    @Test
    fun `wave loading - intra-lift next set omits RPE`() = runTest {
        coEvery { programsRepository.getActive() } returns mockProgram(1, 1, 3)
        every { workoutInProgressRepository.getFlow() } returns flowOf(mockWorkoutInProgress(2L))

        val lift = standardLift(
            position = 0,
            setCount = 4,
            name = "Overhead Press",
            repBottom = 5,
            repTop = 10,
            rpeTarget = 8f,
            progression = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            stepSize = 1,
            deloadWeek = 3
        )
        val workout = mockWorkout("PUSH B", listOf(lift))
        coEvery { workoutsRepository.getById(2L) } returns workout

        coEvery { setResultsRepository.getAll() } returns listOf(testSetResult(liftPosition = 0, setPosition = 0))

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNotNull(result)
        Assertions.assertTrue(result!!.nextSet.startsWith("Overhead Press"))
        Assertions.assertTrue(result.nextSet.contains("reps"))
        Assertions.assertFalse(result.nextSet.contains("@"))
    }

    @Test
    fun `moves to next lift after finishing previous`() = runTest {
        coEvery { programsRepository.getActive() } returns mockProgram(0, 0, 3)
        every { workoutInProgressRepository.getFlow() } returns flowOf(mockWorkoutInProgress(3L))

        val liftA = standardLift(
            position = 0,
            setCount = 2,
            name = "Squat",
            repBottom = 6,
            repTop = 8,
            rpeTarget = 7f,
            progression = ProgressionScheme.DOUBLE_PROGRESSION
        )
        val liftB = standardLift(
            position = 1,
            setCount = 3,
            name = "Leg Press",
            repBottom = 10,
            repTop = 12,
            rpeTarget = 8f,
            progression = ProgressionScheme.DOUBLE_PROGRESSION
        )

        val workout = mockWorkout("LEGS A", listOf(liftA, liftB))
        coEvery { workoutsRepository.getById(3L) } returns workout

        coEvery { setResultsRepository.getAll() } returns listOf(testSetResult(liftPosition = 0, setPosition = 1))

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNotNull(result)
        Assertions.assertTrue(result!!.nextSet.startsWith("Leg Press"))
        Assertions.assertTrue(result.nextSet.contains("10-12 reps"))
        Assertions.assertTrue(result.nextSet.contains("@8"))
    }

    @Test
    fun `workout complete when last set of last lift done`() = runTest {
        coEvery { programsRepository.getActive() } returns mockProgram(0, 0, 3)
        every { workoutInProgressRepository.getFlow() } returns flowOf(mockWorkoutInProgress(4L))

        val lift = standardLift(
            position = 0,
            setCount = 1,
            name = "Deadlift",
            repBottom = 3,
            repTop = 5,
            rpeTarget = 8f,
            progression = ProgressionScheme.DOUBLE_PROGRESSION
        )
        val workout = mockWorkout("PULL A", listOf(lift))
        coEvery { workoutsRepository.getById(4L) } returns workout

        coEvery { setResultsRepository.getAll() } returns listOf(testSetResult(liftPosition = 0, setPosition = 0))

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNotNull(result)
        Assertions.assertEquals("Workout Complete!", result!!.nextSet)
    }

    @Test
    fun `custom lift uses per-set rep range and RPE`() = runTest {
        coEvery { programsRepository.getActive() } returns mockProgram(0, 0, 3)
        every { workoutInProgressRepository.getFlow() } returns flowOf(mockWorkoutInProgress(5L))

        val customLift = customLift(
            position = 0,
            name = "Cable Fly",
            sets = listOf(
                StandardSet(workoutLiftId = 0, position = 0, rpeTarget = 7f, repRangeBottom = 12, repRangeTop = 15),
                StandardSet(workoutLiftId = 0, position = 1, rpeTarget = 8f, repRangeBottom = 10, repRangeTop = 12)
            )
        )
        val workout = mockWorkout("PUSH C", listOf(customLift))
        coEvery { workoutsRepository.getById(5L) } returns workout

        coEvery { setResultsRepository.getAll() } returns listOf(testSetResult(liftPosition = 0, setPosition = 0))

        val result = helper.getActiveWorkoutMetadata()

        Assertions.assertNotNull(result)
        Assertions.assertTrue(result!!.nextSet.startsWith("Cable Fly"))
        Assertions.assertTrue(result.nextSet.contains("10-12 reps"))
        Assertions.assertTrue(result.nextSet.contains("@8"))
    }

    // -------------------- Android plumbing: Robolectric --------------------

    @Test
    fun `startRestTimerNotification - no timer - returns false and no service started`() = runTest {
        coEvery { restTimerInProgressRepository.get() } returns null

        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.nextStartedService // drain

        val isActive = helper.startRestTimerNotification(app)

        Assertions.assertFalse(isActive)
        Assertions.assertNull(shadowApp.nextStartedService)
    }

    @Test
    fun `startRestTimerNotification - positive remaining - returns true and starts service`() = runTest {
        val now = System.currentTimeMillis()
        coEvery { restTimerInProgressRepository.get() } returns RestTimerInProgress(
            restTime = 120_000L,
            timeStartedInMillis = now - 60_000L
        )

        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.nextStartedService // drain

        val isActive = helper.startRestTimerNotification(app)

        Assertions.assertTrue(isActive)
        val started = shadowApp.nextStartedService
        Assertions.assertNotNull(started)
        Assertions.assertEquals(
            RestTimerNotificationService::class.java.name,
            started!!.component?.className
        )
    }

    @Test
    fun `startRestTimerNotification - zero remaining - returns false and no service started`() = runTest {
        val now = System.currentTimeMillis()
        coEvery { restTimerInProgressRepository.get() } returns RestTimerInProgress(
            restTime = 60_000L,
            timeStartedInMillis = now - 60_000L
        )

        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.nextStartedService // drain

        val isActive = helper.startRestTimerNotification(app)

        Assertions.assertFalse(isActive)
        Assertions.assertNull(shadowApp.nextStartedService)
    }

    // -------------------- Helpers --------------------

    private fun mockProgram(currentMesocycle: Int, currentMicrocycle: Int, deloadWeek: Int): Program {
        val program = mockk<Program>(relaxed = true)
        every { program.currentMesocycle } returns currentMesocycle
        every { program.currentMicrocycle } returns currentMicrocycle
        every { program.deloadWeek } returns deloadWeek
        return program
    }

    private fun mockWorkoutInProgress(workoutId: Long): WorkoutInProgress {
        val wip = mockk<WorkoutInProgress>(relaxed = true)
        every { wip.workoutId } returns workoutId
        every { wip.startTime } returns Date()
        return wip
    }

    private fun mockWorkout(name: String, lifts: List<GenericWorkoutLift>): Workout {
        val workout = mockk<Workout>(relaxed = true)
        every { workout.name } returns name
        every { workout.lifts } returns lifts
        return workout
    }


    private fun standardLift(
        position: Int,
        setCount: Int,
        name: String,
        repBottom: Int,
        repTop: Int,
        rpeTarget: Float,
        progression: ProgressionScheme,
        stepSize: Int? = null,
        deloadWeek: Int? = null,
    ): StandardWorkoutLift {
        val lift = mockk<StandardWorkoutLift>(relaxed = true)
        every { lift.position } returns position
        every { lift.setCount } returns setCount
        every { lift.liftName } returns name
        every { lift.repRangeBottom } returns repBottom
        every { lift.repRangeTop } returns repTop
        every { lift.rpeTarget } returns rpeTarget
        every { lift.progressionScheme } returns progression
        every { lift.stepSize } returns stepSize
        every { lift.deloadWeek } returns deloadWeek
        return lift
    }

    private fun customLift(
        position: Int,
        name: String,
        sets: List<StandardSet>,
    ): CustomWorkoutLift {
        val lift = mockk<CustomWorkoutLift>(relaxed = true)
        every { lift.position } returns position
        every { lift.liftName } returns name
        every { lift.customLiftSets } returns sets
        every { lift.setCount } returns sets.size
        return lift
    }

    private data class TestSetResult(
        override val id: Long = 0L,
        override val workoutId: Long = 0L,
        override val liftId: Long = 0L,
        override val liftPosition: Int,
        override val setPosition: Int,
        override val weight: Float = 0f,
        override val reps: Int = 0,
        override val rpe: Float = 0f,
        override val oneRepMax: Int = 0,
        override val setType: SetType = SetType.STANDARD,
        override val isDeload: Boolean = false
    ) : SetResult {
        override fun copyBase(
            id: Long,
            workoutId: Long,
            liftId: Long,
            liftPosition: Int,
            setPosition: Int,
            weight: Float,
            reps: Int,
            rpe: Float,
            setType: SetType,
            isDeload: Boolean
        ): SetResult = copy(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftPosition = liftPosition,
            setPosition = setPosition,
            weight = weight,
            reps = reps,
            rpe = rpe,
            setType = setType,
            isDeload = isDeload
        )
    }

    private fun testSetResult(liftPosition: Int, setPosition: Int): SetResult =
        TestSetResult(liftPosition = liftPosition, setPosition = setPosition)

    private fun idleRobolectric() {
        // Run anything scheduled on foreground & background thread schedulers
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable()
        Robolectric.getBackgroundThreadScheduler().advanceToLastPostedRunnable()
        // Drain main looper tasks (Handler posts, etc.)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }
}
