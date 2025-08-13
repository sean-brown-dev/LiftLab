package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CloneProgramUseCaseTest {
    @MockK lateinit var programsRepository: ProgramsRepository

    private lateinit var useCase: CloneProgramUseCase

    // Minimal concrete set for the "unknown set type" scenario
    private data class TestSet(
        override val id: Long,
        override val workoutLiftId: Long,
        override val position: Int,
        override val rpeTarget: Float = 8f,
        override val repRangeBottom: Int = 5,
        override val repRangeTop: Int = 8,
    ) : GenericLiftSet

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        useCase = CloneProgramUseCase(programsRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    @DisplayName("Clones program: inserts a new Program and applies a delta that recreates all workouts/lifts under the new id")
    fun clonesProgram_buildsDelta_andDelegates() = runTest {
        // Given a program graph with non-zero ids
        val w1 = Workout(
            id = 10L,
            programId = 7L,
            name = "W1",
            position = 1,
            lifts = listOf(
                customLift(
                    id = 100L,
                    workoutId = 10L,
                    liftId = 501L,
                    position = 1,
                    setCount = 0
                )
            )
        )
        val w2 = Workout(
            id = 11L,
            programId = 7L,
            name = "W2",
            position = 2,
            lifts = listOf(
                customLift(
                    id = 101L,
                    workoutId = 11L,
                    liftId = 502L,
                    position = 1,
                    setCount = 0
                )
            )
        )
        val program = Program(
            id = 99L,
            name = "P",
            isActive = false,
            deloadWeek = 4,
            currentMicrocyclePosition = 0,
            workouts = listOf(w1, w2)
        )

        // Insert returns the id of the new program
        coEvery { programsRepository.insert(any()) } returns 123L

        // Capture the delta applied after insert
        val captured = slot<ProgramDelta>()
        coEvery { programsRepository.applyDelta(eq(123L), capture(captured)) } just Runs

        // When
        val newId = useCase(program)
        assertEquals(123L, newId)

        // Then: repository gets a delta that contains two workout inserts addressed to the new program id
        coVerify(exactly = 1) { programsRepository.insert(any()) }
        coVerify(exactly = 1) { programsRepository.applyDelta(eq(123L), any()) }

        val delta = captured.captured
        assertEquals(2, delta.workouts.size, "Should recreate all workouts")
        assertTrue(delta.workouts.all { it.workoutInsert != null }, "Each change should insert a workout")
        assertTrue(delta.workouts.all { it.workoutInsert?.programId == 123L }, "Inserted workouts must target the new program id")

        // Lifts: ensure we attempt to recreate lifts too (counts should match per workout)
        delta.workouts.forEach { wc ->
            val source = program.workouts.single { it.name == wc.workoutInsert?.name }
            assertEquals(source.lifts.size, wc.lifts.size)
        }
    }

    @Test
    @DisplayName("Throws when encountering an unknown workout-lift type")
    fun throwsOnUnknownWorkoutLiftType() = runTest {
        // A fake lift that is NOT one of the handled types (StandardWorkoutLift / CustomWorkoutLift)
        val unknownLift = object : GenericWorkoutLift {
            override val id: Long = 42L
            override val workoutId: Long = 9L
            override val liftId: Long = 999L
            override val liftName: String = "X"
            override val liftMovementPattern: MovementPattern = MovementPattern.FOREARM_ISO
            override val liftVolumeTypes: Int = 0
            override val liftSecondaryVolumeTypes: Int? = null
            override val position: Int = 1
            override val setCount: Int = 0
            override val progressionScheme: ProgressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION
            override val deloadWeek: Int? = null
            override val incrementOverride: Float? = null
            override val restTime: kotlin.time.Duration? = null
            override val restTimerEnabled: Boolean = false
            override val liftNote: String? = null
        }

        val program = Program(
            id = 1L, name = "P",
            workouts = listOf(
                Workout(id = 1L, programId = 1L, name = "W", position = 1, lifts = listOf(unknownLift))
            )
        )

        assertThrows(Exception::class.java) {
            runTest {
                useCase(program)
            }
        }
        // Ensure we did not delegate to insert when error
        coVerify(exactly = 0) { programsRepository.insert(any()) }
    }

    @Test
    @DisplayName("Throws when encountering an unknown set type within a CustomWorkoutLift")
    fun throwsOnUnknownSetTypeInsideCustomLift() = runTest {
        val badSet = TestSet(id = 5L, workoutLiftId = 100L, position = 1)
        val custom = customLift(
            id = 100L,
            workoutId = 10L,
            liftId = 501L,
            position = 1,
            setCount = 1,
            sets = listOf(badSet) // <- not StandardSet/DropSet/MyoRepSet
        )
        val program = Program(
            id = 1L, name = "P",
            workouts = listOf(
                Workout(id = 10L, programId = 1L, name = "W", position = 1, lifts = listOf(custom))
            )
        )

        assertThrows(Exception::class.java) {
            runTest {
                useCase(program)
            }
        }
        coVerify(exactly = 0) { programsRepository.insert(any()) }
    }

    // -------- helpers --------

    private fun customLift(
        id: Long,
        workoutId: Long,
        liftId: Long,
        position: Int,
        setCount: Int,
        sets: List<GenericLiftSet> = emptyList(),
    ): CustomWorkoutLift {
        return CustomWorkoutLift(
            id = id,
            workoutId = workoutId,
            liftId = liftId,
            liftName = "L$liftId",
            liftMovementPattern = MovementPattern.FOREARM_ISO,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = position,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            deloadWeek = null,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            liftNote = null,
            customLiftSets = sets
        )
    }
}