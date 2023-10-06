package com.browntowndev.liftlab.ui.models

import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel

class ChartModel(
    val chartEntryModel: ChartEntryModel,
    val axisValuesOverrider: AxisValuesOverrider<ChartEntryModel>?,
    val bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>,
    val startAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.Start>,
    val persistentMarkers: (Marker) -> Map<Float, Marker>?,
    val itemPlacer: AxisItemPlacer.Vertical,
    val bottomAxisLabelRotationDegrees: Float = 45f,
) {
    val hasData by lazy {
        chartEntryModel.entries[0].any()
    }
}