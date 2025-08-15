package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteSetResultByIdUseCaseTest {

    @RelaxedMockK
    private lateinit var transactionScope: TransactionScope

    @RelaxedMockK
    private lateinit var repo: LiveWorkoutCompletedSetsRepository

    @BeforeEach
    fun setUp() {
        transactionScope = mockk(relaxed = true)
        repo = mockk(relaxed = true)

        coEvery { transactionScope.execute(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun myo(
        id: Long,
        liftId: Long = 10L,
        liftPos: Int = 0,
        setPos: Int = 0,
        myoPos: Int?,
        weight: Float = 100f,
        reps: Int = 5,
        rpe: Float = 9f,
        isDeload: Boolean = false
    ): MyoRepSetResult =
        MyoRepSetResult(
            id = id,
            workoutId = 1L,
            liftId = liftId,
            liftPosition = liftPos,
            setPosition = setPos,
            myoRepSetPosition = myoPos,
            weight = weight,
            reps = reps,
            rpe = rpe,
            isDeload = isDeload
        )

    @Test
    fun `deleting non-myo set deletes exactly one`() = runTest {
        val targetId = 42L
        val nonMyo = mockk<SetResult>(relaxed = true)
        every { nonMyo.id } returns targetId

        coEvery { repo.getById(targetId) } returns nonMyo
        coEvery { repo.delete(nonMyo) } returns 1

        val sut = DeleteSetResultByIdUseCase(repo, transactionScope)
        val deleted = sut(targetId)

        assertEquals(1, deleted)
        coVerify(exactly = 1) { repo.delete(nonMyo) }
        coVerify(exactly = 0) { repo.getAllForLiftAtPosition(any(), any(), any()) }
        coVerify(exactly = 0) { repo.deleteMany(any()) }
    }

    @Test
    fun `deleting activation myo deletes entire sequence`() = runTest {
        val act = myo(id = 1L, myoPos = null)
        val mini0 = myo(id = 2L, myoPos = 0)
        val mini1 = myo(id = 3L, myoPos = 1)

        coEvery { repo.getById(1L) } returns act
        coEvery { repo.getAllForLiftAtPosition(act.liftId, act.liftPosition, act.setPosition) } returns listOf(
            act, mini0, mini1
        )
        coEvery { repo.deleteMany(any()) } returns 3

        val sut = DeleteSetResultByIdUseCase(repo, transactionScope)
        val deleted = sut(1L)

        assertEquals(3, deleted)
        coVerify(exactly = 1) {
            repo.deleteMany(match { it.map { r -> (r as MyoRepSetResult).id }.toSet() == setOf(1L, 2L, 3L) })
        }
        coVerify(exactly = 0) { repo.delete(any()) }
    }

    @Test
    fun `deleting middle myo deletes that and all subsequent in sequence`() = runTest {
        val act = myo(id = 10L, myoPos = null)
        val mini0 = myo(id = 11L, myoPos = 0)
        val mini1 = myo(id = 12L, myoPos = 1) // target
        val mini2 = myo(id = 13L, myoPos = 2)

        coEvery { repo.getById(12L) } returns mini1
        coEvery { repo.getAllForLiftAtPosition(mini1.liftId, mini1.liftPosition, mini1.setPosition) } returns listOf(
            act, mini0, mini1, mini2
        )
        coEvery { repo.deleteMany(any()) } returns 2

        val sut = DeleteSetResultByIdUseCase(repo, transactionScope)
        val deleted = sut(12L)

        assertEquals(2, deleted)
        coVerify(exactly = 1) {
            repo.deleteMany(match { it.map { r -> (r as MyoRepSetResult).id }.toSet() == setOf(12L, 13L) })
        }
        coVerify(exactly = 0) { repo.delete(any()) }
    }

    @Test
    fun `deleting last myo deletes only that one`() = runTest {
        val act = myo(id = 20L, myoPos = null)
        val mini0 = myo(id = 21L, myoPos = 0)
        val mini1 = myo(id = 22L, myoPos = 1) // target is last
        coEvery { repo.getById(22L) } returns mini1
        coEvery { repo.getAllForLiftAtPosition(mini1.liftId, mini1.liftPosition, mini1.setPosition) } returns listOf(
            act, mini0, mini1
        )
        coEvery { repo.deleteMany(any()) } returns 1

        val sut = DeleteSetResultByIdUseCase(repo, transactionScope)
        val deleted = sut(22L)

        assertEquals(1, deleted)
        coVerify { repo.deleteMany(match { it.single().let { r -> (r as MyoRepSetResult).id == 22L } }) }
        coVerify(exactly = 0) { repo.delete(any()) }
    }

    @Test
    fun `nonexistent id returns 0 and does nothing`() = runTest {
        coEvery { repo.getById(777L) } returns null

        val sut = DeleteSetResultByIdUseCase(repo, transactionScope)
        val deleted = sut(777L)

        assertEquals(0, deleted)
        coVerify(exactly = 0) { repo.delete(any()) }
        coVerify(exactly = 0) { repo.getAllForLiftAtPosition(any(), any(), any()) }
        coVerify(exactly = 0) { repo.deleteMany(any()) }
    }

    @Test
    fun `getAllForLiftAtPosition may contain non-myo results but they are ignored`() = runTest {
        val act = myo(id = 30L, myoPos = null)
        val mini0 = myo(id = 31L, myoPos = 0)
        val randomOther = mockk<SetResult>(relaxed = true).also { every { it.id } returns 999L }

        coEvery { repo.getById(31L) } returns mini0
        coEvery { repo.getAllForLiftAtPosition(mini0.liftId, mini0.liftPosition, mini0.setPosition) } returns listOf(
            act, mini0, randomOther // non-myo is present but should be filtered out
        )
        coEvery { repo.deleteMany(any()) } returns 1

        val sut = DeleteSetResultByIdUseCase(repo, transactionScope)
        val deleted = sut(31L)

        assertEquals(1, deleted) // only mini0 should be >= target myoPos(0); activation(-1) is not >= 0
        coVerify(exactly = 1) {
            repo.deleteMany(match { list ->
                list.size == 1 && (list.first() as MyoRepSetResult).id == 31L
            })
        }
    }
}
