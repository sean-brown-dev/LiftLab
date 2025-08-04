package com.browntowndev.liftlab.ui.models

import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel

class LiftMetricChartModel(
    val id: Long,
    val liftName: String,
    val type: LiftMetricChartType,
    val chartModel: BaseChartModel<LineCartesianLayerModel>,
)