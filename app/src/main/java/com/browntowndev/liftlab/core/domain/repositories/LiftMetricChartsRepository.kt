package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart

interface LiftMetricChartsRepository : Repository<LiftMetricChart, Long> {
    suspend fun deleteAllWithNoLifts()
}
