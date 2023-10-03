package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.views.composables.TextDropdown


@Composable
fun VolumeTypeDropdown(
    volumeTypeDisplay: String,
    unselectedVolumeTypeOptions: List<VolumeType>,
    onUpdateVolumeType: (newVolumeType: VolumeType) -> Unit,
) {
    var isExpanded by remember(volumeTypeDisplay) { mutableStateOf(false) }
    var selectedOption by remember(volumeTypeDisplay) { mutableStateOf(volumeTypeDisplay) }

    Row(
        modifier = Modifier
            .clickable { isExpanded = true }
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(5.dp)
            )
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextDropdown(
            isExpanded = isExpanded,
            onToggleExpansion = { isExpanded = !isExpanded },
            text = volumeTypeDisplay,
            fontSize = 18.sp
        ) {
            unselectedVolumeTypeOptions.fastForEach { option ->
                val volumeTypeOption by remember(unselectedVolumeTypeOptions) { mutableStateOf(option.displayName()) }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = volumeTypeOption,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    onClick = {
                        selectedOption = volumeTypeOption
                        isExpanded = false

                        onUpdateVolumeType(option)
                    })
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            modifier = Modifier
                .clickable { isExpanded = true }
                .size(32.dp),
            imageVector = Icons.Filled.ArrowDropDown,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null
        )
    }
}