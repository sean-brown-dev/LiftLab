package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

// ---- Explicit JUnit Jupiter assertion imports (no wildcards) ----
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateLiftUseCaseTest {

    private lateinit var liftsRepository: LiftsRepository
    private lateinit var transactionScope: TransactionScope
    private lateinit var useCase: CreateLiftUseCase

    @BeforeEach
    fun setUp() {
        liftsRepository = mockk(relaxed = true)

        // Your preferred TransactionScope mocking style (+ value-returning variant)
        transactionScope = mockk(relaxed = true)
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        coEvery { transactionScope.execute(any<suspend () -> Unit>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        useCase = CreateLiftUseCase(
            liftsRepository = liftsRepository,
            transactionScope = transactionScope
        )
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `empty name is replaced with 'New Lift' and the copy is inserted returned id is surfaced`() = runTest {
        val original = mockk<Lift>(relaxed = true) {
            every { name } returns ""
        }
        val renamed = mockk<Lift>(relaxed = true) {
            every { name } returns "New Lift"
        }
        // the use case calls copy(name = "New Lift")
        every { original.copy(name = "New Lift") } returns renamed

        val inserted: CapturingSlot<Lift> = slot()
        coEvery { liftsRepository.insert(capture(inserted)) } returns 123L

        useCase(original)

        // transactional wrapper
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
        // inserted the renamed copy
        assertSame(renamed, inserted.captured)
    }

    @Test
    fun `non-empty name is preserved and the same instance is inserted`() = runTest {
        val lift = mockk<Lift>(relaxed = true) {
            every { name } returns "Bench Press"
        }

        val inserted: CapturingSlot<Lift> = slot()
        coEvery { liftsRepository.insert(capture(inserted)) } returns 777L

        useCase(lift)

        // Same object must be passed to insert (no copy)
        assertSame(lift, inserted.captured)
        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
    }

    @Test
    fun `whitespace-only name is not treated as empty (no renaming)`() = runTest {
        val lift = mockk<Lift>(relaxed = true) {
            every { name } returns "   " // not empty -> should not be replaced
        }

        val inserted: CapturingSlot<Lift> = slot()
        coEvery { liftsRepository.insert(capture(inserted)) } returns 9L

        useCase(lift)

        assertSame(lift, inserted.captured, "whitespace-only name should NOT trigger defaulting")
    }

    @Test
    fun `executes inside TransactionScope and returns the lambda's value`() = runTest {
        val lift = mockk<Lift>(relaxed = true) { every { name } returns "Deadlift" }
        coEvery { liftsRepository.insert(lift) } returns 42L

        useCase(lift)

        coVerify(exactly = 1) { transactionScope.execute(any<suspend () -> Unit>()) }
    }
}
