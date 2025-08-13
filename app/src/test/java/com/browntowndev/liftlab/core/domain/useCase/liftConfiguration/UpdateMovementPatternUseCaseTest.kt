package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeUtils
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateMovementPatternUseCaseTest {

    private lateinit var liftRepository: LiftsRepository
    private lateinit var updateMovementPatternUseCase: UpdateMovementPatternUseCase
    private lateinit var transactionScope: TransactionScope

    private fun createTestLift(
        id: Long,
        name: String,
        movementPattern: MovementPattern,
        volumeTypesBitmask: Int,
        secondaryVolumeTypesBitmask: Int?
    ) = Lift(
        id = id,
        name = name,
        movementPattern = movementPattern,
        volumeTypesBitmask = volumeTypesBitmask,
        secondaryVolumeTypesBitmask = secondaryVolumeTypesBitmask,
        isBodyweight = false,
        restTimerEnabled = true,
        note = null,
        incrementOverride = null,
        restTime = null
    )

    @BeforeEach
    fun setUp() {
        // Mock the repository dependency
        liftRepository = mockk()
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        updateMovementPatternUseCase = UpdateMovementPatternUseCase(liftRepository, transactionScope)
    }

    @Test
    fun `invoke should update lift with new pattern and calculated primary and secondary bitmasks`() =
        runTest {
            // GIVEN
            // 1. An initial lift with an old movement pattern and bitmasks
            val initialLift = createTestLift(
                id = 1L,
                name = "Bench Press",
                movementPattern = MovementPattern.AB_ISO, // Old pattern
                volumeTypesBitmask = VolumeType.AB.bitMask,
                secondaryVolumeTypesBitmask = null
            )

            // 2. The new movement pattern we want to apply
            val newMovementPattern = MovementPattern.HORIZONTAL_PUSH

            // 3. Mock the repository's update function to do nothing and capture the argument
            val liftSlot = slot<Lift>()
            coEvery { liftRepository.update(capture(liftSlot)) } returns Unit

            // 4. Manually calculate the expected bitmasks for verification
            // This relies on the known logic within VolumeTypeUtils
            val expectedPrimaryBitmask = VolumeTypeUtils.getDefaultVolumeTypes(newMovementPattern)
                .sumOf { it.bitMask }
            val expectedSecondaryBitmask =
                VolumeTypeUtils.getDefaultSecondaryVolumeTypes(newMovementPattern)
                    ?.sumOf { it.bitMask }

            // WHEN
            // Execute the use case
            updateMovementPatternUseCase(initialLift, newMovementPattern)

            // THEN
            // 1. Verify the repository's update method was called exactly once
            coVerify(exactly = 1) { liftRepository.update(any()) }

            // 2. Get the captured Lift object that was passed to the repository
            val capturedLift = liftSlot.captured

            // 3. Assert that the captured lift has the correct, updated properties
            assertEquals(newMovementPattern, capturedLift.movementPattern)
            assertEquals(expectedPrimaryBitmask, capturedLift.volumeTypesBitmask)
            assertEquals(expectedSecondaryBitmask, capturedLift.secondaryVolumeTypesBitmask)

            // 4. Assert that other properties of the lift remain unchanged
            assertEquals(initialLift.id, capturedLift.id)
            assertEquals(initialLift.name, capturedLift.name)
        }

    @Test
    fun `invoke should update lift and set secondary bitmask to null when new pattern has no secondary types`() =
        runTest {
            // GIVEN
            // 1. An initial lift that currently has a secondary bitmask
            val initialLift = createTestLift(
                id = 2L,
                name = "Squat",
                movementPattern = MovementPattern.LEG_PUSH, // Old pattern
                volumeTypesBitmask = 1,
                secondaryVolumeTypesBitmask = 2 // This should be nulled out
            )

            // 2. A new movement pattern that has no secondary volume types
            val newMovementPattern = MovementPattern.AB_ISO

            // 3. Mock the repository and capture the argument
            val liftSlot = slot<Lift>()
            coEvery { liftRepository.update(capture(liftSlot)) } returns Unit

            // 4. Calculate the expected primary bitmask
            val expectedPrimaryBitmask = VolumeTypeUtils.getDefaultVolumeTypes(newMovementPattern)
                .sumOf { it.bitMask }

            // WHEN
            updateMovementPatternUseCase(initialLift, newMovementPattern)

            // THEN
            // 1. Verify the repository call
            coVerify(exactly = 1) { liftRepository.update(any()) }

            // 2. Get the captured lift
            val capturedLift = liftSlot.captured

            // 3. Assert properties are updated correctly
            assertEquals(newMovementPattern, capturedLift.movementPattern)
            assertEquals(expectedPrimaryBitmask, capturedLift.volumeTypesBitmask)

            // 4. CRITICAL: Assert that the secondary bitmask is now null
            assertNull(capturedLift.secondaryVolumeTypesBitmask)
        }
}