package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.ui.views.navigation.LiftLabBottomSheet

@Composable
fun VolumeChipBottomSheet(
    placeAboveBottomNavBar: Boolean,
    title: String,
    volumeChipLabels: List<CharSequence>,
    content: @Composable (PaddingValues) -> Unit,
) {
    var sheetPeekHeight by remember { mutableStateOf(if(placeAboveBottomNavBar) 110.dp else 55.dp) }
    var bottomSpacerHeight by remember { mutableStateOf(if(placeAboveBottomNavBar) 55.dp else 0.dp) }
    LaunchedEffect(key1 = placeAboveBottomNavBar) {
        sheetPeekHeight = if(placeAboveBottomNavBar) 115.dp else 35.dp
        bottomSpacerHeight = if(placeAboveBottomNavBar) 75.dp else 0.dp
    }

    LiftLabBottomSheet(
        sheetPeekHeight = sheetPeekHeight,
        bottomSpacerHeight = bottomSpacerHeight,
        label = title,
        volumeTypes = volumeChipLabels,
        content = content,
    )
}