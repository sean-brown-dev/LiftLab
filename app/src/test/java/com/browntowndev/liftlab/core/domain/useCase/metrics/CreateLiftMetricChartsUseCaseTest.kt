package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateLiftMetricChartsUseCaseTest {

    private lateinit var useCase: CreateLiftMetricChartsUseCase
    private lateinit var liftMetricChartsRepository: LiftMetricChartsRepository
    private lateinit var transactionScope: TransactionScope

    @BeforeEach
    fun setUp() {
        liftMetricChartsRepository = mockk()
        transactionScope = mockk(relaxed = true)
        useCase = CreateLiftMetricChartsUseCase(liftMetricChartsRepository, transactionScope)
    }

    @Test
    fun `when invoke then upserts charts`() = runTest {
        val chartIds = listOf(1L)
        val liftIds = listOf(10L, 20L)
        val initialCharts = listOf(LiftMetricChart(id = 1L, liftId = 0, chartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX))

        coEvery { liftMetricChartsRepository.getMany(chartIds) } returns initialCharts
        coEvery { liftMetricChartsRepository.upsertMany(any()) } coAnswers { emptyList() }
        coEvery { transactionScope.execute(any<suspend () -> Any?>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }

        useCase(chartIds, liftIds)

        coVerify {
            liftMetricChartsRepository.upsertMany(
                listOf(
                    LiftMetricChart(id = 1L, liftId = 10L, chartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX),
                    LiftMetricChart(id = 0L, liftId = 20L, chartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX)
                )
            )
        }
    }
}