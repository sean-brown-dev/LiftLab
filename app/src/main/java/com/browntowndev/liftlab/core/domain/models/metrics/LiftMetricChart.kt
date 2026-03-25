package com.browntowndev.liftlab.core.domain.models.metrics

import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType

data class LiftMetricChart(
    val id: Long = 0,
    val liftId: Long? = null,
    val chartType: LiftMetricChartType,
)
