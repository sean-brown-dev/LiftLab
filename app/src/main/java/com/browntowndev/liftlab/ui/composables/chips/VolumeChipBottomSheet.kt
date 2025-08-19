package com.browntowndev.liftlab.ui.composables.chips

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.views.navigation.LiftLabBottomSheet

@Composable
fun VolumeChipBottomSheet(
    modifier: Modifier = Modifier,
    placeAboveBottomNavBar: Boolean,
    title: String,
    combinedVolumeChipLabels: List<CharSequence>,
    primaryVolumeChipLabels: List<CharSequence>,
    secondaryVolumeChipLabels: List<CharSequence>,
    content: @Composable (PaddingValues) -> Unit,
) {
    val sheetPeekHeight by remember(placeAboveBottomNavBar) { mutableStateOf(if(placeAboveBottomNavBar) 130.dp else 70.dp) }
    val bottomSpacerHeight by remember(placeAboveBottomNavBar) { mutableStateOf(if(placeAboveBottomNavBar) 85.dp else 0.dp) }
    val topSpacerHeight by remember(placeAboveBottomNavBar) { mutableStateOf(if(placeAboveBottomNavBar) 5.dp else 35.dp) }

    LiftLabBottomSheet(
        sheetPeekHeight = sheetPeekHeight,
        bottomSpacerHeight = bottomSpacerHeight,
        topSpacerHeight = topSpacerHeight,
        label = title,
        combinedVolumeChipLabels = combinedVolumeChipLabels,
        primaryVolumeChipLabels = primaryVolumeChipLabels,
        secondaryVolumeChipLabels = secondaryVolumeChipLabels,
        content = content,
    )
}