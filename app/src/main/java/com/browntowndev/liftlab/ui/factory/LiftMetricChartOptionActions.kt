package com.browntowndev.liftlab.ui.factory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.enums.displayName
import com.browntowndev.liftlab.ui.models.metrics.LiftMetricOptionTree
import com.browntowndev.liftlab.ui.models.metrics.LiftMetricOptions

/**
 * Groups all the user actions from the lift metric chart picker.
 * This keeps the factory function's signature clean.
 */
data class LiftMetricChartOptionActions(
    val onSelectLiftForMetricCharts: () -> Unit,
    val onUpdateLiftChartTypeSelections: (type: String, selected: Boolean) -> Unit,
    val onAddVolumeMetricChart: () -> Unit,
    val onUpdateVolumeTypeImpactSelection: (type: String, selected: Boolean) -> Unit,
    val onUpdateVolumeTypeSelections: (type: String, selected: Boolean) -> Unit
)

/**
 * Creates the LiftMetricOptionTree used for the chart picker.
 * This is decoupled from the ViewModel and is concerned only with building the UI model.
 */
fun createLiftMetricChartOptions(actions: LiftMetricChartOptionActions): LiftMetricOptionTree {
    return LiftMetricOptionTree(
        completionButtonText = "Next",
        completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        options = listOf(
            LiftMetricOptions(
                options = listOf("Lift Metrics"),
                child = LiftMetricOptions(
                    options = LiftMetricChartType.entries.map { it.displayName() },
                    completionButtonText = "Choose Lift",
                    completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onCompletion = actions.onSelectLiftForMetricCharts,
                    onSelectionChanged = actions.onUpdateLiftChartTypeSelections
                ),
                completionButtonText = "Next",
                completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            ),
            LiftMetricOptions(
                options = listOf("Volume Metrics"),
                child = LiftMetricOptions(
                    options = VolumeType.entries.map { it.displayName() },
                    child = LiftMetricOptions(
                        options = VolumeTypeImpact.entries.map { it.displayName() },
                        completionButtonText = "Confirm",
                        completionButtonIcon = Icons.Filled.Check,
                        onCompletion = actions.onAddVolumeMetricChart,
                        onSelectionChanged = actions.onUpdateVolumeTypeImpactSelection,
                    ),
                    completionButtonText = "Next",
                    completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onSelectionChanged = actions.onUpdateVolumeTypeSelections,
                ),
                completionButtonText = "Next",
                completionButtonIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            ),
        )
    )
}