package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetProgramConfigurationStateFlowUseCaseTest {

    @MockK(relaxed = true)
    private lateinit var programsRepository: ProgramsRepository

    private lateinit var useCase: GetProgramConfigurationStateFlowUseCase

    @BeforeEach
    fun setUp() {
        programsRepository = mockk(relaxed = true)
        useCase = GetProgramConfigurationStateFlowUseCase(programsRepository)
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `emits state with first active program and its workouts sorted by position`() = runTest {
        // Workouts intentionally out of order: 3, 1, 2
        val w3 = workout(position = 3)
        val w1 = workout(position = 1)
        val w2 = workout(position = 2)
        val unsorted = listOf(w3, w1, w2)

        val active = mockk<Program>(relaxed = true)
        val inactive = mockk<Program>(relaxed = true)

        val sortedSlot: CapturingSlot<List<Workout>> = slot()
        val activeCopied = mockk<Program>(relaxed = true)

        every { active.isActive } returns true
        every { inactive.isActive } returns false
        every { active.workouts } returns unsorted
        // The use case calls program.copy(workouts = sortedList); capture the sorted list
        every { active.copy(workouts = capture(sortedSlot)) } returns activeCopied

        val allPrograms = listOf(inactive, active)
        every { programsRepository.getAllFlow() } returns flow {
            emit(allPrograms)
        }

        val states = useCase().toList(mutableListOf())

        // Only one emission from upstream -> one state
        assertEquals(1, states.size)

        // program is the copy returned by copy(workouts=...)
        assertSame(activeCopied, states[0].program)
        // allPrograms passed through as-is (same reference)
        assertSame(allPrograms, states[0].allPrograms)

        // Verify sorting happened: positions should be [1, 2, 3]
        val sortedPositions = sortedSlot.captured.map { it.position }
        assertEquals(listOf(1, 2, 3), sortedPositions)
    }

    @Test
    fun `when no active program exists, program is null and allPrograms passes through`() = runTest {
        val p1 = mockk<Program>(relaxed = true) { every { isActive } returns false }
        val p2 = mockk<Program>(relaxed = true) { every { isActive } returns false }
        val allPrograms = listOf(p1, p2)

        every { programsRepository.getAllFlow() } returns flow { emit(allPrograms) }

        val states = useCase().toList(mutableListOf())

        assertEquals(1, states.size)
        assertNull(states[0].program)
        assertSame(allPrograms, states[0].allPrograms)
    }

    @Test
    fun `multiple emissions - selects first active each time and sorts its workouts`() = runTest {
        // Emission #1 -> program A active
        val a_w2 = workout(2)
        val a_w1 = workout(1)
        val progA = mockk<Program>(relaxed = true)
        val progA_copy = mockk<Program>(relaxed = true)
        val slotA: CapturingSlot<List<Workout>> = slot()
        every { progA.isActive } returns true
        every { progA.workouts } returns listOf(a_w2, a_w1)
        every { progA.copy(workouts = capture(slotA)) } returns progA_copy

        // Emission #2 -> program B active
        val b_w3 = workout(3)
        val b_w1 = workout(1)
        val b_w2 = workout(2)
        val progB = mockk<Program>(relaxed = true)
        val progB_copy = mockk<Program>(relaxed = true)
        val slotB: CapturingSlot<List<Workout>> = slot()
        every { progB.isActive } returns true
        every { progB.workouts } returns listOf(b_w3, b_w1, b_w2)
        every { progB.copy(workouts = capture(slotB)) } returns progB_copy

        // Inactives
        val inactive1 = mockk<Program>(relaxed = true) { every { isActive } returns false }
        val inactive2 = mockk<Program>(relaxed = true) { every { isActive } returns false }

        // Upstream emits twice with different active programs
        val emission1 = listOf(inactive1, progA)
        val emission2 = listOf(progB, inactive2) // now B is the first active
        every { programsRepository.getAllFlow() } returns flow {
            emit(emission1)
            emit(emission2)
        }

        val states = useCase().toList(mutableListOf())

        assertEquals(2, states.size)

        // #1 chose A
        assertSame(progA_copy, states[0].program)
        assertSame(emission1, states[0].allPrograms)
        assertEquals(listOf(1, 2), slotA.captured.map { it.position })

        // #2 chose B
        assertSame(progB_copy, states[1].program)
        assertSame(emission2, states[1].allPrograms)
        assertEquals(listOf(1, 2, 3), slotB.captured.map { it.position })
    }

    @Test
    fun `when multiple actives exist, picks the first in list order and does not touch later actives`() = runTest {
        val a_w2 = workout(2)
        val a_w1 = workout(1)
        val activeFirst = mockk<Program>(relaxed = true)
        val activeFirstCopy = mockk<Program>(relaxed = true)
        val slotFirst: CapturingSlot<List<Workout>> = slot()
        every { activeFirst.isActive } returns true
        every { activeFirst.workouts } returns listOf(a_w2, a_w1)
        every { activeFirst.copy(workouts = capture(slotFirst)) } returns activeFirstCopy

        val activeLater = mockk<Program>(relaxed = true)
        every { activeLater.isActive } returns true
        // If the code accidentally processed this too, we'd need a stub—so we DON'T stub copy on purpose.

        val allPrograms = listOf(activeFirst, activeLater)
        every { programsRepository.getAllFlow() } returns flow { emit(allPrograms) }

        val states = useCase().toList(mutableListOf())

        assertEquals(1, states.size)
        assertSame(activeFirstCopy, states[0].program)
        assertSame(allPrograms, states[0].allPrograms)
        assertEquals(listOf(1, 2), slotFirst.captured.map { it.position })
        // Implicitly: no exception thrown for activeLater.copy(...) because it was never called
    }

    // -------- helpers --------

    private fun workout(position: Int): Workout =
        mockk(relaxed = true) { every { this@mockk.position } returns position }
}
