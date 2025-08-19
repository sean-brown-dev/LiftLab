package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.ui.composables.dropdown.TextDropdown
import com.browntowndev.liftlab.ui.extensions.displayName


@Composable
fun VolumeTypeDropdown(
    volumeTypeDisplay: VolumeType,
    unselectedVolumeTypeOptions: List<VolumeType>,
    onUpdateVolumeType: (newVolumeType: VolumeType) -> Unit,
) {
    var isExpanded by remember(volumeTypeDisplay) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clickable { isExpanded = true }
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(5.dp)
            )
            .fillMaxWidth()
            .padding(13.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextDropdown(
            isExpanded = isExpanded,
            onToggleExpansion = { isExpanded = !isExpanded },
            text = volumeTypeDisplay.displayName(),
            fontSize = 18.sp
        ) {
            unselectedVolumeTypeOptions.fastForEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName(),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    onClick = {
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