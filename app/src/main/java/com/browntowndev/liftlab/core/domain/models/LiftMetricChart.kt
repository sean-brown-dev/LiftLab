package com.browntowndev.liftlab.core.domain.models

import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

data class LiftMetricChart(
    val id: Long = 0,
    val liftId: Long? = null,
    val chartType: LiftMetricChartType,
)
