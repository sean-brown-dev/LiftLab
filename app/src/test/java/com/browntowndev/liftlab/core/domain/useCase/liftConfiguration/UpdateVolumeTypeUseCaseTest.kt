package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateVolumeTypeUseCaseTest {

    private lateinit var liftsRepository: LiftsRepository
    private lateinit var updateVolumeTypeUseCase: UpdateVolumeTypeUseCase
    private lateinit var transactionScope: TransactionScope

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
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.executeWithResult(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        updateVolumeTypeUseCase = UpdateVolumeTypeUseCase(liftsRepository, transactionScope)
    }

    @Test
    fun `invoke with PRIMARY category should update primary bitmask and call repository`() =
        runTest {
            // GIVEN
            val initialLift = createTestLift(
                volumeTypesBitmask = VolumeType.CHEST.bitMask,
                secondaryVolumeTypesBitmask = VolumeType.TRICEP.bitMask
            )
            val liftSlot = slot<Lift>()
            coEvery { liftsRepository.update(capture(liftSlot)) } returns Unit

            // WHEN
            updateVolumeTypeUseCase(initialLift, 0, VolumeType.ANTERIOR_DELTOID, VolumeTypeCategory.PRIMARY)

            // THEN
            coVerify(exactly = 1) { liftsRepository.update(any()) }

            val capturedLift = liftSlot.captured
            assertEquals(VolumeType.ANTERIOR_DELTOID.bitMask, capturedLift.volumeTypesBitmask)
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
            val liftSlot = slot<Lift>()
            coEvery { liftsRepository.update(capture(liftSlot)) } returns Unit

            // WHEN
            updateVolumeTypeUseCase(initialLift, 0, VolumeType.BICEP, VolumeTypeCategory.SECONDARY)

            // THEN
            coVerify(exactly = 1) { liftsRepository.update(any()) }

            val capturedLift = liftSlot.captured
            assertEquals(VolumeType.BICEP.bitMask, capturedLift.secondaryVolumeTypesBitmask)
            assertEquals(
                initialLift.volumeTypesBitmask,
                capturedLift.volumeTypesBitmask
            ) // Should be unchanged
            assertEquals(initialLift.id, capturedLift.id)
        }
}