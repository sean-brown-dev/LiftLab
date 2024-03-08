package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp

@Composable
fun CollapsibleVolumeChips(
    paddingValues: PaddingValues,
    label: String,
    volumeTypes: List<CharSequence>
) {
    var volumeCollapsed by remember { mutableStateOf(true) }
    Row {
        ExpandableCard(
            paddingValues = paddingValues,
            isCollapsed = volumeCollapsed,
            summaryText = label,
            toggleExpansion = { volumeCollapsed = !volumeCollapsed },
            headerContent = {
                Text(
                    text = label,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        ) {
            LabeledChips(labels = volumeTypes, onClick = { volumeCollapsed = !volumeCollapsed })
        }
    }
}