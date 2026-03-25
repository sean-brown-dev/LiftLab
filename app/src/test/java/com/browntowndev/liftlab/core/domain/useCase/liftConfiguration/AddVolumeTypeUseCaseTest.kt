package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeCategory
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddVolumeTypeUseCaseTest {

    private lateinit var liftsRepository: LiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: AddVolumeTypeUseCase

    @BeforeEach
    fun setUp() {
        liftsRepository = mockk(relaxed = true)

        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Any?>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        useCase = AddVolumeTypeUseCase(
            liftsRepository = liftsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `PRIMARY category - adds to primary bitmask and persists when id is positive`() = runTest {
        // Given a lift with existing primary=2, secondary=8
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 10L
            every { volumeTypesBitmask } returns 0b0010
            every { secondaryVolumeTypesBitmask } returns 0b1000
        }

        // new volume type mask = 4
        val volumeType = mockk<VolumeType>(relaxed = true) { every { bitMask } returns 0b0100 }

        // Capture the new primary value passed to copy(...)
        val primaryAdded = slot<Int>()
        val updatedLift = mockk<Lift>(relaxed = true)
        every { lift.copy(volumeTypesBitmask = capture(primaryAdded)) } returns updatedLift

        coEvery { liftsRepository.update(updatedLift) } just Runs

        // When
        val result = useCase(lift, volumeType, VolumeTypeCategory.PRIMARY)

        // Then
        assertEquals(0b0010 + 0b0100, primaryAdded.captured)
        coVerify(exactly = 1) { liftsRepository.update(updatedLift) }
        assertSame(updatedLift, result)
    }

    @Test
    fun `SECONDARY category - adds to secondary bitmask and persists (TDD - must base on secondary, not primary)`() = runTest {
        // primary=2, secondary=8; expect secondary becomes 8+4=12
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 5L
            every { volumeTypesBitmask } returns 0b0010
            every { secondaryVolumeTypesBitmask } returns 0b1000
        }
        val volumeType = mockk<VolumeType>(relaxed = true) { every { bitMask } returns 0b0100 }

        val secondaryAdded = slot<Int>()
        val updatedLift = mockk<Lift>(relaxed = true)
        every { lift.copy(secondaryVolumeTypesBitmask = capture(secondaryAdded)) } returns updatedLift

        coEvery { liftsRepository.update(updatedLift) } just Runs

        val result = useCase(lift, volumeType, VolumeTypeCategory.SECONDARY)

        // Expectation: sum should be based on **secondary** (8) not primary (2)
        assertEquals(0b1000 + 0b0100, secondaryAdded.captured, "secondary bitmask should add to existing secondary value")
        coVerify(exactly = 1) { liftsRepository.update(updatedLift) }
        assertSame(updatedLift, result)
    }

    @Test
    fun `SECONDARY category - when secondary is null, treat as zero`() = runTest {
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 6L
            every { volumeTypesBitmask } returns 0b0001
            every { secondaryVolumeTypesBitmask } returns null
        }
        val volumeType = mockk<VolumeType>(relaxed = true) { every { bitMask } returns 0b1000 }

        val secondaryAdded = slot<Int>()
        val updatedLift = mockk<Lift>(relaxed = true)
        every { lift.copy(secondaryVolumeTypesBitmask = capture(secondaryAdded)) } returns updatedLift

        coEvery { liftsRepository.update(updatedLift) } just Runs

        val result = useCase(lift, volumeType, VolumeTypeCategory.SECONDARY)

        assertEquals(0 + 0b1000, secondaryAdded.captured)
        coVerify(exactly = 1) { liftsRepository.update(updatedLift) }
        assertSame(updatedLift, result)
    }

    @Test
    fun `does not persist when id is non-positive but still returns updated copy`() = runTest {
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 0L // not persisted
            every { volumeTypesBitmask } returns 3
            every { secondaryVolumeTypesBitmask } returns 7
        }
        val volumeType = mockk<VolumeType>(relaxed = true) { every { bitMask } returns 5 }

        val primaryAdded = slot<Int>()
        val updatedLift = mockk<Lift>(relaxed = true)
        every { lift.copy(volumeTypesBitmask = capture(primaryAdded)) } returns updatedLift

        val result = useCase(lift, volumeType, VolumeTypeCategory.PRIMARY)

        assertEquals(3 + 5, primaryAdded.captured)
        coVerify(exactly = 0) { liftsRepository.update(any()) }
        assertSame(updatedLift, result)
    }

    @Test
    fun `executes inside execute and returns its value`() = runTest {
        val lift = mockk<Lift>(relaxed = true) {
            every { id } returns 1L
            every { volumeTypesBitmask } returns 0
            every { secondaryVolumeTypesBitmask } returns 0
        }
        val vt = mockk<VolumeType>(relaxed = true) { every { bitMask } returns 1 }

        val updatedLift = mockk<Lift>(relaxed = true)
        every { lift.copy(volumeTypesBitmask = 1) } returns updatedLift
        coEvery { liftsRepository.update(updatedLift) } just Runs

        val out = useCase(lift, vt, VolumeTypeCategory.PRIMARY)

        // Proves the return value from the transaction block is surfaced
        assertSame(updatedLift, out)
    }
}
