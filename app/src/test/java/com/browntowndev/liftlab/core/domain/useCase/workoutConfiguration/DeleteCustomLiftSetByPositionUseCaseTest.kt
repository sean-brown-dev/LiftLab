package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

// ---- Explicit JUnit Jupiter assertions (no wildcards) ----
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteCustomLiftSetByPositionUseCaseTest {

    private lateinit var customSetsRepository: CustomLiftSetsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: DeleteCustomLiftSetByPositionUseCase

    @BeforeEach
    fun setUp() {
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)
        customSetsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)

        // Required style for TransactionScope from earlier threads
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = DeleteCustomLiftSetByPositionUseCase(
            customSetsRepository = customSetsRepository,
            workoutLiftsRepository = workoutLiftsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() {
        // nothing to unmock here
    }

    @Test
    fun `when deleteByPosition returns zero, it does not fetch or update`() = runTest {
        val id = 42L
        val pos = 3
        coEvery { customSetsRepository.deleteByPosition(id, pos) } returns 0

        useCase(workoutLiftId = id, position = pos)

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { customSetsRepository.deleteByPosition(id, pos) }
        coVerify(exactly = 0) { workoutLiftsRepository.getById(any()) }
        coVerify(exactly = 0) { workoutLiftsRepository.update(any()) }
    }

    @Test
    fun `when sets deleted and lift is Standard, it reduces setCount and updates`() = runTest {
        val id = 7L
        val pos = 0
        val deleteCount = 2
        coEvery { customSetsRepository.deleteByPosition(id, pos) } returns deleteCount

        // Real instance isn't required; we specifically stub copy with new count
        val originalSetCount = 5
        val updatedSetCount = originalSetCount - deleteCount

        val updatedStandard = mockk<StandardWorkoutLift>(relaxed = true) {
            every { setCount } returns updatedSetCount
        }
        val standard = mockk<StandardWorkoutLift>(relaxed = true) {
            every { setCount } returns originalSetCount
            every { copy(setCount = updatedSetCount) } returns updatedStandard
        }

        coEvery { workoutLiftsRepository.getById(id) } returns standard
        coEvery { workoutLiftsRepository.update(updatedStandard) } returns Unit

        useCase(workoutLiftId = id, position = pos)

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { customSetsRepository.deleteByPosition(id, pos) }
        coVerify(exactly = 1) { workoutLiftsRepository.getById(id) }
        coVerify(exactly = 1) { workoutLiftsRepository.update(updatedStandard) }

        assertAll(
            { assertEquals(5, originalSetCount) },
            { assertEquals(3, updatedSetCount) },
            { assertEquals(3, updatedStandard.setCount) }
        )
    }

    @Test
    fun `when sets deleted and lift is Custom, it reduces setCount and updates`() = runTest {
        val id = 8L
        val pos = 1
        val deleteCount = 1
        coEvery { customSetsRepository.deleteByPosition(id, pos) } returns deleteCount

        val originalSetCount = 4
        val updatedSetCount = originalSetCount - deleteCount

        val updatedCustom = mockk<CustomWorkoutLift>(relaxed = true) {
            every { setCount } returns updatedSetCount
        }
        val custom = mockk<CustomWorkoutLift>(relaxed = true) {
            every { setCount } returns originalSetCount
            every { copy(setCount = updatedSetCount) } returns updatedCustom
        }

        coEvery { workoutLiftsRepository.getById(id) } returns custom
        coEvery { workoutLiftsRepository.update(updatedCustom) } returns Unit

        useCase(workoutLiftId = id, position = pos)

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { customSetsRepository.deleteByPosition(id, pos) }
        coVerify(exactly = 1) { workoutLiftsRepository.getById(id) }
        coVerify(exactly = 1) { workoutLiftsRepository.update(updatedCustom) }

        assertAll(
            { assertEquals(4, originalSetCount) },
            { assertEquals(3, updatedSetCount) },
            { assertEquals(3, updatedCustom.setCount) }
        )
    }

    @Test
    fun `when sets deleted but lift not found, it does not update`() = runTest {
        val id = 11L
        val pos = 2
        val deleteCount = 3
        coEvery { customSetsRepository.deleteByPosition(id, pos) } returns deleteCount

        coEvery { workoutLiftsRepository.getById(id) } returns null

        useCase(workoutLiftId = id, position = pos)

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { customSetsRepository.deleteByPosition(id, pos) }
        coVerify(exactly = 1) { workoutLiftsRepository.getById(id) }
        coVerify(exactly = 0) { workoutLiftsRepository.update(any()) }
    }

    @Test
    fun `when deleteCount exceeds current count, setCount clamps to zero`() = runTest {
        val id = 99L
        val pos = 5
        val deleteCount = 10
        coEvery { customSetsRepository.deleteByPosition(id, pos) } returns deleteCount

        val originalSetCount = 3
        val clampedSetCount = 0 // what we expect now

        val updatedStandard = mockk<StandardWorkoutLift>(relaxed = true) {
            every { setCount } returns clampedSetCount
        }
        val standard = mockk<StandardWorkoutLift>(relaxed = true) {
            every { setCount } returns originalSetCount
            every { copy(setCount = clampedSetCount) } returns updatedStandard
        }

        coEvery { workoutLiftsRepository.getById(id) } returns standard
        coEvery { workoutLiftsRepository.update(updatedStandard) } returns Unit

        useCase(workoutLiftId = id, position = pos)

        coVerify(exactly = 1) { workoutLiftsRepository.update(updatedStandard) }
        assertEquals(0, updatedStandard.setCount)
    }

}
