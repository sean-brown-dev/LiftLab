package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.views.composables.DeleteableOnSwipeLeft

@Composable
fun VolumeTypeMenu(
    sectionHeader: String,
    allowDeleteAll: Boolean,
    volumeTypes: List<String>,
    volumeTypeOptions: Map<String, VolumeType>,
    unselectedVolumeTypeOptions: List<VolumeType>,
    onUpdateVolumeType: (index: Int, newVolumeType: VolumeType) -> Unit,
    onAddVolumeType: (newVolumeType: VolumeType) -> Unit,
    onRemoveVolumeType: (toRemove: VolumeType) -> Unit,
) {
    Text(
        text = sectionHeader,
        color = MaterialTheme.colorScheme.tertiary,
        fontSize = 12.sp,
    )
    volumeTypes.forEachIndexed { index, volumeType ->
        var expanded by remember(volumeType) { mutableStateOf(false) }
        var selectedOption by remember(volumeType) { mutableStateOf(volumeType) }

        DeleteableOnSwipeLeft(
            confirmationDialogHeader = "Delete Volume Type?",
            confirmationDialogBody = "Confirm to remove the volume type.",
            enabled = remember(key1 = allowDeleteAll, key2 = volumeTypes) { allowDeleteAll || volumeTypes.size > 1 },
            shape = RoundedCornerShape(5.dp),
            onDelete = { onRemoveVolumeType(volumeTypeOptions[selectedOption]!!) }
        ) {
            Row(
                modifier = Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(5.dp)),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = selectedOption,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clickable(onClick = { expanded = true })
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        volumeTypeOptions.keys.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                },
                                onClick = {
                                    selectedOption = option
                                    expanded = false

                                    onUpdateVolumeType(index, volumeTypeOptions[option]!!)
                                })
                        }
                    }
                }
            }
        }
    }

    if (unselectedVolumeTypeOptions.isNotEmpty()) {
        Row {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier
                    .clickable {
                        onAddVolumeType(unselectedVolumeTypeOptions.first())
                    }
                    .padding(10.dp),
                imageVector = Icons.Filled.Add,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
        }
    }
}