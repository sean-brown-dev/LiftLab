package com.browntowndev.liftlab.ui.viewmodels.states

data class BottomSheetState constructor(
    val visible: Boolean = true,
    val label: String = "",
    val volumeChipLabels: List<CharSequence> = listOf(),
)