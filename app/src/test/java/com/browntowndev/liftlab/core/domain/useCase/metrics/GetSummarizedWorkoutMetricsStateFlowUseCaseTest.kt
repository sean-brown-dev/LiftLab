package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.domain.models.metrics.LiftId
import com.browntowndev.liftlab.core.domain.models.metrics.WorkoutLogId
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.util.createSetLogEntry
import com.browntowndev.liftlab.core.domain.util.createWorkoutLogEntry
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.Date

@ExtendWith(MockKExtension::class)
class GetSummarizedWorkoutMetricsStateFlowUseCaseTest {

    @MockK
    private lateinit var workoutLogRepository: WorkoutLogRepository

    private lateinit var useCase: GetSummarizedWorkoutMetricsStateFlowUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetSummarizedWorkoutMetricsStateFlowUseCase(workoutLogRepository)
    }

    @Test
    fun `when workout logs are empty then return empty state`() = runTest {
        // Given
        val emptyWorkoutLogs = emptyList<WorkoutLogEntry>()
        every { workoutLogRepository.getAllFlow() } returns flowOf(emptyWorkoutLogs)

        // When
        val result = useCase.invoke().first()

        // Then
        assertTrue(result.dateOrderedWorkoutLogsWithPersonalRecords.isEmpty())
        assertTrue(result.topSets.isEmpty())
    }

    @Test
    fun `when workout logs exist then correctly calculate personal records and top sets`() = runTest {
        // Given
        val set1 = createSetLogEntry(id = 0, liftId = 1, weight = 100f, reps = 5, rpe = 8f)
        val set2 = createSetLogEntry(id = 1, liftId = 1, weight = 110f, reps = 3, rpe = 8f) // PR for lift 1
        val set3 = createSetLogEntry(id = 2, liftId = 2, weight = 50f, reps = 10, rpe = 9f) // PR for lift 2
        val set4 = createSetLogEntry(id = 3, liftId = 2, weight = 45f, reps = 12, rpe = 9f)

        val log1 = createWorkoutLogEntry(id = 1, setResults = listOf(set1, set3))
        val log2 = createWorkoutLogEntry(id = 2, date = Date.from(Instant.now().plusSeconds(1)), setResults = listOf(set2, set4))

        val workoutLogs = listOf(log1, log2)
        every { workoutLogRepository.getAllFlow() } returns flowOf(workoutLogs)

        // When
        val result = useCase.invoke().first()

        // Then
        assertEquals(2, result.dateOrderedWorkoutLogsWithPersonalRecords.size)
        assertEquals(2, result.topSets.size)

        // Verify Personal Records
        val prLift1 = result.dateOrderedWorkoutLogsWithPersonalRecords.flatMap { it.setLogEntries }.find { it.id == 1L }
        val prLift2 = result.dateOrderedWorkoutLogsWithPersonalRecords.flatMap { it.setLogEntries }.find { it.id ==2L }
        assertTrue(prLift1?.isPersonalRecord == true)
        assertTrue(prLift2?.isPersonalRecord == true)


        // Verify Top Sets
        val topSetWorkout1Lift1 = result.topSets[WorkoutLogId(1)]?.get(LiftId(1))
        val topSetWorkout1Lift2 = result.topSets[WorkoutLogId(1)]?.get(LiftId(2))
        val topSetWorkout2Lift1 = result.topSets[WorkoutLogId(2)]?.get(LiftId(1))
        val topSetWorkout2Lift2 = result.topSets[WorkoutLogId(2)]?.get(LiftId(2))

        assertEquals(set1, topSetWorkout1Lift1?.setLog)
        assertEquals(set3.copy(isPersonalRecord = true), topSetWorkout1Lift2?.setLog)
        assertEquals(set2.copy(isPersonalRecord = true), topSetWorkout2Lift1?.setLog)
        assertEquals(set4, topSetWorkout2Lift2?.setLog)

        assertEquals(1, topSetWorkout1Lift1?.setCount)
        assertEquals(1, topSetWorkout1Lift2?.setCount)
        assertEquals(1, topSetWorkout2Lift1?.setCount)
        assertEquals(1, topSetWorkout2Lift2?.setCount)
    }
}