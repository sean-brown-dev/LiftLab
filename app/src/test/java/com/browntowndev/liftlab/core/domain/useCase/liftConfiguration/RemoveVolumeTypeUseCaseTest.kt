package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.enums.toVolumeTypes
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
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
class RemoveVolumeTypeUseCaseTest {
/*

/*


    private lateinit var liftsRepository: LiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: RemoveVolumeTypeUseCase

    @BeforeEach
    fun setUp() {
        liftsRepository = mockk(relaxed = true)

        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Any?>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        useCase = RemoveVolumeTypeUseCase(
            liftsRepository = liftsRepository,
            transactionScope = transactionScope
        )

        // IMPORTANT: mock the top-level Int extension fun `toVolumeTypes()`.
        // If it lives in a differently named file, update the string accordingly.
        // Try: "com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt"
        mockkStatic("com.browntowndev.liftlab.core.domain.enums.VolumeTypeKt")
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    // -------- PRIMARY branch --------

    @Test
    fun `PRIMARY - removes present type when multiple primaries exist and persists`() = runTest {
        val ab = VolumeType.AB
        val abMask = ab.bitMask
        val primaryMask = 100 // arbitrary mask value used only for stubbing
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 7L
            every { volumeTypesBitmask } returns primaryMask
            every { secondaryVolumeTypesBitmask } returns 0
        }

        // Stub: primary has "AB" and at least one other (size > 1)
        every { primaryMask.toVolumeTypes() } returns listOf(ab, ab)

        val capturedPrimary: CapturingSlot<Int> = slot()
        val updated = mockk<Lift>(relaxed = true)
        every { lift.copy(volumeTypesBitmask = capture(capturedPrimary)) } returns updated
        coEvery { liftsRepository.update(updated) } just Runs

        val out = useCase(lift, ab, VolumeTypeCategory.PRIMARY)

        // Subtraction semantics from your code
        assertEquals(primaryMask - abMask, capturedPrimary.captured)
        coVerify(exactly = 1) { liftsRepository.update(updated) }
        assertSame(updated, out)
    }

    @Test
    fun `PRIMARY - throws when attempting to remove the only remaining primary type`() = runTest {
        val ab = VolumeType.AB
        val mask = 42
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 99L
            every { volumeTypesBitmask } returns mask
        }

        // Only one primary type present
        every { mask.toVolumeTypes() } returns listOf(ab)
        val ex = assertThrows<IllegalArgumentException> {
            useCase(lift, ab, VolumeTypeCategory.PRIMARY)
        }
        assertTrue(ex.message?.contains("only remaining") == true)
        coVerify(exactly = 0) { liftsRepository.update(any()) }
    }

    @Test
    fun `PRIMARY - throws when volume type is not present in primary`() = runTest {
        val ab = VolumeType.AB
        val mask = 13
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 5L
            every { volumeTypesBitmask } returns mask
        }

        // Primary contains nothing relevant (or an empty set) -> not present
        every { mask.toVolumeTypes() } returns emptyList()

        val ex = assertThrows<IllegalArgumentException> {
            useCase(lift, ab, VolumeTypeCategory.PRIMARY)
        }
        assertTrue(ex.message?.contains("not present") == true)
        coVerify(exactly = 0) { liftsRepository.update(any()) }
    }

    // -------- SECONDARY branch --------

    @Test
    fun `SECONDARY - removes present type from secondary when it exists in PRIMARY, and persists`() = runTest {
        val ab = VolumeType.AB
        val abMask = ab.bitMask
        val primaryMask = 77
        val secondaryMask = 0b1010 // 10
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 10L
            every { volumeTypesBitmask } returns primaryMask
            every { secondaryVolumeTypesBitmask } returns secondaryMask
        }

        // Presence check is against PRIMARY per your new logic
        every { primaryMask.toVolumeTypes() } returns listOf(ab, ab)

        val capturedSecondary: CapturingSlot<Int> = slot()
        val updated = mockk<Lift>(relaxed = true)
        every { lift.copy(secondaryVolumeTypesBitmask = capture(capturedSecondary)) } returns updated
        coEvery { liftsRepository.update(updated) } just Runs

        val out = useCase(lift, ab, VolumeTypeCategory.SECONDARY)

        // Subtraction semantics on secondary
        assertEquals(secondaryMask - abMask, capturedSecondary.captured)
        coVerify(exactly = 1) { liftsRepository.update(updated) }
        assertSame(updated, out)
    }

    @Test
    fun `SECONDARY - throws when the type is not present in PRIMARY (guard), even if secondary has any value`() = runTest {
        val ab = VolumeType.AB
        val primaryMask = 50
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 11L
            every { volumeTypesBitmask } returns primaryMask
            every { secondaryVolumeTypesBitmask } returns 0b1111
        }

        // Presence check fails because PRIMARY does not contain the type
        every { primaryMask.toVolumeTypes() } returns emptyList()

        val ex = assertThrows<IllegalArgumentException> {
            useCase(lift, ab, VolumeTypeCategory.SECONDARY)
        }
        assertTrue(ex.message?.contains("not present") == true)
        coVerify(exactly = 0) { liftsRepository.update(any()) }
    }

    @Test
    fun `SECONDARY - when secondary is null, subtracts from 0 (TDD guard for your current subtraction semantics)`() = runTest {
        val ab = VolumeType.AB
        val abMask = ab.bitMask
        val primaryMask = 60
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 12L
            every { volumeTypesBitmask } returns primaryMask
            every { secondaryVolumeTypesBitmask } returns null
        }

        // Presence check passes (in PRIMARY)
        every { primaryMask.toVolumeTypes() } returns listOf(ab)

        val capturedSecondary: CapturingSlot<Int?> = slot()
        val updated = mockk<Lift>(relaxed = true)
        every { lift.copy(secondaryVolumeTypesBitmask = captureNullable(capturedSecondary)) } returns updated
        coEvery { liftsRepository.update(updated) } just Runs

        val out = useCase(lift, ab, VolumeTypeCategory.SECONDARY)

        // Per your code: (null -> 0) - mask  ==>  0 - abMask
        assertEquals(0 - abMask, capturedSecondary.captured)
        assertSame(updated, out)
        coVerify(exactly = 1) { liftsRepository.update(updated) }
    }

    // -------- persistence & transaction behavior --------

    @Test
    fun `does not persist when id is non-positive but still returns updated copy`() = runTest {
        val ab = VolumeType.AB
        val abMask = ab.bitMask
        val primaryMask = 70

        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 0L // non-persisted
            every { volumeTypesBitmask } returns primaryMask
            every { secondaryVolumeTypesBitmask } returns 0
        }

        // Primary contains at least AB and something else (size>1)
        every { primaryMask.toVolumeTypes() } returns listOf(ab, ab)

        val updated = mockk<Lift>(relaxed = true)
        every { lift.copy(volumeTypesBitmask = primaryMask - abMask) } returns updated

        val out = useCase(lift, ab, VolumeTypeCategory.PRIMARY)

        assertSame(updated, out)
        coVerify(exactly = 0) { liftsRepository.update(any()) }
    }

    @Test
    fun `executes inside execute and returns its value`() = runTest {
        val ab = VolumeType.AB
        val abMask = ab.bitMask
        val primaryMask = 80

        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 1L
            every { volumeTypesBitmask } returns primaryMask
            every { secondaryVolumeTypesBitmask } returns 0
        }

        every { primaryMask.toVolumeTypes() } returns listOf(ab, ab)

        val updated = mockk<Lift>(relaxed = true)
        every { lift.copy(volumeTypesBitmask = primaryMask - abMask) } returns updated
        coEvery { liftsRepository.update(updated) } just Runs

        val out = useCase(lift, ab, VolumeTypeCategory.PRIMARY)

        // Proves the transaction wrapper returned the block's result
        assertSame(updated, out)
        // And the repo update occurred inside it
        coVerify(exactly = 1) { liftsRepository.update(updated) }
    }

*/

*/
}
