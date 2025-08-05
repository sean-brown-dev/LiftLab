package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DeleteVolumeMetricChartByIdUseCaseTest {

    @MockK
    private lateinit var volumeMetricChartsRepository: VolumeMetricChartsRepository

    @MockK
    private lateinit var transactionScope: TransactionScope

    private lateinit var useCase: DeleteVolumeMetricChartByIdUseCase

    @BeforeEach
    fun setUp() {
        useCase = DeleteVolumeMetricChartByIdUseCase(volumeMetricChartsRepository, transactionScope)
        coEvery { transactionScope.execute(any()) } coAnswers {
            val block = it.invocation.args[0] as suspend () -> Unit
            block()
        }
    }

    @Test
    fun `given chart id when invoke then deletes chart within transaction`() = runTest {
        val chartId = 1L
        coEvery { volumeMetricChartsRepository.deleteById(chartId) } coAnswers { 1 }

        useCase(chartId)

        coVerify(exactly = 1) { transactionScope.execute(any()) }
        coVerify(exactly = 1) { volumeMetricChartsRepository.deleteById(chartId) }
    }
}