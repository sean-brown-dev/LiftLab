package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateProgramUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: CreateProgramUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)

        // Your preferred TransactionScope mocking style
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = CreateProgramUseCase(
            programsRepository = programsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `creates an INACTIVE program - only inserts, no deactivation of current`() = runTest {
        val insertedSlot: CapturingSlot<Program> = slot()
        coEvery { programsRepository.insert(capture(insertedSlot)) } returns 123L

        val currentActive = Program(name = "Current", isActive = true, deloadWeek = 2)

        useCase(name = "New Plan", isActive = false, currentActiveProgram = currentActive)

        // Transaction ran and only insert happened
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { programsRepository.insert(any()) }
        coVerify(exactly = 0) { programsRepository.update(any()) }

        // Inserted Program matches requested values
        assertEquals("New Plan", insertedSlot.captured.name)
        assertFalse(insertedSlot.captured.isActive)
    }

    @Test
    fun `creates an ACTIVE program with NO current active - inserts only`() = runTest {
        val inserted: CapturingSlot<Program> = slot()
        coEvery { programsRepository.insert(capture(inserted)) } returns 99L

        useCase(name = "New Active", isActive = true, currentActiveProgram = null)

        coVerify(exactly = 1) { programsRepository.insert(any()) }
        coVerify(exactly = 0) { programsRepository.update(any()) }

        assertEquals("New Active", inserted.captured.name)
        assertTrue(inserted.captured.isActive)
    }

    @Test
    fun `creates an ACTIVE program with a CURRENT active - inserts new and deactivates current via update with a COPIED instance`() = runTest {
        // Arrange
        val currentActive = Program(name = "Old Active", isActive = true, deloadWeek = 1)
        coEvery { programsRepository.insert(any()) } returns 777L

        val updatedSlot: CapturingSlot<Program> = slot()
        coEvery { programsRepository.update(capture(updatedSlot)) } just Runs

        // Act
        useCase(name = "Brand New", isActive = true, currentActiveProgram = currentActive)

        // Assert: transaction + order of operations
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerifyOrder {
            programsRepository.insert(any())
            programsRepository.update(any())
        }

        // The updated Program MUST be a deactivated copy (not the original instance)
        val updated = updatedSlot.captured
        assertFalse(updated.isActive, "Current active program should be deactivated")
        assertNotSame(
            currentActive,
            updated,
            "Use the result of copy(isActive=false) when calling update(...)"
        )
    }
}
