package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class UpsertManyVolumeMetricChartsUseCaseTest {

    @MockK
    private lateinit var volumeMetricChartsRepository: VolumeMetricChartsRepository

    @MockK
    private lateinit var transactionScope: TransactionScope

    private lateinit var useCase: UpsertManyVolumeMetricChartsUseCase

    @BeforeEach
    fun setUp() {
        useCase = UpsertManyVolumeMetricChartsUseCase(volumeMetricChartsRepository, transactionScope)
        coEvery { transactionScope.execute(any()) } coAnswers {
            val block = it.invocation.args[0] as suspend () -> Unit
            block()
        }
    }

    @Test
    fun `given charts when invoke then upserts charts within transaction`() = runTest {
        val charts = listOf(
            VolumeMetricChart(
                id = 1,
                volumeType = VolumeType.CHEST,
                volumeTypeImpact = VolumeTypeImpact.PRIMARY
            ),
            VolumeMetricChart(
                id = 2,
                volumeType = VolumeType.QUAD,
                volumeTypeImpact = VolumeTypeImpact.PRIMARY)
        )
        coEvery { volumeMetricChartsRepository.upsertMany(charts) } coAnswers { emptyList() }

        useCase(charts)

        coVerify(exactly = 1) { transactionScope.execute(any()) }
        coVerify(exactly = 1) { volumeMetricChartsRepository.upsertMany(charts) }
    }
}