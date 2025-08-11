package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

// Explicit Jupiter assertion imports per your request:
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.extensions.getRecalculatedStepSizeForLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateWorkoutLiftDeloadWeekUseCaseTest {

    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: UpdateWorkoutLiftDeloadWeekUseCase

    @BeforeEach
    fun setUp() {
        workoutLiftsRepository = mockk(relaxed = true)

        // Your preferred TransactionScope mocking style
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        // Also handle blocks that return a value, if any call sites use it
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        useCase = UpdateWorkoutLiftDeloadWeekUseCase(
            workoutLiftsRepository = workoutLiftsRepository,
            transactionScope = transactionScope
        )

        // Static-mock the file class that contains the extension.
        // If your function lives in a differently named file, change the string accordingly.
        mockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutExtensionsKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutExtensionsKt")
        unmockkAll()
    }

    @Test
    fun `Standard lift - explicit deloadWeek recalculates stepSize with lift-level deload`() = runTest {
        val base = stdLift(
            id = 1L, workoutId = 10L, liftId = 100L, position = 0,
            stepSize = null, deloadWeek = null
        )

        val programDeloadWeek = 3
        val explicitDeload = 1
        // We control the extension's output for determinism
        every { base.getRecalculatedStepSizeForLift(deloadToUseInsteadOfLiftLevel = explicitDeload) } returns 2

        val updatedSlot = slot<GenericWorkoutLift>()
        coEvery { workoutLiftsRepository.update(capture(updatedSlot)) } just Runs

        useCase(
            workoutLift = base,
            deloadWeek = explicitDeload,
            programDeloadWeek = programDeloadWeek
        )

        // Transaction ran
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }

        // Repository received a StandardWorkoutLift copy with updated fields
        val updated = assertInstanceOf(StandardWorkoutLift::class.java, updatedSlot.captured)
        updated as StandardWorkoutLift
        assertEquals(explicitDeload, updated.deloadWeek)
        assertEquals(2, updated.stepSize)
        // other fields left intact
        assertEquals(base.liftId, updated.liftId)
        assertEquals(base.position, updated.position)

        // Extension called exactly once with explicit deload
        verify(exactly = 1) { base.getRecalculatedStepSizeForLift(explicitDeload) }
    }

    @Test
    fun `Standard lift - null deloadWeek uses programDeloadWeek for stepSize`() = runTest {
        val base = stdLift(
            id = 2L, workoutId = 20L, liftId = 200L, position = 1,
            stepSize = 1, deloadWeek = 2 // previous values should be replaced appropriately
        )

        val programDeloadWeek = 4
        every { base.getRecalculatedStepSizeForLift(deloadToUseInsteadOfLiftLevel = programDeloadWeek) } returns 5

        val updatedSlot = slot<GenericWorkoutLift>()
        coEvery { workoutLiftsRepository.update(capture(updatedSlot)) } just Runs

        useCase(
            workoutLift = base,
            deloadWeek = null,
            programDeloadWeek = programDeloadWeek
        )

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }

        val updated = assertInstanceOf(StandardWorkoutLift::class.java, updatedSlot.captured)
        updated as StandardWorkoutLift
        assertNull(updated.deloadWeek, "Lift-level deload should remain null")
        assertEquals(5, updated.stepSize, "Step size should be recalculated using program deload")
        assertEquals(base.id, updated.id)
        assertEquals(base.liftName, updated.liftName)

        verify(exactly = 1) { base.getRecalculatedStepSizeForLift(programDeloadWeek) }
    }

    @Test
    fun `Custom lift - copies only deloadWeek (no step-size recalculation)`() = runTest {
        val custom = mockk<CustomWorkoutLift>(relaxed = true) {
            every { id } returns 42L
            every { deloadWeek } returns null
        }
        val updatedCustom = mockk<CustomWorkoutLift>(relaxed = true) {
            every { id } returns 42L
            every { deloadWeek } returns 7
        }
        // Stubbing the data-class copy by named parameter works with MockK
        every { custom.copy(deloadWeek = 7) } returns updatedCustom

        val updatedSlot = slot<GenericWorkoutLift>()
        coEvery { workoutLiftsRepository.update(capture(updatedSlot)) } just Runs

        useCase(
            workoutLift = custom,
            deloadWeek = 7,
            programDeloadWeek = 99 // should be ignored for Custom
        )

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        assertSame(updatedCustom, updatedSlot.captured, "Custom branch should pass the copy(custom, deloadWeek) to repository")
        // No calls to the Standard extension should happen in this branch
        verify(exactly = 0) { any<StandardWorkoutLift>().getRecalculatedStepSizeForLift(any()) }
    }

    @Test
    fun `unknown GenericWorkoutLift implementation throws and does not call repository`() {
        val unknown = object : GenericWorkoutLift {
            override val id: Long = 99L
            override val workoutId: Long = 9L
            override val liftId: Long = 909L
            override val liftName: String = "Unknown"
            override val liftMovementPattern: MovementPattern = MovementPattern.HORIZONTAL_PUSH
            override val liftVolumeTypes: Int = 0
            override val liftSecondaryVolumeTypes: Int? = null
            override val liftNote: String? = null
            override val position: Int = 0
            override val setCount: Int = 3
            override val progressionScheme: ProgressionScheme = ProgressionScheme.DOUBLE_PROGRESSION
            override val deloadWeek: Int? = null
            override val incrementOverride: Float? = null
            override val restTime: Duration? = null
            override val restTimerEnabled: Boolean = false
        }

        val ex = assertThrows(Exception::class.java) {
            runTest {
                useCase(workoutLift = unknown, deloadWeek = 1, programDeloadWeek = 1)
            }
        }
        assertTrue(ex.message?.contains("not recognized") == true)

        coVerify(exactly = 0) { workoutLiftsRepository.update(any()) }
    }

    // --------- helpers ---------

    private fun stdLift(
        id: Long,
        workoutId: Long,
        liftId: Long,
        position: Int,
        stepSize: Int?,
        deloadWeek: Int?
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
        restTime = null,
        restTimerEnabled = false,
        deloadWeek = deloadWeek,
        liftNote = null,
        rpeTarget = 8f,
        repRangeBottom = 8,
        repRangeTop = 10,
        stepSize = stepSize
    )
}
