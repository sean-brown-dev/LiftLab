package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.views.navigation.LiftLabBottomSheet

@Composable
fun VolumeChipBottomSheet(
    placeAboveBottomNavBar: Boolean,
    title: String,
    combinedVolumeChipLabels: List<CharSequence>,
    primaryVolumeChipLabels: List<CharSequence>,
    secondaryVolumeChipLabels: List<CharSequence>,
    content: @Composable (PaddingValues) -> Unit,
) {
    val sheetPeekHeight by remember(placeAboveBottomNavBar) { mutableStateOf(if(placeAboveBottomNavBar) 125.dp else 35.dp) }
    val bottomSpacerHeight by remember(placeAboveBottomNavBar) { mutableStateOf(if(placeAboveBottomNavBar) 85.dp else 0.dp) }

    LiftLabBottomSheet(
        sheetPeekHeight = sheetPeekHeight,
        bottomSpacerHeight = bottomSpacerHeight,
        label = title,
        combinedVolumeChipLabels = combinedVolumeChipLabels,
        primaryVolumeChipLabels = primaryVolumeChipLabels,
        secondaryVolumeChipLabels = secondaryVolumeChipLabels,
        content = content,
    )
}