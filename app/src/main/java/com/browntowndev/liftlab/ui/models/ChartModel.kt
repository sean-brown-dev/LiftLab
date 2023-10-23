package com.browntowndev.liftlab.ui.models

import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.composed.ComposedChartEntryModel
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.marker.Marker

interface BaseChartModel {
    val axisValuesOverrider: AxisValuesOverrider<ChartEntryModel>?
    val bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>
    val startAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.Start>
    val persistentMarkers: ((Marker) -> Map<Float, Marker>?)?
    val startAxisItemPlacer: AxisItemPlacer.Vertical
    val bottomAxisLabelRotationDegrees: Float
}

class ChartModel(
    val chartEntryModel: ChartEntryModel,
    override val axisValuesOverrider: AxisValuesOverrider<ChartEntryModel>?,
    override val bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>,
    override val startAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.Start>,
    override val persistentMarkers: ((Marker) -> Map<Float, Marker>?)? = null,
    override val startAxisItemPlacer: AxisItemPlacer.Vertical,
    override val bottomAxisLabelRotationDegrees: Float = 45f,
): BaseChartModel {
    val hasData by lazy {
        chartEntryModel.entries.any() &&
                chartEntryModel.entries.any {
                    it.any()
                }
    }
}

class ComposedChartModel(
    val composedChartEntryModel: ComposedChartEntryModel<ChartEntryModel>,
    override val axisValuesOverrider: AxisValuesOverrider<ChartEntryModel>?,
    override val bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>,
    override val startAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.Start>,
    val endAxisValueFormatter: AxisValueFormatter<AxisPosition.Vertical.End>,
    override val persistentMarkers: ((Marker) -> Map<Float, Marker>?)? = null,
    override val startAxisItemPlacer: AxisItemPlacer.Vertical,
    val endAxisItemPlacer: AxisItemPlacer.Vertical,
    override val bottomAxisLabelRotationDegrees: Float = 45f,
): BaseChartModel {
    val hasData by lazy {
        composedChartEntryModel.entries.any() &&
                composedChartEntryModel.entries.any {
                    it.any()
                }
    }
}
