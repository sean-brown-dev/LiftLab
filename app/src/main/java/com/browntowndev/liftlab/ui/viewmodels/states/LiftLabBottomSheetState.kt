package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.runtime.Stable

@Stable
data class LiftLabBottomSheetState constructor(
    val visible: Boolean = true,
    val label: String = "",
    val volumeChipLabels: List<CharSequence> = listOf(),
)