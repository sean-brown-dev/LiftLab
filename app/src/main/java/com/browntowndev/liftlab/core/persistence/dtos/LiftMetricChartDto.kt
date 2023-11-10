package com.browntowndev.liftlab.core.persistence.dtos

import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

data class LiftMetricChartDto(
    val id: Long = 0,
    val liftId: Long? = null,
    val chartType: LiftMetricChartType,
)
