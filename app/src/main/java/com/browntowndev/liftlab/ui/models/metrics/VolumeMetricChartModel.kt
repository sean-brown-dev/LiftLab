package com.browntowndev.liftlab.ui.models.metrics

import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel

class VolumeMetricChartModel(
    val id: Long,
    val volumeType: String,
    val volumeTypeImpact: String,
    val chartModel: BaseChartModel<LineCartesianLayerModel>,
)