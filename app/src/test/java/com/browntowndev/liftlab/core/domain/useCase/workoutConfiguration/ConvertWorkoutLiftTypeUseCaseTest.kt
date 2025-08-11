package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.extensions.convertToCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.extensions.convertToStandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ConvertWorkoutLiftTypeUseCaseTest {

    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var customLiftSetsRepository: CustomLiftSetsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: ConvertWorkoutLiftTypeUseCase

    @BeforeEach
    fun setUp() {
        workoutLiftsRepository = mockk(relaxed = true)
        customLiftSetsRepository = mockk(relaxed = true)

        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = ConvertWorkoutLiftTypeUseCase(
            workoutLiftsRepository = workoutLiftsRepository,
            customLiftSetsRepository = customLiftSetsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `enableCustomSets true converts Standard - updates lift and inserts custom sets`() = runTest {
        // Given a Standard workout lift to convert
        val standard = StandardWorkoutLift(
            id = 123L,
            workoutId = 1L,
            liftId = 2L,
            liftName = "Bench",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = 0,
            setCount = 3,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            deloadWeek = null,
            liftNote = null,
            rpeTarget = 8f,
            repRangeBottom = 5,
            repRangeTop = 8,
            stepSize = null
        )

        val custom = standard.convertToCustomWorkoutLift()

        // When
        useCase(workoutLiftToConvert = standard, enableCustomSets = true)

        // Then
        coVerify(exactly = 1) { workoutLiftsRepository.update(custom) }
        coVerify(exactly = 1) { customLiftSetsRepository.insertMany(custom.customLiftSets) }
        coVerify(exactly = 0) { customLiftSetsRepository.deleteAllForLift(any()) }
    }

    @Test
    fun `enableCustomSets true throws if already Custom - no repo calls`() = runTest {
        val alreadyCustom = mockk<CustomWorkoutLift>(relaxed = true) {
            every { id } returns 777L
        }

        val ex = assertThrows<Exception> {
            useCase(workoutLiftToConvert = alreadyCustom, enableCustomSets = true)
        }
        assertEquals("Lift already has custom lift sets.", ex.message)

        // No conversion attempted, no repo calls
        verify(exactly = 0) { alreadyCustom.convertToStandardWorkoutLift() }
        coVerify(exactly = 0) { workoutLiftsRepository.update(any()) }
        coVerify(exactly = 0) { customLiftSetsRepository.insertMany(any()) }
        coVerify(exactly = 0) { customLiftSetsRepository.deleteAllForLift(any()) }
    }

    @Test
    fun `enableCustomSets false converts Custom - updates lift and deletes custom sets`() = runTest {
        // Given a Custom workout lift to convert back to Standard
        val custom = CustomWorkoutLift(
            id = 42L,
            workoutId = 1L,
            liftId = 2L,
            liftName = "Test Lift",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = 0,
            setCount = 0,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            deloadWeek = null,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            liftNote = "",
            customLiftSets = emptyList(),
        )

        // When
        useCase(workoutLiftToConvert = custom, enableCustomSets = false)

        // Then
        coVerify(exactly = 1) { workoutLiftsRepository.update(custom.convertToStandardWorkoutLift()) }
        coVerify(exactly = 1) { customLiftSetsRepository.deleteAllForLift(42L) }
        coVerify(exactly = 0) { customLiftSetsRepository.insertMany(any()) }
    }

    @Test
    fun `enableCustomSets false throws if given Standard - no repo calls`() = runTest {
        val standard = mockk<StandardWorkoutLift>(relaxed = true) {
            every { id } returns 99L
        }

        val ex = assertThrows<Exception> {
            useCase(workoutLiftToConvert = standard, enableCustomSets = false)

        }
        assertEquals("Lift does not have custom lift sets to remove.", ex.message)

        // No conversion attempted, no repo calls
        verify(exactly = 0) { standard.convertToCustomWorkoutLift() }
        coVerify(exactly = 0) { workoutLiftsRepository.update(any()) }
        coVerify(exactly = 0) { customLiftSetsRepository.insertMany(any()) }
        coVerify(exactly = 0) { customLiftSetsRepository.deleteAllForLift(any()) }
    }

    @Test
    fun `work is executed within provided TransactionScope`() = runTest {
        // Count invocations to ensure exactly one transaction execution
        var executed = 0
        val countingScope: TransactionScope = mockk(relaxed = true)
        coEvery { countingScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
            executed++
        }

        val localUseCase = ConvertWorkoutLiftTypeUseCase(
            workoutLiftsRepository = workoutLiftsRepository,
            customLiftSetsRepository = customLiftSetsRepository,
            transactionScope = countingScope
        )
        val standard = StandardWorkoutLift(
            id = 123L,
            workoutId = 1L,
            liftId = 2L,
            liftName = "Bench",
            liftMovementPattern = MovementPattern.HORIZONTAL_PUSH,
            liftVolumeTypes = 0,
            liftSecondaryVolumeTypes = null,
            position = 0,
            setCount = 3,
            progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            incrementOverride = null,
            restTime = null,
            restTimerEnabled = false,
            deloadWeek = null,
            liftNote = null,
            rpeTarget = 8f,
            repRangeBottom = 5,
            repRangeTop = 8,
            stepSize = null
        )

        localUseCase(workoutLiftToConvert = standard, enableCustomSets = true)
        testScheduler.advanceUntilIdle()

        assertEquals(1, executed, "TransactionScope.execute should be called exactly once")
    }
}
