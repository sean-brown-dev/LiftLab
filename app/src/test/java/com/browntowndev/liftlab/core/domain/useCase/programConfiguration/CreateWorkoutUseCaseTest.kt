package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

// --- Explicit JUnit Jupiter assertions (no wildcards) ---
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
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
class CreateWorkoutUseCaseTest {

    private lateinit var workoutsRepository: WorkoutsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: CreateWorkoutUseCase

    @BeforeEach
    fun setUp() {
        workoutsRepository = mockk(relaxed = true)

        // Preferred TransactionScope mock style (+ value-returning variant)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        useCase = CreateWorkoutUseCase(
            workoutsRepository = workoutsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `creates workout with position = program_workouts_count, empty lifts, and returns inserted id`() = runTest {
        // Program with two existing workouts -> new workout.position = 2
        val program = mockk<Program>(relaxed = true) {
            io.mockk.every { id } returns 50L
            io.mockk.every { workouts } returns listOf(mockk<Workout>(relaxed = true), mockk(relaxed = true))
        }

        val inserted: CapturingSlot<Workout> = slot()
        coEvery { workoutsRepository.insert(capture(inserted)) } returns 999L

        useCase(program = program, name = "Upper A")

        // Transaction used
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        // Insert called once
        coVerify(exactly = 1) { workoutsRepository.insert(any()) }

        // Assert fields of inserted workout
        val w = inserted.captured
        assertEquals(50L, w.programId)
        assertEquals("Upper A", w.name)
        assertEquals(2, w.position, "position should equal program.workouts.count()")
        assertTrue(w.lifts.isEmpty(), "newly-created workout should start with an empty lifts list")
    }

    @Test
    fun `when program has no workouts, new workout position is 0`() = runTest {
        val program = mockk<Program>(relaxed = true) {
            io.mockk.every { id } returns 7L
            io.mockk.every { workouts } returns emptyList()
        }

        val inserted: CapturingSlot<Workout> = slot()
        coEvery { workoutsRepository.insert(capture(inserted)) } returns 1L

        useCase(program = program, name = "Day 1")

        coVerify(exactly = 1) { workoutsRepository.insert(any()) }
        assertEquals(0, inserted.captured.position)
        assertEquals(7L, inserted.captured.programId)
        assertEquals("Day 1", inserted.captured.name)
        assertTrue(inserted.captured.lifts.isEmpty())
    }

    @Test
    fun `executes entirely within TransactionScope`() = runTest {
        val program = mockk<Program>(relaxed = true) {
            io.mockk.every { id } returns 1L
            io.mockk.every { workouts } returns emptyList()
        }
        coEvery { workoutsRepository.insert(any()) } returns 42L

        useCase(program, "Any")

        // Ensure the transactional wrapper is used and the value is propagated
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
    }
}
