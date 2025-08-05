package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.domain.util.TestDefaults
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class InsertManyLiftMetricChartsUseCaseTest {

    @MockK
    private lateinit var liftMetricChartsRepository: LiftMetricChartsRepository

    @MockK
    private lateinit var transactionScope: TransactionScope

    private lateinit var useCase: InsertManyLiftMetricChartsUseCase

    @BeforeEach
    fun setUp() {
        useCase = InsertManyLiftMetricChartsUseCase(liftMetricChartsRepository, transactionScope)
        coEvery { transactionScope.executeWithResult<List<Long>>(any()) } coAnswers {
            val block = it.invocation.args[0] as suspend () -> List<Long>
            block()
        }
    }

    @Test
    fun `given charts when invoke then inserts charts within transaction`() = runTest {
        val charts = listOf(
            createTestLiftMetricChart(id = 1, liftId = 1),
            createTestLiftMetricChart(id = 2, liftId = 2)
        )
        val expectedIds = listOf(1L, 2L)
        coEvery { liftMetricChartsRepository.deleteAllWithNoLifts() } just runs
        coEvery { liftMetricChartsRepository.insertMany(charts) } returns expectedIds

        val result = useCase(charts)

        assertEquals(expectedIds, result)
        coVerify(exactly = 1) { transactionScope.executeWithResult(any()) }
        coVerify(exactly = 1) { liftMetricChartsRepository.deleteAllWithNoLifts() }
        coVerify(exactly = 1) { liftMetricChartsRepository.insertMany(charts) }
    }

    private fun createTestLiftMetricChart(
        id: Long = TestDefaults.DEFAULT_ID,
        liftId: Long? = TestDefaults.DEFAULT_LIFT_ID,
        chartType: LiftMetricChartType = LiftMetricChartType.ESTIMATED_ONE_REP_MAX
    ): LiftMetricChart {
        return LiftMetricChart(
            id = id,
            liftId = liftId,
            chartType = chartType
        )
    }
}