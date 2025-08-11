package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class SetProgramAsActiveUseCaseTest {

    private lateinit var programsRepository: ProgramsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: SetProgramAsActiveUseCase

    // Crashlytics mock
    private lateinit var crashlytics: FirebaseCrashlytics

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)

        // Preferred TransactionScope mocking style
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        useCase = SetProgramAsActiveUseCase(
            programsRepository = programsRepository,
            transactionScope = transactionScope
        )

        // Static-mock Crashlytics getInstance() reliably
        mockkStatic(FirebaseCrashlytics::class)
        crashlytics = mockk(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `activating a program when none are active updates only that program and does not deactivate others`() = runTest {
        val p1 = program(id = 1L, isActive = false)
        val p2 = program(id = 2L, isActive = false)

        val p2Activated = mockk<Program>(relaxed = true)
        every { p2.copy(isActive = true) } returns p2Activated

        coEvery { programsRepository.update(p2Activated) } just Runs
        // No updateMany expected

        useCase(idOfProgramToActivate = 2L, allPrograms = listOf(p1, p2))

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        coVerify(exactly = 1) { programsRepository.update(p2Activated) }
        coVerify(exactly = 0) { programsRepository.updateMany(any()) }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `activating a program when a different one is active deactivates the previously-active program`() = runTest {
        val active = program(id = 1L, isActive = true)
        val target = program(id = 2L, isActive = false)

        val targetActivated = mockk<Program>(relaxed = true)
        val activeDeactivated = mockk<Program>(relaxed = true)

        every { target.copy(isActive = true) } returns targetActivated
        every { active.copy(isActive = false) } returns activeDeactivated

        val updatedMany: CapturingSlot<List<Program>> = slot()
        coEvery { programsRepository.update(targetActivated) } just Runs
        coEvery { programsRepository.updateMany(capture(updatedMany)) } just Runs

        useCase(idOfProgramToActivate = 2L, allPrograms = listOf(active, target))

        coVerify(exactly = 1) { programsRepository.update(targetActivated) }
        coVerify(exactly = 1) { programsRepository.updateMany(any()) }
        assertEquals(listOf(activeDeactivated), updatedMany.captured)
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `activating when multiple programs are active logs to Crashlytics and deactivates all others`() = runTest {
        // Three actives; choose the middle one as the target so two remain to deactivate.
        val a = program(id = 1L, isActive = true)
        val b = program(id = 2L, isActive = true)
        val c = program(id = 3L, isActive = true)

        val bActivated = mockk<Program>(relaxed = true)
        val aDeactivated = mockk<Program>(relaxed = true)
        val cDeactivated = mockk<Program>(relaxed = true)

        every { b.copy(isActive = true) } returns bActivated
        every { a.copy(isActive = false) } returns aDeactivated
        every { c.copy(isActive = false) } returns cDeactivated

        val updatedMany: CapturingSlot<List<Program>> = slot()
        coEvery { programsRepository.update(bActivated) } just Runs
        coEvery { programsRepository.updateMany(capture(updatedMany)) } just Runs

        useCase(idOfProgramToActivate = 2L, allPrograms = listOf(a, b, c))

        // Repository interactions
        coVerify(exactly = 1) { programsRepository.update(bActivated) }
        coVerify(exactly = 1) { programsRepository.updateMany(any()) }
        assertEquals(setOf(aDeactivated, cDeactivated), updatedMany.captured.toSet())

        // Crashlytics recorded because more than one program was active at once
        verify(exactly = 1) {
            crashlytics.recordException(match { ex ->
                ex is Exception && ex.message?.contains("Multiple programs were active at once") == true
            })
        }
    }

    @Test
    fun `activating a program that is already active performs update and no deactivations`() = runTest {
        val active = program(id = 10L, isActive = true)
        val inactive = program(id = 11L, isActive = false)

        val activeActivated = mockk<Program>(relaxed = true)
        every { active.copy(isActive = true) } returns activeActivated

        coEvery { programsRepository.update(activeActivated) } just Runs

        useCase(idOfProgramToActivate = 10L, allPrograms = listOf(active, inactive))

        coVerify(exactly = 1) { programsRepository.update(activeActivated) }
        coVerify(exactly = 0) { programsRepository.updateMany(any()) }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `throws when program to activate is not found - no repo or crashlytics calls`() = runTest {
        val p1 = program(id = 1L, isActive = false)
        val p2 = program(id = 2L, isActive = true)

        assertThrows<IllegalArgumentException> {
            useCase(idOfProgramToActivate = 99L, allPrograms = listOf(p1, p2))
        }

        // No repository or crashlytics interaction on failure
        coVerify(exactly = 0) { programsRepository.update(any()) }
        coVerify(exactly = 0) { programsRepository.updateMany(any()) }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    // -------- helpers --------

    private fun program(
        id: Long,
        isActive: Boolean
    ): Program = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.isActive } returns isActive
        // copy(isActive=...) is stubbed per-test where needed
    }
}
