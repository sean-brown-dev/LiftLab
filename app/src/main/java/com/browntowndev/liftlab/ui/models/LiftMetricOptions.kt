package com.browntowndev.liftlab.ui.models

import androidx.compose.ui.graphics.vector.ImageVector

class LiftMetricOptions(
    val options: List<String>,
    val child: LiftMetricOptions? = null,
    val completionButtonText: String,
    val completionButtonIcon: ImageVector,
    val onCompletion: (() -> Unit)? = null,
    val onSelectionChanged: ((value: String, selected: Boolean) -> Unit)? = null,
)

class LiftMetricOptionTree(
    val options: List<LiftMetricOptions>,
    val completionButtonText: String,
    val completionButtonIcon: ImageVector,
)