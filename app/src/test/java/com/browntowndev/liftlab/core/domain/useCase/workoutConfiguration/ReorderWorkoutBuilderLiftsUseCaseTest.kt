
package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ReorderWorkoutBuilderLiftsUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: ReorderWorkoutBuilderLiftsUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        programsRepository = mockk(relaxed = true)
        workoutInProgressRepository = mockk(relaxed = true)
        liveWorkoutCompletedSetsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Any?>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }
        useCase = ReorderWorkoutBuilderLiftsUseCase(
            programsRepository,
            workoutInProgressRepository,
            liveWorkoutCompletedSetsRepository,
            transactionScope
        )
    }

    @Test
    @DisplayName("Applies delta that updates each workout-lift position to the mapped index")
    fun reorders_positions() = runTest {
        val wl1 = mockk<GenericWorkoutLift> { every { id } returns 1L; every { liftId } returns 101L; every { position } returns 0 }
        val wl2 = mockk<GenericWorkoutLift> { every { id } returns 2L; every { liftId } returns 102L; every { position } returns 1 }
        val wl3 = mockk<GenericWorkoutLift> { every { id } returns 3L; every { liftId } returns 103L; every { position } returns 2 }

        val newIdx = mapOf(1L to 2, 2L to 0, 3L to 1)

        val captured = slot<ProgramDelta>()
        coJustRun { programsRepository.applyDelta(eq(10L), capture(captured)) }

        useCase.invoke(programId = 10L, workoutId = 99L, workoutLifts = listOf(wl1, wl2, wl3), newWorkoutLiftIndices = newIdx)

        val delta = captured.captured
        assertEquals(1, delta.workouts.size)
        val wc = delta.workouts[0]
        assertEquals(99L, wc.workoutId)
        assertEquals(3, wc.lifts.size)

        val byLiftId = wc.lifts.associateBy { it.workoutLiftId }
        assertEquals(Patch.Set(2), byLiftId.getValue(1L).liftUpdate!!.position)
        assertEquals(Patch.Set(0), byLiftId.getValue(2L).liftUpdate!!.position)
        assertEquals(Patch.Set(1), byLiftId.getValue(3L).liftUpdate!!.position)
    }

    @Test
    @DisplayName("When workout is in progress, updates saved set results with remapped liftPosition and upserts")
    fun updates_in_progress_completed_sets() = runTest {
        coEvery { workoutInProgressRepository.isWorkoutInProgress(99L) } returns true
        coEvery { programsRepository.getActive() } returns mockk<Program>(relaxed = true)
        coJustRun { programsRepository.applyDelta(any(), any()) }

        coEvery { liveWorkoutCompletedSetsRepository.getAll() } returns emptyList()
        coEvery { liveWorkoutCompletedSetsRepository.upsertMany(any()) } returns emptyList()

        val wl1 = mockk<GenericWorkoutLift> { every { id } returns 1L; every { liftId } returns 101L; every { position } returns 0 }
        val wl2 = mockk<GenericWorkoutLift> { every { id } returns 2L; every { liftId } returns 102L; every { position } returns 1 }

        useCase.invoke(
            programId = 10L,
            workoutId = 99L,
            workoutLifts = listOf(wl1, wl2),
            newWorkoutLiftIndices = mapOf(1L to 1, 2L to 0)
        )

        coVerify { liveWorkoutCompletedSetsRepository.upsertMany(any()) }
    }
}
