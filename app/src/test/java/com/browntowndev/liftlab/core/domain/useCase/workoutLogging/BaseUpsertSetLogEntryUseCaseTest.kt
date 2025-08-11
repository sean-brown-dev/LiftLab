
package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.domain.extensions.findSet
import com.browntowndev.liftlab.core.domain.extensions.toSetLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

@OptIn(ExperimentalCoroutinesApi::class)
class BaseUpsertSetLogEntryUseCaseTest {

    // We test the protected logic by exposing it through a tiny test subclass
    private class TestUseCase : BaseUpsertSetLogEntryUseCase() {
        fun expose(loggingWorkoutLift: LoggingWorkoutLift, setResult: SetResult, workoutLogEntryId: Long): SetLogEntry {
            // Use reflection to call the protected method (simpler than adding a wrapper)
            val fn = BaseUpsertSetLogEntryUseCase::class.declaredFunctions.first { it.name == "getSetLogEntryFromSetResult" }
            fn.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return fn.call(this, loggingWorkoutLift, setResult, workoutLogEntryId) as SetLogEntry
        }
    }

    @RelaxedMockK lateinit var loggingLift: LoggingWorkoutLift
    @RelaxedMockK lateinit var result: SetResult
    @RelaxedMockK lateinit var myoResult: MyoRepSetResult
    @RelaxedMockK lateinit var genericSet: GenericLoggingSet
    @RelaxedMockK lateinit var setLogEntry: SetLogEntry

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(
            "com.browntowndev.liftlab.core.domain.extensions.WorkoutLogExtensionsKt",
        )

        // Default stubs
        every { result.setPosition } returns 3
        every { result.liftPosition } returns 0
        every { result.toSetLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns setLogEntry

        every { genericSet.repRangeTop } returns 12
        every { genericSet.repRangeBottom } returns 8
        every { genericSet.rpeTarget } returns 8.0f
        every { genericSet.weightRecommendation } returns null
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.browntowndev.liftlab.core.domain.extensions.WorkoutLogExtensionsKt")
    }

    @Test
    fun getSetLogEntryFromSetResult_findsSetOnExactPosition() = runTest {
        every { loggingLift.findSet(setPosition = 3, myoRepSetPosition = null) } returns genericSet

        val useCase = TestUseCase()
        val out = useCase.expose(loggingLift, result, 99L)

        assertSame(setLogEntry, out)
        verify(exactly = 1) { loggingLift.findSet(3, null) }
        verify(exactly = 0) { loggingLift.findSet(2, null) }
        verify(exactly = 1) {
            result.toSetLogEntry(
                liftName = any(),
                liftMovementPattern = any(),
                progressionScheme = any(),
                workoutLogEntryId = 99L,
                repRangeTop = 12,
                repRangeBottom = 8,
                rpeTarget = 8.0f,
                weightRecommendation = null
            )
        }
    }

    @Test
    fun getSetLogEntryFromSetResult_fallbacksToPreviousPositionWhenNotFound() = runTest {
        every { loggingLift.findSet(setPosition = 3, myoRepSetPosition = null) } returns null
        every { loggingLift.findSet(setPosition = 2, myoRepSetPosition = null) } returns genericSet

        val useCase = TestUseCase()
        val out = useCase.expose(loggingLift, result, 99L)

        assertSame(setLogEntry, out)
        verify(exactly = 1) { loggingLift.findSet(3, null) }
        verify(exactly = 1) { loggingLift.findSet(2, null) }
    }

    @Test
    fun getSetLogEntryFromSetResult_throwsWhenNoSetFound() = runTest {
        every { loggingLift.findSet(setPosition = any(), myoRepSetPosition = any()) } returns null

        val useCase = TestUseCase()
        val ex = assertThrows<InvocationTargetException> {
            useCase.expose(loggingLift, result, 99L)
        }
        assertTrue(ex.targetException.message?.contains("Set not found") == true)
        verify(exactly = 0) { result.toSetLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun getSetLogEntryFromSetResult_passesMyoRepSetPositionWhenMyoResult() = runTest {
        every { myoResult.setPosition } returns 1
        every { myoResult.myoRepSetPosition } returns 2
        // Exact lookup will return a set
        every { loggingLift.findSet(setPosition = 1, myoRepSetPosition = 2) } returns genericSet
        every { myoResult.toSetLogEntry(any(), any(), any(), any(), any(), any(), any(), any()) } returns setLogEntry

        val useCase = TestUseCase()
        val out = useCase.expose(loggingLift, myoResult, 999L)

        assertSame(setLogEntry, out)
        verify(exactly = 1) { loggingLift.findSet(1, 2) }
    }
}
