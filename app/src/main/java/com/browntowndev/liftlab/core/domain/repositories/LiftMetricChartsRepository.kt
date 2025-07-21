package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.LiftMetricChart

interface LiftMetricChartsRepository : Repository<LiftMetricChart, Long> {
    suspend fun deleteAllWithNoLifts()
}
