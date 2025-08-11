package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

// Explicit JUnit Jupiter assertions only:
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class CloneProgramUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var setsRepository: CustomLiftSetsRepository
    private lateinit var useCase: CloneProgramUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        workoutsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)
        setsRepository = mockk(relaxed = true)
        useCase = CloneProgramUseCase()
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `clones program, workouts, lifts, and custom sets with correct field mapping and new parent IDs`() = runTest {
        // --- Source program structure ---
        val stdLiftA = standardLift(
            liftId = 101L, liftName = "Bench",
            position = 0, setCount = 3,
            rpe = 8f, repLo = 6, repHi = 8,
            progression = ProgressionScheme.DOUBLE_PROGRESSION, stepSize = 3
        )
        val customLiftA = customLift(
            liftId = 202L, liftName = "Row",
            position = 1, setCount = 4,
            sets = listOf(
                StandardSet(workoutLiftId = -1, position = 0, repRangeTop = 10, repRangeBottom = 8, rpeTarget = 7f),
                DropSet(workoutLiftId = -1, position = 1, repRangeTop = 12, repRangeBottom = 10, rpeTarget = 8f, dropPercentage = 0.25f),
                MyoRepSet(workoutLiftId = -1, position = 2, repRangeTop = 15, repRangeBottom = 12, rpeTarget = 8.5f, repFloor = 3, setGoal = 4, setMatching = true, maxSets = 6)
            )
        )
        val stdLiftB = standardLift(
            liftId = 303L, liftName = "Squat",
            position = 0, setCount = 3,
            rpe = 8.5f, repLo = 4, repHi = 6,
            progression = ProgressionScheme.DOUBLE_PROGRESSION, stepSize = 5
        )

        val wA = Workout(
            programId = 999, // irrelevant for source
            name = "Upper A",
            position = 1,
            lifts = listOf(stdLiftA, customLiftA)
        )
        val wB = Workout(
            programId = 999,
            name = "Lower A",
            position = 2,
            lifts = listOf(stdLiftB)
        )

        val sourceProgram = Program(
            name = "Powerbuilding",
            isActive = true,
            deloadWeek = 3
        ).copy(workouts = listOf(wA, wB))

        // --- Repo stubs & captures ---
        // Insert program -> returns new programId
        val insertedPrograms = slot<Program>()
        coEvery { programsRepository.insert(capture(insertedPrograms)) } returns 100L

        // Insert workouts -> return IDs based on name so we can assert mapping
        val insertedWorkouts = mutableListOf<Workout>()
        coEvery { workoutsRepository.insert(any()) } answers {
            val wo = arg<Workout>(0)
            insertedWorkouts += wo
            when (wo.name) {
                "Upper A" -> 200L
                "Lower A" -> 201L
                else -> error("Unexpected workout name")
            }
        }

        // Insert lifts -> return sequential IDs; we also capture the lifts passed
        val insertedLifts = mutableListOf<GenericWorkoutLift>()
        val liftIds = arrayOf(300L, 301L, 302L) // stdA -> 300, customA -> 301, stdB -> 302
        var liftIndex = 0
        coEvery { workoutLiftsRepository.insert(capture(insertedLifts)) } answers {
            liftIds[liftIndex++]
        }

        // Insert sets -> capture every cloned set
        val insertedSets = mutableListOf<GenericLiftSet>()
        coEvery { setsRepository.insert(capture(insertedSets)) } returnsMany listOf(800L, 801L, 802L)

        // --- Act ---
        useCase(
            programsRepository = programsRepository,
            workoutsRepository = workoutsRepository,
            workoutLiftsRepository = workoutLiftsRepository,
            setsRepository = setsRepository,
            program = sourceProgram
        )

        // --- Assert program insert ---
        assertEquals("Powerbuilding", insertedPrograms.captured.name)
        assertTrue(insertedPrograms.captured.isActive)
        assertEquals(3, insertedPrograms.captured.deloadWeek)

        // --- Assert workouts cloned with new programId and cleared lifts ---
        assertEquals(2, insertedWorkouts.size)
        val insertedA = insertedWorkouts.first { it.name == "Upper A" }
        val insertedB = insertedWorkouts.first { it.name == "Lower A" }
        assertEquals(100L, insertedA.programId)
        assertEquals(1, insertedA.position)
        assertTrue(insertedA.lifts.isEmpty(), "cloned workouts must be inserted with empty lifts")
        assertEquals(100L, insertedB.programId)
        assertEquals(2, insertedB.position)
        assertTrue(insertedB.lifts.isEmpty())

        // --- Assert lifts cloned with new workoutIds and correct fields ---
        assertEquals(3, insertedLifts.size)

        val clonedStdA = insertedLifts[0] as StandardWorkoutLift
        assertEquals(200L, clonedStdA.workoutId)
        assertEquals(stdLiftA.liftId, clonedStdA.liftId)
        assertEquals(stdLiftA.liftName, clonedStdA.liftName)
        assertEquals(stdLiftA.liftMovementPattern, clonedStdA.liftMovementPattern)
        assertEquals(stdLiftA.liftVolumeTypes, clonedStdA.liftVolumeTypes)
        assertEquals(stdLiftA.liftSecondaryVolumeTypes, clonedStdA.liftSecondaryVolumeTypes)
        assertEquals(stdLiftA.position, clonedStdA.position)
        assertEquals(stdLiftA.setCount, clonedStdA.setCount)
        assertEquals(stdLiftA.progressionScheme, clonedStdA.progressionScheme)
        assertEquals(stdLiftA.incrementOverride, clonedStdA.incrementOverride)
        assertEquals(stdLiftA.restTime, clonedStdA.restTime)
        assertEquals(stdLiftA.restTimerEnabled, clonedStdA.restTimerEnabled)
        assertEquals(stdLiftA.deloadWeek, clonedStdA.deloadWeek)
        assertEquals(stdLiftA.liftNote, clonedStdA.liftNote)
        assertEquals(stdLiftA.rpeTarget, clonedStdA.rpeTarget)
        assertEquals(stdLiftA.repRangeBottom, clonedStdA.repRangeBottom)
        assertEquals(stdLiftA.repRangeTop, clonedStdA.repRangeTop)
        assertEquals(stdLiftA.stepSize, clonedStdA.stepSize)

        val clonedCustomA = insertedLifts[1] as CustomWorkoutLift
        assertEquals(200L, clonedCustomA.workoutId)
        assertEquals(customLiftA.liftId, clonedCustomA.liftId)
        assertEquals(customLiftA.liftName, clonedCustomA.liftName)
        assertEquals(customLiftA.liftMovementPattern, clonedCustomA.liftMovementPattern)
        assertEquals(customLiftA.liftVolumeTypes, clonedCustomA.liftVolumeTypes)
        assertEquals(customLiftA.liftSecondaryVolumeTypes, clonedCustomA.liftSecondaryVolumeTypes)
        assertEquals(customLiftA.position, clonedCustomA.position)
        assertEquals(customLiftA.setCount, clonedCustomA.setCount)
        assertEquals(customLiftA.progressionScheme, clonedCustomA.progressionScheme)
        assertEquals(customLiftA.incrementOverride, clonedCustomA.incrementOverride)
        assertEquals(customLiftA.restTime, clonedCustomA.restTime)
        assertEquals(customLiftA.restTimerEnabled, clonedCustomA.restTimerEnabled)
        assertEquals(customLiftA.deloadWeek, clonedCustomA.deloadWeek)
        assertEquals(customLiftA.liftNote, clonedCustomA.liftNote)
        assertTrue(clonedCustomA.customLiftSets.isEmpty(), "custom lift sets must be cloned separately")

        val clonedStdB = insertedLifts[2] as StandardWorkoutLift
        assertEquals(201L, clonedStdB.workoutId)
        assertEquals(stdLiftB.liftId, clonedStdB.liftId)

        // --- Assert sets cloned against the new custom workoutLiftId (301) and field mapping preserved ---
        assertEquals(3, insertedSets.size)
        val s0 = insertedSets[0] as StandardSet
        val s1 = insertedSets[1] as DropSet
        val s2 = insertedSets[2] as MyoRepSet

        assertEquals(301L, s0.workoutLiftId)
        assertEquals(0, s0.position)
        assertEquals(10, s0.repRangeTop)
        assertEquals(8, s0.repRangeBottom)
        assertEquals(7f, s0.rpeTarget)

        assertEquals(301L, s1.workoutLiftId)
        assertEquals(1, s1.position)
        assertEquals(12, s1.repRangeTop)
        assertEquals(10, s1.repRangeBottom)
        assertEquals(8f, s1.rpeTarget)
        assertEquals(0.25f, s1.dropPercentage)

        assertEquals(301L, s2.workoutLiftId)
        assertEquals(2, s2.position)
        assertEquals(15, s2.repRangeTop)
        assertEquals(12, s2.repRangeBottom)
        assertEquals(8.5f, s2.rpeTarget)
        assertEquals(3, s2.repFloor)
        assertEquals(4, s2.setGoal)
        assertTrue(s2.setMatching)
        assertEquals(6, s2.maxSets)
    }

    @Test
    fun `throws when encountering an unknown lift type`() = runTest {
        // Unknown (non-Standard, non-Custom) lift
        val unknownLift = mockk<GenericWorkoutLift>(relaxed = true)
        val workout = Workout(programId = 1L, name = "W", position = 0, lifts = listOf(unknownLift))
        val program = Program(name = "P", isActive = false, deloadWeek = 0).copy(workouts = listOf(workout))

        coEvery { programsRepository.insert(any()) } returns 1L
        coEvery { workoutsRepository.insert(any()) } returns 2L

        assertThrows<Exception> {
            useCase(programsRepository, workoutsRepository, workoutLiftsRepository, setsRepository, program)
        }

        // If the code is correct, we should have attempted to clone the workout then fail on the lift type
        coVerify(exactly = 1) { programsRepository.insert(any()) }
        coVerify(exactly = 1) { workoutsRepository.insert(any()) }
        coVerify(exactly = 0) { workoutLiftsRepository.insert(any()) }
        coVerify(exactly = 0) { setsRepository.insert(any()) }
    }

    @Test
    fun `throws when encountering an unknown custom set type`() = runTest {
        // Unknown set — interface mock is enough to miss all concrete branches
        val badSet = mockk<GenericLiftSet>(relaxed = true)
        val custom = customLift(
            liftId = 5L, liftName = "Custom",
            position = 0, setCount = 1,
            sets = listOf(badSet)
        )
        val workout = Workout(programId = 1L, name = "W", position = 0, lifts = listOf(custom))
        val program = Program(name = "P", isActive = false, deloadWeek = 0).copy(workouts = listOf(workout))

        coEvery { programsRepository.insert(any()) } returns 10L
        coEvery { workoutsRepository.insert(any()) } returns 20L
        coEvery { workoutLiftsRepository.insert(any()) } returns 30L

        assertThrows<Exception> {
            useCase(programsRepository, workoutsRepository, workoutLiftsRepository, setsRepository, program)
        }

        coVerify(exactly = 1) { programsRepository.insert(any()) }
        coVerify(exactly = 1) { workoutsRepository.insert(any()) }
        coVerify(exactly = 1) { workoutLiftsRepository.insert(any()) }
        // Should fail before inserting any sets
        coVerify(exactly = 0) { setsRepository.insert(any()) }
    }

    @Test
    fun `empty program clones only the program row (no workouts, lifts, or sets)`() = runTest {
        val empty = Program(name = "Empty", isActive = false).copy(workouts = emptyList())

        coEvery { programsRepository.insert(any()) } returns 5L

        useCase(programsRepository, workoutsRepository, workoutLiftsRepository, setsRepository, empty)

        coVerify(exactly = 1) { programsRepository.insert(any()) }
        coVerify(exactly = 0) { workoutsRepository.insert(any()) }
        coVerify(exactly = 0) { workoutLiftsRepository.insert(any()) }
        coVerify(exactly = 0) { setsRepository.insert(any()) }
    }

    // ---------- helpers ----------

    private fun standardLift(
        liftId: Long,
        liftName: String,
        position: Int,
        setCount: Int,
        rpe: Float,
        repLo: Int,
        repHi: Int,
        progression: ProgressionScheme,
        stepSize: Int?,
        incrementOverride: Float? = null,
        restTime: Duration? = null,
        restTimerEnabled: Boolean = false,
        deloadWeek: Int? = null,
        note: String? = null
    ) = StandardWorkoutLift(
        id = 0L,
        workoutId = 0L,
        liftId = liftId,
        liftName = liftName,
        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
        liftVolumeTypes = 0,
        liftSecondaryVolumeTypes = null,
        position = position,
        setCount = setCount,
        progressionScheme = progression,
        incrementOverride = incrementOverride,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        deloadWeek = deloadWeek,
        liftNote = note,
        rpeTarget = rpe,
        repRangeBottom = repLo,
        repRangeTop = repHi,
        stepSize = stepSize
    )

    private fun customLift(
        liftId: Long,
        liftName: String,
        position: Int,
        setCount: Int,
        sets: List<GenericLiftSet>,
        incrementOverride: Float? = null,
        restTime: Duration? = null,
        restTimerEnabled: Boolean = false,
        deloadWeek: Int? = null,
        note: String? = null
    ) = CustomWorkoutLift(
        id = 0L,
        workoutId = 0L,
        liftId = liftId,
        liftName = liftName,
        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
        liftVolumeTypes = 0,
        liftSecondaryVolumeTypes = null,
        position = position,
        setCount = setCount,
        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
        incrementOverride = incrementOverride,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        deloadWeek = deloadWeek,
        liftNote = note,
        customLiftSets = sets
    )
}
