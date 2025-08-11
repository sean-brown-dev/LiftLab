package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteProgramUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var workoutLiftsRepository: WorkoutLiftsRepository
    private lateinit var customLiftSetsRepository: CustomLiftSetsRepository
    private lateinit var workoutInProgressRepository: WorkoutInProgressRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: DeleteProgramUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        workoutsRepository = mockk(relaxed = true)
        workoutLiftsRepository = mockk(relaxed = true)
        customLiftSetsRepository = mockk(relaxed = true)
        workoutInProgressRepository = mockk(relaxed = true)

        // Preferred TransactionScope mocking style
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = DeleteProgramUseCase(
            programsRepository = programsRepository,
            workoutsRepository = workoutsRepository,
            workoutLiftsRepository = workoutLiftsRepository,
            customLiftSetsRepository = customLiftSetsRepository,
            workoutInProgressRepository = workoutInProgressRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `no-op when program not found (only getById called)`() = runTest {
        coEvery { programsRepository.getById(99L) } returns null

        useCase(programId = 99L)

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { programsRepository.getById(99L) }

        // No further interactions
        coVerify(exactly = 0) { workoutInProgressRepository.deleteAll() }
        coVerify(exactly = 0) { programsRepository.delete(any()) }
        coVerify(exactly = 0) { workoutsRepository.deleteByProgramId(any()) }
        coVerify(exactly = 0) { workoutLiftsRepository.deleteByProgramId(any()) }
        coVerify(exactly = 0) { customLiftSetsRepository.deleteByProgramId(any()) }
        coVerify(exactly = 0) { programsRepository.getAll() }
        coVerify(exactly = 0) { programsRepository.update(any()) }
    }

    @Test
    fun `deleting ACTIVE program clears in-progress workout, deletes all rows, and activates first when none active remain`() = runTest {
        val toDelete = mockk<Program>(relaxed = true) { every { isActive } returns true }
        coEvery { programsRepository.getById(1L) } returns toDelete

        // After deletion, repository returns two programs, both inactive
        val first = mockk<Program>(relaxed = true) { every { isActive } returns false }
        val second = mockk<Program>(relaxed = true) { every { isActive } returns false }
        val firstActivated = mockk<Program>(relaxed = true) { every { isActive } returns true }
        every { first.copy(isActive = true) } returns firstActivated

        coEvery { programsRepository.getAll() } returns listOf(first, second)

        // Child deletions and updates just run
        coEvery { programsRepository.delete(toDelete) } coAnswers { 1 }
        coEvery { workoutsRepository.deleteByProgramId(1L) } just Runs
        coEvery { workoutLiftsRepository.deleteByProgramId(1L) } just Runs
        coEvery { customLiftSetsRepository.deleteByProgramId(1L) } just Runs
        coEvery { workoutInProgressRepository.deleteAll() } coAnswers { 1 }
        coEvery { programsRepository.update(firstActivated) } just Runs

        useCase(programId = 1L)

        // Transaction executed
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }

        // Because program was active, in-progress is cleared
        coVerify(exactly = 1) { workoutInProgressRepository.deleteAll() }

        // Deletions
        coVerify(exactly = 1) { programsRepository.delete(toDelete) }
        coVerify(exactly = 1) { workoutsRepository.deleteByProgramId(1L) }
        coVerify(exactly = 1) { workoutLiftsRepository.deleteByProgramId(1L) }
        coVerify(exactly = 1) { customLiftSetsRepository.deleteByProgramId(1L) }

        // Post-delete activation: choose first() and activate
        coVerify(exactly = 1) { programsRepository.getAll() }
        coVerify(exactly = 1) { programsRepository.update(firstActivated) }
    }

    @Test
    fun `deleting ACTIVE program when another program is already active - no reactivation needed`() = runTest {
        val toDelete = mockk<Program>(relaxed = true) { every { isActive } returns true }
        coEvery { programsRepository.getById(2L) } returns toDelete

        val someActive = mockk<Program>(relaxed = true) { every { isActive } returns true }
        coEvery { programsRepository.getAll() } returns listOf(someActive)

        coEvery { workoutInProgressRepository.deleteAll() } coAnswers { 1 }
        coEvery { programsRepository.delete(toDelete) } coAnswers { 1 }
        coEvery { workoutsRepository.deleteByProgramId(2L) } just Runs
        coEvery { workoutLiftsRepository.deleteByProgramId(2L) } just Runs
        coEvery { customLiftSetsRepository.deleteByProgramId(2L) } just Runs

        useCase(programId = 2L)

        coVerify(exactly = 1) { workoutInProgressRepository.deleteAll() }
        coVerify(exactly = 1) { programsRepository.delete(toDelete) }
        coVerify(exactly = 1) { programsRepository.getAll() }
        // Because one is already active, no update should occur
        coVerify(exactly = 0) { programsRepository.update(any()) }
    }

    @Test
    fun `deleting INACTIVE program deletes rows without clearing in-progress, and activates first when none active remain`() = runTest {
        val toDelete = mockk<Program>(relaxed = true) { every { isActive } returns false }
        coEvery { programsRepository.getById(3L) } returns toDelete

        val first = mockk<Program>(relaxed = true) { every { isActive } returns false }
        val firstActivated = mockk<Program>(relaxed = true) { every { isActive } returns true }
        every { first.copy(isActive = true) } returns firstActivated
        coEvery { programsRepository.getAll() } returns listOf(first)

        coEvery { programsRepository.delete(toDelete) } coAnswers { 1 }
        coEvery { workoutsRepository.deleteByProgramId(3L) } just Runs
        coEvery { workoutLiftsRepository.deleteByProgramId(3L) } just Runs
        coEvery { customLiftSetsRepository.deleteByProgramId(3L) } just Runs

        useCase(programId = 3L)

        // Inactive -> do NOT clear in-progress
        coVerify(exactly = 0) { workoutInProgressRepository.deleteAll() }

        // Deletions happened
        coVerify(exactly = 1) { programsRepository.delete(toDelete) }
        coVerify(exactly = 1) { workoutsRepository.deleteByProgramId(3L) }
        coVerify(exactly = 1) { workoutLiftsRepository.deleteByProgramId(3L) }
        coVerify(exactly = 1) { customLiftSetsRepository.deleteByProgramId(3L) }

        // Since none active remain but list non-empty, first() becomes active
        coVerify(exactly = 1) { programsRepository.getAll() }
        coVerify(exactly = 1) { programsRepository.update(firstActivated) }
    }

    @Test
    fun `deleting INACTIVE program and no programs remain - no activation`() = runTest {
        val toDelete = mockk<Program>(relaxed = true) { every { isActive } returns false }
        coEvery { programsRepository.getById(4L) } returns toDelete

        coEvery { programsRepository.getAll() } returns emptyList()

        coEvery { programsRepository.delete(toDelete) } coAnswers { 1 }
        coEvery { workoutsRepository.deleteByProgramId(4L) } just Runs
        coEvery { workoutLiftsRepository.deleteByProgramId(4L) } just Runs
        coEvery { customLiftSetsRepository.deleteByProgramId(4L) } just Runs

        useCase(programId = 4L)

        coVerify(exactly = 0) { workoutInProgressRepository.deleteAll() }
        coVerify(exactly = 1) { programsRepository.delete(toDelete) }
        coVerify(exactly = 1) { workoutsRepository.deleteByProgramId(4L) }
        coVerify(exactly = 1) { workoutLiftsRepository.deleteByProgramId(4L) }
        coVerify(exactly = 1) { customLiftSetsRepository.deleteByProgramId(4L) }

        // allPrograms empty -> no update
        coVerify(exactly = 1) { programsRepository.getAll() }
        coVerify(exactly = 0) { programsRepository.update(any()) }
    }
}
