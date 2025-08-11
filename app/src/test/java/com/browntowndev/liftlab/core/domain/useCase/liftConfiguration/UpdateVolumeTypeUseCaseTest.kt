package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

// --- Explicit JUnit Jupiter assertions (no wildcards) ---
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.enums.toVolumeTypes
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateVolumeTypeUseCaseTest {

    private lateinit var liftsRepository: LiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateVolumeTypeUseCase

    // --- Helper to create a real Lift instance (no mocking needed) ---
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
        liftsRepository = mockk(relaxed = true)

        // FIX: value-returning signature; return the block's value
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.executeWithResult(any<suspend () -> Any?>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        useCase = UpdateVolumeTypeUseCase(liftsRepository, transactionScope)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `PRIMARY - index 0 updates primary bitmask and calls repository`() = runTest {
        // Given
        val initial = createTestLift(
            volumeTypesBitmask = VolumeType.CHEST.bitMask, // single element -> index 0 valid
            secondaryVolumeTypesBitmask = VolumeType.TRICEP.bitMask
        )
        val captured = slot<Lift>()
        coEvery { liftsRepository.update(capture(captured)) } returns Unit

        // When
        val out = useCase(initial, index = 0, newVolumeType = VolumeType.ANTERIOR_DELTOID, volumeTypeCategory = VolumeTypeCategory.PRIMARY)

        // Then
        coVerify(exactly = 1) { liftsRepository.update(any()) }
        assertEquals(VolumeType.ANTERIOR_DELTOID.bitMask, captured.captured.volumeTypesBitmask)
        assertEquals(initial.secondaryVolumeTypesBitmask, captured.captured.secondaryVolumeTypesBitmask)
        assertSame(captured.captured, out)
    }

    @Test
    fun `SECONDARY - index 0 updates secondary bitmask and calls repository (primary unchanged)`() = runTest {
        val initial = createTestLift(
            volumeTypesBitmask = VolumeType.CHEST.bitMask, // primary list has size 1 -> index 0 valid
            secondaryVolumeTypesBitmask = VolumeType.TRICEP.bitMask
        )
        val captured = slot<Lift>()
        coEvery { liftsRepository.update(capture(captured)) } returns Unit

        val out = useCase(initial, index = 0, newVolumeType = VolumeType.BICEP, volumeTypeCategory = VolumeTypeCategory.SECONDARY)

        coVerify(exactly = 1) { liftsRepository.update(any()) }
        assertEquals(VolumeType.BICEP.bitMask, captured.captured.secondaryVolumeTypesBitmask)
        assertEquals(initial.volumeTypesBitmask, captured.captured.volumeTypesBitmask)
        assertSame(captured.captured, out)
    }

    @Test
    fun `PRIMARY - index out of bounds throws with clear message and does not persist`() = runTest {
        // For deterministic size, mock the top-level toVolumeTypes() of the current primary mask
        val primaryMask = VolumeType.CHEST.bitMask
        mockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
        every { primaryMask.toVolumeTypes() } returns listOf(VolumeType.CHEST) // size = 1

        val lift = createTestLift(volumeTypesBitmask = primaryMask)
        val ex = assertThrows<IllegalArgumentException> {
            useCase(lift, index = 5, newVolumeType = VolumeType.TRICEP, volumeTypeCategory = VolumeTypeCategory.PRIMARY)
        }
        assertTrue(ex.message?.contains("out of bounds") == true)
        coVerify(exactly = 0) { liftsRepository.update(any()) }

        unmockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
    }

    @Test
    fun `SECONDARY - negative index throws (IndexOutOfBounds) and does not persist`() = runTest {
        // Negative index bypasses the >= size check and will throw when setting the list index
        val primaryMask = VolumeType.CHEST.bitMask
        mockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
        every { primaryMask.toVolumeTypes() } returns listOf(VolumeType.CHEST) // size = 1

        val lift = createTestLift(volumeTypesBitmask = primaryMask)
        assertThrows<IndexOutOfBoundsException> {
            useCase(lift, index = -1, newVolumeType = VolumeType.BICEP, volumeTypeCategory = VolumeTypeCategory.SECONDARY)
        }
        coVerify(exactly = 0) { liftsRepository.update(any()) }

        unmockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
    }

    @Test
    fun `SECONDARY - multi-element replacement uses primary-list indexing and writes the SUM bitmask to secondary`() = runTest {
        // Make ordering deterministic for the primary list by stubbing toVolumeTypes()
        val primaryMask = VolumeType.CHEST.bitMask + VolumeType.TRICEP.bitMask
        mockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
        every { primaryMask.toVolumeTypes() } returns listOf(VolumeType.CHEST, VolumeType.TRICEP)

        val initial = createTestLift(volumeTypesBitmask = primaryMask, secondaryVolumeTypesBitmask = VolumeType.CHEST.bitMask)
        val captured = slot<Lift>()
        coEvery { liftsRepository.update(capture(captured)) } returns Unit

        // Replace index 1 (TRICEP) -> BICEP; expected SUM = CHEST + BICEP
        val expectedSecondary = VolumeType.CHEST.bitMask + VolumeType.BICEP.bitMask

        val out = useCase(initial, index = 1, newVolumeType = VolumeType.BICEP, volumeTypeCategory = VolumeTypeCategory.SECONDARY)

        coVerify(exactly = 1) { liftsRepository.update(any()) }
        assertEquals(expectedSecondary, captured.captured.secondaryVolumeTypesBitmask)
        assertEquals(primaryMask, captured.captured.volumeTypesBitmask) // primary unchanged by design
        assertSame(captured.captured, out)

        unmockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
    }

    @Test
    fun `PRIMARY - multi-element replacement updates primary to SUM of updated list`() = runTest {
        val primaryMask = VolumeType.CHEST.bitMask + VolumeType.TRICEP.bitMask
        mockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
        every { primaryMask.toVolumeTypes() } returns listOf(VolumeType.CHEST, VolumeType.TRICEP)

        val initial = createTestLift(volumeTypesBitmask = primaryMask, secondaryVolumeTypesBitmask = VolumeType.BICEP.bitMask)
        val captured = slot<Lift>()
        coEvery { liftsRepository.update(capture(captured)) } returns Unit

        // Replace index 0 (CHEST) -> ANTERIOR_DELTOID; expected = AD + TRICEP
        val expectedPrimary = VolumeType.ANTERIOR_DELTOID.bitMask + VolumeType.TRICEP.bitMask

        val out = useCase(initial, index = 0, newVolumeType = VolumeType.ANTERIOR_DELTOID, volumeTypeCategory = VolumeTypeCategory.PRIMARY)

        coVerify(exactly = 1) { liftsRepository.update(any()) }
        assertEquals(expectedPrimary, captured.captured.volumeTypesBitmask)
        assertEquals(initial.secondaryVolumeTypesBitmask, captured.captured.secondaryVolumeTypesBitmask)
        assertSame(captured.captured, out)

        unmockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
    }

    @Test
    fun `does NOT persist when id is non-positive but still returns the updated copy`() = runTest {
        val initial = createTestLift(
            id = 0L, // non-persisted
            volumeTypesBitmask = VolumeType.CHEST.bitMask
        )
        // No static mocking needed: single-element primary -> index 0 valid, new mask is newVolumeType.bitMask
        val result = useCase(initial, index = 0, newVolumeType = VolumeType.BICEP, volumeTypeCategory = VolumeTypeCategory.PRIMARY)

        // Repository untouched
        coVerify(exactly = 0) { liftsRepository.update(any()) }
        // Returned value reflects the update
        assertEquals(VolumeType.BICEP.bitMask, result.volumeTypesBitmask)
        assertEquals(initial.secondaryVolumeTypesBitmask, result.secondaryVolumeTypesBitmask)
    }

    @Test
    fun `returns the transaction block result (sanity)`() = runTest {
        val initial = createTestLift(volumeTypesBitmask = VolumeType.CHEST.bitMask)
        val out = useCase(initial, index = 0, newVolumeType = VolumeType.CHEST, volumeTypeCategory = VolumeTypeCategory.PRIMARY)
        assertEquals(initial.copy(volumeTypesBitmask = VolumeType.CHEST.bitMask), out)
    }
}
