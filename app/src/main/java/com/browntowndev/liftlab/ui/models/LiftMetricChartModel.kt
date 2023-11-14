package com.browntowndev.liftlab.ui.models

import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

class LiftMetricChartModel(
    val liftName: String,
    val type: LiftMetricChartType,
    val chartModel: BaseChartModel,
)