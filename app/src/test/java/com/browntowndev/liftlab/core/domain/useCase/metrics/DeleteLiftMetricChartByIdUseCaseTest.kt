package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DeleteLiftMetricChartByIdUseCaseTest {

    @MockK
    private lateinit var liftMetricChartsRepository: LiftMetricChartsRepository

    @MockK
    private lateinit var transactionScope: TransactionScope

    private lateinit var useCase: DeleteLiftMetricChartByIdUseCase

    @BeforeEach
    fun setUp() {
        useCase = DeleteLiftMetricChartByIdUseCase(liftMetricChartsRepository, transactionScope)
        coEvery { transactionScope.execute(any<suspend () -> Any?>()) } coAnswers {
            val block = firstArg<suspend () -> Any?>()
            block()
        }
    }

    @Test
    fun `given chart id when invoke then deletes chart within transaction`() = runTest {
        val chartId = 0L
        coEvery { liftMetricChartsRepository.deleteById(chartId) } coAnswers { 1 }

        useCase(chartId)

        coVerify(exactly = 1) { transactionScope.execute(any()) }
        coVerify(exactly = 1) { liftMetricChartsRepository.deleteById(chartId) }
    }
}
