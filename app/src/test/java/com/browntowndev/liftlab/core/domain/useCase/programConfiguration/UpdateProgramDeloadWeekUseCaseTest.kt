package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

// --- Explicit Jupiter assertions (no wildcards) ---
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.extensions.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateProgramDeloadWeekUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateProgramDeloadWeekUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)

        // Preferred TransactionScope mocking style
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = UpdateProgramDeloadWeekUseCase(
            programsRepository = programsRepository,
            workoutLiftsRepository = workoutLiftsRepository,
            transactionScope = transactionScope
        )

        // Static-mock the file class that holds the extension
        // Change the string if your extension is declared in a different file.
        mockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutExtensionsKt")
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `enabling lift-specific deload uses null override and updates lifts when non-empty`() = runTest {
        val workouts: List<Workout> = mockk(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { id } returns 42L
            every { this@mockk.workouts } returns workouts
        }

        // Build a non-empty recalculation result
        val l1 = mockk<StandardWorkoutLift>(relaxed = true)
        val l2 = mockk<StandardWorkoutLift>(relaxed = true)
        val recalculated = mapOf(100L to l1, 200L to l2)

        // Stub the static extension: receiver passed as first arg
        every {
            workouts.getAllLiftsWithRecalculatedStepSize(
                deloadToUseInsteadOfLiftLevel = null
            )
        } returns recalculated

        coEvery { programsRepository.updateDeloadWeek(42L, 3) } just Runs
        val updatedSlot: CapturingSlot<List<StandardWorkoutLift>> = slot()
        coEvery { workoutLiftsRepository.updateMany(capture(updatedSlot)) } just Runs

        useCase(program = program, deloadWeek = 3, useLiftSpecificDeload = true)

        // Transaction ran
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }

        // Always update program.deloadWeek to requested value
        coVerify(exactly = 1) { programsRepository.updateDeloadWeek(42L, 3) }

        // Extension called with NULL override when enabling lift-specific deload
        verify(exactly = 1) {
            workouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = null)
        }

        // Non-empty result -> updateMany with the values
        coVerify(exactly = 1) { workoutLiftsRepository.updateMany(any()) }
        assertEquals(setOf(l1, l2), updatedSlot.captured.toSet())
    }

    @Test
    fun `disabling lift-specific deload uses program deload override and updates lifts when non-empty`() = runTest {
        val workouts: List<Workout> = mockk(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { id } returns 7L
            every { this@mockk.workouts } returns workouts
        }

        val l1 = mockk<StandardWorkoutLift>(relaxed = true)
        val recalculated = mapOf(10L to l1)

        every {
            workouts.getAllLiftsWithRecalculatedStepSize(
                deloadToUseInsteadOfLiftLevel = 5
            )
        } returns recalculated

        coEvery { programsRepository.updateDeloadWeek(7L, 5) } just Runs
        val updated: CapturingSlot<List<StandardWorkoutLift>> = slot()
        coEvery { workoutLiftsRepository.updateMany(capture(updated)) } just Runs

        useCase(program = program, deloadWeek = 5, useLiftSpecificDeload = false)

        coVerify(exactly = 1) { programsRepository.updateDeloadWeek(7L, 5) }
        verify(exactly = 1) {
            workouts.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel = 5)
        }
        coVerify(exactly = 1) { workoutLiftsRepository.updateMany(any()) }
        assertEquals(listOf(l1), updated.captured)
    }

    @Test
    fun `empty recalculation map skips updateMany but still updates deloadWeek`() = runTest {
        val workouts: List<Workout> = mockk(relaxed = true)
        val program = mockk<Program>(relaxed = true) {
            every { id } returns 99L
            every { this@mockk.workouts } returns workouts
        }

        every {
            workouts.getAllLiftsWithRecalculatedStepSize(
                deloadToUseInsteadOfLiftLevel = null
            )
        } returns emptyMap()

        coEvery { programsRepository.updateDeloadWeek(99L, 8) } just Runs

        useCase(program = program, deloadWeek = 8, useLiftSpecificDeload = true)

        coVerify(exactly = 1) { programsRepository.updateDeloadWeek(99L, 8) }
        coVerify(exactly = 0) { workoutLiftsRepository.updateMany(any()) }
        // Transaction executed once
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
    }
}
