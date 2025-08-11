package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

// ---- Explicit JUnit Jupiter imports (no wildcards) ----
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import io.mockk.CapturingSlot
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteWorkoutUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var customLiftSetsRepository: CustomLiftSetsRepository
    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: DeleteWorkoutUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        workoutsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)
        customLiftSetsRepository = mockk(relaxed = true)
        workoutInProgressRepository = mockk(relaxed = true)
        liveWorkoutCompletedSetsRepository = mockk(relaxed = true)

        // Your preferred TransactionScope mocking style
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = DeleteWorkoutUseCase(
            programsRepository = programsRepository,
            workoutsRepository = workoutsRepository,
            workoutLiftsRepository = workoutLiftsRepository,
            customLiftSetsRepository = customLiftSetsRepository,
            workoutInProgressRepository = workoutInProgressRepository,
            liveWorkoutCompletedSetsRepository = liveWorkoutCompletedSetsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `deletes workout, lifts, custom sets per-lift, clears completed sets, reindexes remaining workouts, and skips microcycle update when no active program`() = runTest {
        // Workout to delete
        val workout = mockk<Workout>(relaxed = true) {
            every { id } returns 77L
            every { programId } returns 5L
        }

        // Lifts returned for this workout
        val l1 = mockk<StandardWorkoutLift>(relaxed = true) { every { id } returns 1001L }
        val l2 = mockk<StandardWorkoutLift>(relaxed = true) { every { id } returns 1002L }
        coEvery { workoutLiftsRepository.getForWorkout(77L) } returns listOf(l1, l2)
        coEvery { workoutLiftsRepository.deleteMany(listOf(l1, l2)) }  coAnswers { 2 }

        // Expect delete per **lift.id** (this will FAIL if the use case passes workout.id)
        coEvery { customLiftSetsRepository.deleteAllForLift(1001L) } just Runs
        coEvery { customLiftSetsRepository.deleteAllForLift(1002L) } just Runs

        // No matching in-progress entries; ensure delete not called
        coEvery { workoutInProgressRepository.getAll() } returns emptyList()

        // Completed sets always cleared
        coEvery { liveWorkoutCompletedSetsRepository.deleteAll() } just Runs

        // Remaining workouts in the program, unsorted
        val otherA = mockk<Workout>(relaxed = true) {
            every { id } returns 201L
            every { position } returns 2
        }
        val otherB = mockk<Workout>(relaxed = true) {
            every { id } returns 200L
            every { position } returns 0
        }
        coEvery {
            workoutsRepository.getAllForProgramWithoutLiftsPopulated(5L)
        } returns listOf(otherA, otherB)

        // Stub copy(position=...) to capture reindexing
        val aPosSlot = slot<Int>()
        val bPosSlot = slot<Int>()
        val otherAUpdated = mockk<Workout>(relaxed = true)
        val otherBUpdated = mockk<Workout>(relaxed = true)
        every { otherA.copy(position = capture(aPosSlot)) } returns otherAUpdated
        every { otherB.copy(position = capture(bPosSlot)) } returns otherBUpdated

        val updatedList: CapturingSlot<List<Workout>> = slot()
        coEvery { workoutsRepository.updateMany(capture(updatedList)) } just Runs

        // No active program => skip microcycle adjustment
        coEvery { programsRepository.getActive() } returns null

        // When
        useCase(workout)

        // Then: transaction ran
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }

        // Workout row deleted
        coVerify(exactly = 1) { workoutsRepository.delete(workout) }

        // Lifts fetched and deleted in batch
        coVerify(exactly = 1) { workoutLiftsRepository.getForWorkout(77L) }
        coVerify(exactly = 1) { workoutLiftsRepository.deleteMany(listOf(l1, l2)) }

        // Custom sets deleted **per lift id** (order equals input)
        coVerify(exactly = 1) { customLiftSetsRepository.deleteAllForLift(1001L) }
        coVerify(exactly = 1) { customLiftSetsRepository.deleteAllForLift(1002L) }

        // In-progress: none matched => delete not called
        coVerify(exactly = 1) { workoutInProgressRepository.getAll() }
        coVerify(exactly = 0) { workoutInProgressRepository.delete(any()) }

        // Completed sets cleared
        coVerify(exactly = 1) { liveWorkoutCompletedSetsRepository.deleteAll() }

        // Reindexing: sorted by original position -> [otherB(0), otherA(2)] => indices [0,1]
        assertEquals(0, bPosSlot.captured)
        assertEquals(1, aPosSlot.captured)
        // updateMany receives the copies in sorted order (mapIndexed order)
        assertEquals(listOf(otherBUpdated, otherAUpdated), updatedList.captured)

        // No active program -> no microcycle update
        coVerify(exactly = 1) { programsRepository.getActive() }
        coVerify(exactly = 0) {
            programsRepository.updateMesoAndMicroCycle(any(), any(), any(), any())
        }
    }

    @Test
    fun `adjusts microcycle when active program position exceeds lastIndex`() = runTest {
        val workout = mockk<Workout>(relaxed = true) {
            every { id } returns 1L
            every { programId } returns 10L
        }

        // No lifts
        coEvery { workoutLiftsRepository.getForWorkout(1L) } returns emptyList()
        coEvery { workoutLiftsRepository.deleteMany(emptyList()) } coAnswers { 0 }
        coEvery { liveWorkoutCompletedSetsRepository.deleteAll() } just Runs
        coEvery { workoutInProgressRepository.getAll() } returns emptyList()

        // Two remaining workouts (positions 5, 1) -> sorted -> lastIndex = 1
        val wX = mockk<Workout>(relaxed = true) { every { position } returns 5 }
        val wY = mockk<Workout>(relaxed = true) { every { position } returns 1 }
        coEvery { workoutsRepository.getAllForProgramWithoutLiftsPopulated(10L) } returns listOf(wX, wY)

        // Reindex captured
        val xPos = slot<Int>()
        val yPos = slot<Int>()
        val wXupd = mockk<Workout>(relaxed = true)
        val wYupd = mockk<Workout>(relaxed = true)
        every { wX.copy(position = capture(xPos)) } returns wXupd
        every { wY.copy(position = capture(yPos)) } returns wYupd
        coEvery { workoutsRepository.updateMany(listOf(wYupd, wXupd)) } just Runs

        // Active program with microcycle position = 5 (> lastIndex = 1)
        val program = mockk<Program>(relaxed = true) {
            every { id } returns 9L
            every { currentMesocycle } returns 2
            every { currentMicrocycle } returns 4
            every { currentMicrocyclePosition } returns 5
        }
        coEvery { programsRepository.getActive() } returns program

        // Expect update with microCyclePosition = lastIndex = 1
        coEvery {
            programsRepository.updateMesoAndMicroCycle(
                id = 9L,
                mesoCycle = 2,
                microCycle = 4,
                microCyclePosition = 1
            )
        } just Runs

        useCase(workout)

        // Reindex assertions
        assertEquals(0, yPos.captured)
        assertEquals(1, xPos.captured)
        // Microcycle adjustment happened
        coVerify(exactly = 1) {
            programsRepository.updateMesoAndMicroCycle(9L, 2, 4, 1)
        }
    }

    @Test
    fun `when no workouts remain, sets microcycle position to -1 if it was out of bounds`() = runTest {
        val workout = mockk<Workout>(relaxed = true) {
            every { id } returns 55L
            every { programId } returns 7L
        }

        coEvery { workoutLiftsRepository.getForWorkout(55L) } returns emptyList()
        coEvery { workoutLiftsRepository.deleteMany(emptyList()) } coAnswers { 0 }
        coEvery { liveWorkoutCompletedSetsRepository.deleteAll() } just Runs
        coEvery { workoutInProgressRepository.getAll() } returns emptyList()

        // After deletion, there are NO workouts left
        coEvery { workoutsRepository.getAllForProgramWithoutLiftsPopulated(7L) } returns emptyList()
        val updatedSlot: CapturingSlot<List<Workout>> = slot()
        coEvery { workoutsRepository.updateMany(capture(updatedSlot)) } just Runs

        // Active program has position 0 (> lastIndex = -1) -> expect update with -1
        val program = mockk<Program>(relaxed = true) {
            every { id } returns 42L
            every { currentMesocycle } returns 1
            every { currentMicrocycle } returns 1
            every { currentMicrocyclePosition } returns 0
        }
        coEvery { programsRepository.getActive() } returns program

        coEvery {
            programsRepository.updateMesoAndMicroCycle(42L, 1, 1, -1)
        } just Runs

        useCase(workout)

        // No workouts to update
        assertTrue(updatedSlot.captured.isEmpty())
        // Microcycle corrected to -1
        coVerify(exactly = 1) {
            programsRepository.updateMesoAndMicroCycle(42L, 1, 1, -1)
        }
    }
}
