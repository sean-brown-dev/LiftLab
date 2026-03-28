
package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.extensions.findSet
import com.browntowndev.liftlab.core.domain.extensions.toSetLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
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
class UpsertExistingSetResultUseCaseTest {

    @RelaxedMockK lateinit var setLogEntryRepository: SetLogEntryRepository
    @RelaxedMockK lateinit var transactionScope: TransactionScope

    @RelaxedMockK lateinit var loggingLift: LoggingWorkoutLift
    @RelaxedMockK lateinit var result: SetResult
    @RelaxedMockK lateinit var genericSet: GenericLoggingSet
    @RelaxedMockK lateinit var setLogEntry: SetLogEntry

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { result.setPosition } returns 3
        every { loggingLift.findSet(3, null) } returns genericSet
        mockkStatic(
            "com.browntowndev.liftlab.core.domain.extensions.WorkoutLogExtensionsKt",
        )

        every { genericSet.repRangeTop } returns 10
        every { genericSet.repRangeBottom } returns 8
        every { genericSet.rpeTarget } returns 8.5f
        every { genericSet.weightRecommendation } returns 97.5f

        every { result.toSetLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns setLogEntry

        // Make the transaction execute the provided block
        coEvery { transactionScope.execute(any<suspend () -> Any?>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutLogExtensionsKt")
    }

    @Test
    fun invokesRepositoryWithConvertedSetLogEntry_andReturnsId() = runTest {
        coEvery { setLogEntryRepository.upsert(setLogEntry) } returns 123L
        every { loggingLift.findSet(any(), any()) } returns LoggingStandardSet(
            repRangeTop = 10,
            repRangeBottom = 8,
            rpeTarget = 8.5f,
            weightRecommendation = 97.5f,
            position = result.liftPosition,
            hadInitialWeightRecommendation = false,
            previousSetResultLabel = "",
            repRangePlaceholder = "",
            setNumberLabel = "",
            complete = true,
            completedWeight = result.weight,
            completedReps = result.reps,
            completedRpe = result.rpe
        )

        val useCase = UpsertExistingSetResultUseCase(setLogEntryRepository, transactionScope)
        val id = useCase(
            workoutLogEntryId = 77L,
            setResult = result,
            loggingWorkoutLift = loggingLift
        )

        assertEquals(123L, id)
        coVerify(exactly = 1) { setLogEntryRepository.upsert(setLogEntry) }
    }

    @Test
    fun whenSetNotFound_throwsAndDoesNotCallRepository() = runTest {
        every { loggingLift.findSet(any(), any()) } returns null // both attempts will return null

        val useCase = UpsertExistingSetResultUseCase(setLogEntryRepository, transactionScope)

        val ex = assertThrows<Exception> {
            useCase(workoutLogEntryId = 77L, setResult = result, loggingWorkoutLift = loggingLift)
        }
        assertTrue(ex.message?.contains("Set not found") == true)
        coVerify(exactly = 0) { setLogEntryRepository.upsert(any()) }
    }
}
