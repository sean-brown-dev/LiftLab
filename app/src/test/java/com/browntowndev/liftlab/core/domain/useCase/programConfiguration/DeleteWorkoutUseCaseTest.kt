
package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.delta.ProgramUpdate
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DeleteWorkoutUseCaseTest {
    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: DeleteWorkoutUseCase

    @BeforeEach
    fun setup() {
        programsRepository = mockk(relaxed = true)
        workoutsRepository = mockk(relaxed = true)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        useCase = DeleteWorkoutUseCase(programsRepository, workoutsRepository, transactionScope)
    }

    @Test
    @DisplayName("DeleteWorkoutUseCase: removes workout id, reindexes remaining, and adjusts microcycle when needed")
    fun deletes_and_reindexes_and_adjusts_microcycle() = runTest {
        val programId = 3L
        val toDelete = Workout(id = 20L, programId = programId, name = "X", position = 1, lifts = emptyList())
        val remaining = listOf(
            Workout(id = 21L, programId = programId, name = "A", position = 0, lifts = emptyList()),
            Workout(id = 22L, programId = programId, name = "B", position = 2, lifts = emptyList())
        )
        // Repository returns all workouts; use case filters out the deleted and reindexes
        coEvery { workoutsRepository.getAllForProgramWithoutLiftsPopulated(programId) } returns remaining + toDelete

        // First applyDelta (removal + reindex)
        val captured = mutableListOf<Pair<Long, ProgramDelta>>()
        coEvery { programsRepository.applyDelta(capture(slot<Long>()), capture(slot<ProgramDelta>())) } answers {
            captured += (firstArg<Long>() to secondArg<ProgramDelta>())
        }

        // Active program has microcycle position 5 -> higher than last reindexed (1) => adjust
        coEvery { programsRepository.getActive() } returns Program(id = programId, name = "P", isActive = true, currentMicrocyclePosition = 5)

        useCase.invoke(toDelete)

        // First call: removal + updates
        val first = captured.first { it.first == programId }.second
        assertTrue(first.removedWorkoutIds.contains(20L))
        // Expect reindex of remaining workouts: by sorted position -> indices 0..n-1
        val updatesById = first.workouts.associateBy { it.workoutId }
        assertEquals(Patch.Set(0), updatesById.getValue(21L).workoutUpdate?.position)
        assertEquals(Patch.Set(1), updatesById.getValue(22L).workoutUpdate?.position)

        // Second call: microcycle adjustment
        val second = captured.last().second
        assertEquals(ProgramUpdate(currentMicrocyclePosition = Patch.Set(1)), second.programUpdate)
    }

    @Test
    @DisplayName("DeleteWorkoutUseCase: when microcycle position already within range, no second delta")
    fun deletes_and_reindexes_without_adjusting_microcycle_when_not_needed() = runTest {
        val programId = 4L
        val toDelete = Workout(id = 30L, programId = programId, name = "Gone", position = 0, lifts = emptyList())
        val remaining = listOf(
            Workout(id = 31L, programId = programId, name = "A", position = 1, lifts = emptyList()),
            Workout(id = 32L, programId = programId, name = "B", position = 2, lifts = emptyList())
        )
        coEvery { workoutsRepository.getAllForProgramWithoutLiftsPopulated(programId) } returns remaining + toDelete
        val captured = mutableListOf<Pair<Long, ProgramDelta>>()
        coEvery { programsRepository.applyDelta(capture(slot<Long>()), capture(slot<ProgramDelta>())) } answers {
            captured += (firstArg<Long>() to secondArg<ProgramDelta>())
        }
        // Active program already at last index (1 after reindex), so no second delta
        coEvery { programsRepository.getActive() } returns Program(id = programId, name = "P2", isActive = true, currentMicrocyclePosition = 1)

        useCase.invoke(toDelete)

        // Only one applyDelta call expected
        val first = captured.single { it.first == programId }.second
        assertTrue(first.removedWorkoutIds.contains(30L))
        assertEquals(2, first.workouts.size)
    }
}
