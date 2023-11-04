package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.ui.views.composables.DeleteableOnSwipeLeft
import com.browntowndev.liftlab.ui.views.composables.SectionLabel

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

    SectionLabel(text = sectionHeader)
    volumeTypes.forEachIndexed { index, volumeType ->
        DeleteableOnSwipeLeft(
            confirmationDialogHeader = "Delete Volume Type?",
            confirmationDialogBody = "Confirm to remove the volume type.",
            enabled = remember(key1 = allowDeleteAll, key2 = volumeTypes) { allowDeleteAll || volumeTypes.size > 1 },
            shape = RoundedCornerShape(5.dp),
            onDelete = { onRemoveVolumeType(volumeTypeOptions[volumeType]!!) }
        ) {
            VolumeTypeDropdown(
                volumeTypeDisplay = volumeType,
                unselectedVolumeTypeOptions = unselectedVolumeTypeOptions,
                onUpdateVolumeType = { onUpdateVolumeType(index, it) },
            )
        }
    }

    if (unselectedVolumeTypeOptions.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(
                modifier = Modifier.padding(bottom = 20.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                onClick =  { onAddVolumeType(unselectedVolumeTypeOptions.first()) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                )
                Text(
                    text = "Add Volume Type",
                    fontSize = 14.sp,
                )
            }
        }
    }
}