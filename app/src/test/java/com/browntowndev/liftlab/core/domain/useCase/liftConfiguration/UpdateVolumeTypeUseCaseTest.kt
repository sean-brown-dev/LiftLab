package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
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
import org.junit.jupiter.api.assertThrows

class UpdateVolumeTypeUseCaseTest {

    private lateinit var liftsRepository: LiftsRepository
    private lateinit var updateVolumeTypeUseCase: UpdateVolumeTypeUseCase

    // --- Helper Method ---
    private fun createTestLift(
        id: Long = 1L,
        name: String = "Test Lift",
        movementPattern: MovementPattern = MovementPattern.HORIZONTAL_PUSH,
        volumeTypesBitmask: Int = VolumeType.CHEST.bitMask,
        secondaryVolumeTypesBitmask: Int? = VolumeType.TRICEP.bitMask
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
        liftsRepository = mockk()
        updateVolumeTypeUseCase = UpdateVolumeTypeUseCase(liftsRepository)
    }

    @Test
    fun `invoke with PRIMARY category should update primary bitmask and call repository`() =
        runTest {
            // GIVEN
            val initialLift = createTestLift(
                volumeTypesBitmask = VolumeType.CHEST.bitMask,
                secondaryVolumeTypesBitmask = VolumeType.TRICEP.bitMask
            )
            val newPrimaryBitmask = VolumeType.CHEST.bitMask or VolumeType.ANTERIOR_DELTOID.bitMask
            val liftSlot = slot<Lift>()
            coEvery { liftsRepository.update(capture(liftSlot)) } returns Unit

            // WHEN
            updateVolumeTypeUseCase(initialLift, newPrimaryBitmask, VolumeTypeCategory.PRIMARY)

            // THEN
            coVerify(exactly = 1) { liftsRepository.update(any()) }

            val capturedLift = liftSlot.captured
            assertEquals(newPrimaryBitmask, capturedLift.volumeTypesBitmask)
            assertEquals(
                initialLift.secondaryVolumeTypesBitmask,
                capturedLift.secondaryVolumeTypesBitmask
            ) // Should be unchanged
            assertEquals(initialLift.id, capturedLift.id) // Other properties should be unchanged
        }

    @Test
    fun `invoke with SECONDARY category should update secondary bitmask and call repository`() =
        runTest {
            // GIVEN
            val initialLift = createTestLift(
                volumeTypesBitmask = VolumeType.CHEST.bitMask,
                secondaryVolumeTypesBitmask = VolumeType.TRICEP.bitMask
            )
            val newSecondaryBitmask = VolumeType.TRICEP.bitMask or VolumeType.BICEP.bitMask
            val liftSlot = slot<Lift>()
            coEvery { liftsRepository.update(capture(liftSlot)) } returns Unit

            // WHEN
            updateVolumeTypeUseCase(initialLift, newSecondaryBitmask, VolumeTypeCategory.SECONDARY)

            // THEN
            coVerify(exactly = 1) { liftsRepository.update(any()) }

            val capturedLift = liftSlot.captured
            assertEquals(newSecondaryBitmask, capturedLift.secondaryVolumeTypesBitmask)
            assertEquals(
                initialLift.volumeTypesBitmask,
                capturedLift.volumeTypesBitmask
            ) // Should be unchanged
            assertEquals(initialLift.id, capturedLift.id)
        }

    @Test
    fun `invoke with SECONDARY category and null bitmask should update secondary to null`() =
        runTest {
            // GIVEN
            val initialLift =
                createTestLift(secondaryVolumeTypesBitmask = VolumeType.TRICEP.bitMask)
            val liftSlot = slot<Lift>()
            coEvery { liftsRepository.update(capture(liftSlot)) } returns Unit

            // WHEN
            updateVolumeTypeUseCase(initialLift, null, VolumeTypeCategory.SECONDARY)

            // THEN
            coVerify(exactly = 1) { liftsRepository.update(any()) }

            val capturedLift = liftSlot.captured
            assertNull(capturedLift.secondaryVolumeTypesBitmask)
            assertEquals(
                initialLift.volumeTypesBitmask,
                capturedLift.volumeTypesBitmask
            ) // Should be unchanged
        }

    @Test
    fun `invoke with PRIMARY category and null bitmask should throw IllegalArgumentException`() =
        runTest {
            // GIVEN
            val initialLift = createTestLift()

            // WHEN & THEN
            val exception = assertThrows<IllegalArgumentException> {
                updateVolumeTypeUseCase(initialLift, null, VolumeTypeCategory.PRIMARY)
            }
            assertEquals("Primary volume type cannot be null", exception.message)

            // Verify that the repository was never called because the exception was thrown first
            coVerify(exactly = 0) { liftsRepository.update(any()) }
        }
}