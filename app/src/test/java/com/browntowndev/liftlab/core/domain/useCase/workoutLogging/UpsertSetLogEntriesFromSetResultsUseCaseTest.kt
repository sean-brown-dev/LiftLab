
package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.extensions.findSet
import com.browntowndev.liftlab.core.domain.extensions.toSetLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class UpsertSetLogEntriesFromSetResultsUseCaseTest {

    @RelaxedMockK lateinit var setLogEntryRepository: SetLogEntryRepository
    @RelaxedMockK lateinit var transactionScope: TransactionScope

    @RelaxedMockK lateinit var lift0: LoggingWorkoutLift
    @RelaxedMockK lateinit var lift1: LoggingWorkoutLift
    @RelaxedMockK lateinit var res0: SetResult
    @RelaxedMockK lateinit var res1: SetResult
    @RelaxedMockK lateinit var set0: GenericLoggingSet
    @RelaxedMockK lateinit var set1: GenericLoggingSet
    @RelaxedMockK lateinit var entry0: SetLogEntry
    @RelaxedMockK lateinit var entry1: SetLogEntry

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(
            "com.browntowndev.liftlab.core.domain.extensions.WorkoutLogExtensionsKt",
        )

        every { res0.liftPosition } returns 0
        every { res1.liftPosition } returns 1
        every { res0.setPosition } returns 1
        every { res1.setPosition } returns 2

        every { lift0.findSet(1, null) } returns set0
        every { lift1.findSet(2, null) } returns set1

        every { set0.repRangeTop } returns 8
        every { set0.repRangeBottom } returns 6
        every { set0.rpeTarget } returns 7.5f
        every { set0.weightRecommendation } returns null

        every { set1.repRangeTop } returns 10
        every { set1.repRangeBottom } returns 8
        every { set1.rpeTarget } returns 8.0f
        every { set1.weightRecommendation } returns 95.0f

        every { res0.toSetLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns entry0
        every { res1.toSetLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns entry1

        coEvery { transactionScope.executeWithResult(any<suspend () -> List<Long>>()) } coAnswers {
            val block = arg<suspend () -> List<Long>>(0)
            block.invoke()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutLogExtensionsKt")
    }

    @Test
    fun mapsEachResult_toSetLogEntry_andDelegatesToRepository() = runTest {
        coEvery { setLogEntryRepository.upsertMany(listOf(entry0, entry1)) } returns listOf(101L, 202L)

        val useCase = UpsertSetLogEntriesFromSetResultsUseCase(setLogEntryRepository, transactionScope)
        val ids = useCase(
            workoutLogEntryId = 77L,
            loggingWorkoutLifts = listOf(lift0, lift1),
            setResults = listOf(res0, res1)
        )

        assertEquals(listOf(101L, 202L), ids)
        coVerify(exactly = 1) { setLogEntryRepository.upsertMany(listOf(entry0, entry1)) }
    }

    @Test
    fun whenLiftPositionOutOfBounds_throws() = runTest {
        every { res1.liftPosition } returns 2 // out of bounds for size=2

        val useCase = UpsertSetLogEntriesFromSetResultsUseCase(setLogEntryRepository, transactionScope)

        val ex = assertThrows<Exception> {
            useCase(
                workoutLogEntryId = 77L,
                loggingWorkoutLifts = listOf(lift0, lift1),
                setResults = listOf(res0, res1)
            )
        }
        assertTrue(ex.message?.contains("Lift position is out of bounds") == true)
        coVerify(exactly = 0) { setLogEntryRepository.upsertMany(any()) }
    }
}
