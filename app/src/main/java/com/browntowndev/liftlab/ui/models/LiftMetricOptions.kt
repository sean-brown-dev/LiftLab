package com.browntowndev.liftlab.ui.models

import androidx.compose.ui.graphics.vector.ImageVector

class LiftMetricOptions(
    val options: List<String>,
    val child: LiftMetricOptions? = null,
    val multiSelect: Boolean = false,
    val completionButtonText: String,
    val completionButtonIcon: ImageVector,
    val onCompletion: () -> Unit = {},
    val onSelectionChanged: (value: String, selected: Boolean) -> Unit = {_, _ ->},
)