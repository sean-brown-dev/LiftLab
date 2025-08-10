package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.util.createLift
import com.browntowndev.liftlab.core.domain.util.createLiftMetricChart
import com.browntowndev.liftlab.core.domain.util.createProgram
import com.browntowndev.liftlab.core.domain.util.createVolumeMetricChart
import com.browntowndev.liftlab.core.domain.util.createWorkoutLogEntry
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class GetConfiguredMetricsStateFlowUseCaseTest {

    @MockK
    private lateinit var programsRepository: ProgramsRepository
    @MockK
    private lateinit var workoutLogRepository: WorkoutLogRepository
    @MockK
    private lateinit var liftsRepository: LiftsRepository
    @MockK
    private lateinit var liftMetricChartsRepository: LiftMetricChartsRepository
    @MockK
    private lateinit var volumeMetricChartsRepository: VolumeMetricChartsRepository
    @MockK
    private lateinit var getGroupedLiftMetricChartDataUseCase: GetGroupedLiftMetricChartDataUseCase
    @MockK
    private lateinit var getGroupedVolumeMetricChartDataUseCase: GetGroupedVolumeMetricChartDataUseCase

    private lateinit var useCase: GetConfiguredMetricsStateFlowUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetConfiguredMetricsStateFlowUseCase(
            programsRepository,
            workoutLogRepository,
            liftsRepository,
            liftMetricChartsRepository,
            volumeMetricChartsRepository,
            getGroupedLiftMetricChartDataUseCase,
            getGroupedVolumeMetricChartDataUseCase
        )
    }

    @Test
    fun `when invoke then combines flows and returns configured metrics state`() = runTest {
        // Given
        val activeProgram = createProgram(name = "Test Program")
        val lifts = listOf(createLift(name = "Squat"))
        val workoutLogs = listOf(createWorkoutLogEntry())
        val liftMetricCharts = listOf(createLiftMetricChart())
        val volumeMetricCharts = listOf(createVolumeMetricChart())
        val groupedLiftData: Map<Long, List<WorkoutLogEntry>> = mapOf(1L to emptyList())
        val groupedVolumeData: Map<VolumeMetricChart, List<WorkoutLogEntry>> = mapOf(volumeMetricCharts.first() to emptyList())


        every { programsRepository.getActiveProgramFlow() } returns flowOf(activeProgram)
        every { liftsRepository.getAllFlow() } returns flowOf(lifts)
        every { workoutLogRepository.getAllFlow() } returns flowOf(workoutLogs)
        every { liftMetricChartsRepository.getAllFlow() } returns flowOf(liftMetricCharts)
        every { volumeMetricChartsRepository.getAllFlow() } returns flowOf(volumeMetricCharts)
        every { getGroupedLiftMetricChartDataUseCase(any(), any()) } returns groupedLiftData
        every { getGroupedVolumeMetricChartDataUseCase(any(), any(), any()) } returns groupedVolumeData

        // When
        val result = useCase.invoke().first()

        // Then
        assertEquals(activeProgram, result.activeProgram)
        assertEquals(lifts, result.lifts)
        assertEquals(workoutLogs, result.workoutLogs)
        assertEquals(liftMetricCharts, result.liftMetricCharts)
        assertEquals(groupedLiftData, result.liftMetricChartData)
        assertEquals(groupedVolumeData, result.volumeMetricChartData)
    }
}