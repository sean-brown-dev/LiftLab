package com.browntowndev.liftlab.core.domain.useCase.settings

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.extensions.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.SettingKey
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateLiftSpecificDeloadSettingUseCaseTest {

    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: UpdateLiftSpecificDeloadSettingUseCase

    @BeforeEach
    fun setUp() {
        workoutLiftsRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        useCase = UpdateLiftSpecificDeloadSettingUseCase(
            workoutLiftsRepository = workoutLiftsRepository,
            settingsRepository = settingsRepository
        )

        // Static-mock the file class that contains the extension
        // (update the string if your extension lives in a differently named file).
        mockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutExtensionsKt")
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `enabling lift-specific deload uses null override, updates lifts when non-empty, and disables deload prompt`() = runTest {
        val mockWorkouts = mockk<List<Workout>>(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { workouts } returns mockWorkouts
            every { deloadWeek } returns 4 // should be ignored when enabling
        }

        // Build a deterministic non-empty result
        val w1 = stdLift(id = 1L, workoutId = 10L, liftId = 100L, position = 0)
        val w2 = stdLift(id = 2L, workoutId = 10L, liftId = 200L, position = 1)
        val recalculated = mapOf(100L to w1, 200L to w2)

        // Expect: when enabling, extension is called with null override
        every {
            mockWorkouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = null)
        } returns recalculated

        val updatedSlot: CapturingSlot<List<StandardWorkoutLift>> = slot()
        coEvery { workoutLiftsRepository.updateMany(capture(updatedSlot)) } returns Unit
        coEvery { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, true) } returns Unit
        coEvery { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) } returns Unit

        useCase(program = program, useLiftSpecificDeload = true)

        // Repo interactions
        coVerify(exactly = 1) { workoutLiftsRepository.updateMany(any()) }
        assertEquals(2, updatedSlot.captured.size)
        assertTrue(updatedSlot.captured.containsAll(listOf(w1, w2)))

        // Settings writes
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, true) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) }
    }

    @Test
    fun `disabling lift-specific deload uses program deload override and updates lifts when non-empty`() = runTest {
        val mockWorkouts = mockk<List<Workout>>(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { workouts } returns mockWorkouts
            every { deloadWeek } returns 3 // MUST be used when disabling lift-specific
        }

        val w1 = stdLift(id = 11L, workoutId = 20L, liftId = 110L, position = 0)
        val recalculated = mapOf(110L to w1)

        every {
            mockWorkouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = 3)
        } returns recalculated

        val updatedSlot: CapturingSlot<List<StandardWorkoutLift>> = slot()
        coEvery { workoutLiftsRepository.updateMany(capture(updatedSlot)) } returns Unit
        coEvery { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, false) } returns Unit

        useCase(program = program, useLiftSpecificDeload = false)

        coVerify(exactly = 1) { workoutLiftsRepository.updateMany(any()) }
        assertEquals(1, updatedSlot.captured.size)
        assertTrue(updatedSlot.captured.contains(w1))

        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, false) }
        // When disabling, the use case does NOT touch PromptForDeloadWeek
        coVerify(exactly = 0) { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, any()) }
    }

    @Test
    fun `when recalculation returns empty map, it still writes LiftSpecificDeload but skips updateMany`() = runTest {
        val mockWorkouts = mockk<List<Workout>>(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { workouts } returns mockWorkouts
            every { deloadWeek } returns 5
        }

        // Case A: enabling (null override) with empty result
        every { mockWorkouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = null) } returns emptyMap()
        coEvery { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, true) } returns Unit
        coEvery { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) } returns Unit

        useCase(program = program, useLiftSpecificDeload = true)

        coVerify(exactly = 0) { workoutLiftsRepository.updateMany(any()) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, true) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) }

        // Case B: disabling (program deload override) with empty result
        every { mockWorkouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = 5) } returns emptyMap()
        coEvery { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, false) } returns Unit

        useCase(program = program, useLiftSpecificDeload = false)

        coVerify(exactly = 0) { workoutLiftsRepository.updateMany(any()) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.LiftSpecificDeload, false) }
        coVerify(exactly = 1) { settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false) } // from Case A only
    }

    // -------- helpers --------

    private fun stdLift(
        id: Long,
        workoutId: Long,
        liftId: Long,
        position: Int
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
