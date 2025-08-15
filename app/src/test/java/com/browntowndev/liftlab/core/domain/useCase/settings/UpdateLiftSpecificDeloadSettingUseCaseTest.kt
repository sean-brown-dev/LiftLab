package com.browntowndev.liftlab.core.domain.useCase.settings

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.extensions.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.common.SettingKey
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class UpdateLiftSpecificDeloadSettingUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: UpdateLiftSpecificDeloadSettingUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        useCase = UpdateLiftSpecificDeloadSettingUseCase(
            programsRepository = programsRepository,
            settingsRepository = settingsRepository
        )

        // Static-mock the top-level extension function
        mockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutExtensionsKt")
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `enabling lift-specific deload uses null override, builds delta with lifts in order, and disables deload prompt`() = runTest {
        val workouts: List<Workout> = mockk(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { id } returns 1L
            every { this@mockk.workouts } returns workouts
            every { deloadWeek } returns 4 // ignored when enabling
        }

        // Two lifts under one workout; order (B, A) must be preserved in delta
        val workoutId = 10L
        val liftA = buildStandardWorkoutLift(id = 1L, workoutId = workoutId, liftId = 100L, position = 0)
        val liftB = buildStandardWorkoutLift(id = 2L, workoutId = workoutId, liftId = 200L, position = 1)

        every { workouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = null) } returns
                mapOf(workoutId to listOf(liftB, liftA))

        val deltaSlot: CapturingSlot<com.browntowndev.liftlab.core.domain.delta.ProgramDelta> = slot()
        coEvery { programsRepository.applyDelta(programId = 1L, delta = capture(deltaSlot)) } returns Unit
        coEvery { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, true) } returns Unit
        coEvery { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) } returns Unit

        useCase(program = program, useLiftSpecificDeload = true)

        // Repository invocation and delta inspection
        coVerify(exactly = 1) { programsRepository.applyDelta(1L, any()) }
        val delta = deltaSlot.captured
        assertEquals(1, delta.workouts.size)
        val workoutChange = delta.workouts.first()
        assertEquals(workoutId, workoutChange.workoutId)
        assertEquals(listOf(liftB.id, liftA.id), workoutChange.lifts.map { it.workoutLiftId })

        // Settings
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, true) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) }
    }

    @Test
    fun `disabling lift-specific deload uses program deload override and builds grouped delta per workout`() = runTest {
        val workouts: List<Workout> = mockk(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { id } returns 7L
            every { this@mockk.workouts } returns workouts
            every { deloadWeek } returns 3
        }

        val workoutIdA = 20L
        val workoutIdB = 30L
        val liftC = buildStandardWorkoutLift(id = 11L, workoutId = workoutIdA, liftId = 110L, position = 0)
        val liftD = buildStandardWorkoutLift(id = 12L, workoutId = workoutIdA, liftId = 120L, position = 1)
        val liftE = buildStandardWorkoutLift(id = 21L, workoutId = workoutIdB, liftId = 210L, position = 0)

        // Use linkedMapOf to lock in map iteration order (A then B)
        every { workouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = 3) } returns
                linkedMapOf(workoutIdA to listOf(liftC, liftD), workoutIdB to listOf(liftE))

        val deltaSlot: CapturingSlot<com.browntowndev.liftlab.core.domain.delta.ProgramDelta> = slot()
        coEvery { programsRepository.applyDelta(programId = 7L, delta = capture(deltaSlot)) } returns Unit
        coEvery { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, false) } returns Unit

        useCase(program = program, useLiftSpecificDeload = false)

        coVerify(exactly = 1) { programsRepository.applyDelta(7L, any()) }
        val delta = deltaSlot.captured
        assertEquals(2, delta.workouts.size)

        val changeA = delta.workouts.first { it.workoutId == workoutIdA }
        assertEquals(listOf(liftC.id, liftD.id), changeA.lifts.map { it.workoutLiftId })

        val changeB = delta.workouts.first { it.workoutId == workoutIdB }
        assertEquals(listOf(liftE.id), changeB.lifts.map { it.workoutLiftId })

        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, false) }
        // This use case does not touch PromptForDeloadWeek when disabling
        coVerify(exactly = 0) { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, any()) }
    }

    @Test
    fun `when recalculation returns empty map, still writes setting but skips applyDelta`() = runTest {
        val workouts: List<Workout> = mockk(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { id } returns 9L
            every { this@mockk.workouts } returns workouts
            every { deloadWeek } returns 5
        }

        // Enable path -> empty result
        every { workouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = null) } returns emptyMap()
        coEvery { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, true) } returns Unit
        coEvery { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) } returns Unit

        useCase(program = program, useLiftSpecificDeload = true)

        coVerify(exactly = 0) { programsRepository.applyDelta(any(), any()) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, true) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) }

        // Disable path -> empty result
        every { workouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = 5) } returns emptyMap()
        coEvery { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, false) } returns Unit

        useCase(program = program, useLiftSpecificDeload = false)

        coVerify(exactly = 0) { programsRepository.applyDelta(any(), any()) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, false) }
        // PromptForDeloadWeek remains as set above (only affected during enabling path)
    }

    // -------- helpers --------

    private fun buildStandardWorkoutLift(
        id: Long,
        workoutId: Long,
        liftId: Long,
        position: Int,
    ) = StandardWorkoutLift(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftName = "Lift-$liftId",
        liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
        liftVolumeTypes = 0,
        liftSecondaryVolumeTypes = null,
        position = position,
        setCount = 3,
        progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
        incrementOverride = null,
        restTime = null as Duration?,
        restTimerEnabled = false,
        deloadWeek = null,
        liftNote = null,
        rpeTarget = 8f,
        repRangeBottom = 8,
        repRangeTop = 10,
        stepSize = null
    )
}
