package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.browntowndev.liftlab.ui.views.composables.FocusableRoundTextField

@Composable
fun DetailsTab(
    liftName: String,
    volumeTypes: List<String>,
    secondaryVolumeTypes: List<String>,
    onLiftNameChanged: (newName: String) -> Unit,
    onUpdateVolumeType: (newBitmask: Int) -> Unit,
    onUpdateSecondaryVolumeType: (newBitmask: Int?) -> Unit,
) {
    val volumeTypeOptions = rememberSaveable {
        val sortedVolumeTypeOptions = VolumeType.values().sortedBy { it.displayName() }
        sortedVolumeTypeOptions.associateBy { it.displayName() }
    }

    val unselectedVolumeTypeOptions = remember(volumeTypes) {
        val unselectedTypes = volumeTypeOptions.keys.filterNot { volumeTypes.contains(it) }
        unselectedTypes.fastMap {
            volumeTypeOptions[it]!!.bitMask
        }
    }

    Column(
        modifier = Modifier.padding(start = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        FocusableRoundTextField(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp, end = 10.dp),
            focus = false,
            value = liftName,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.tertiaryContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            supportingText = { Text(text = "Name", color = MaterialTheme.colorScheme.tertiary) },
            onValueChange = {
                onLiftNameChanged(it)
            },
        )

        Text(
            text = "Primary Volume Types",
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 12.sp,
        )
        volumeTypes.forEachIndexed { index, volumeType ->
            var expanded by remember(volumeType) { mutableStateOf(false) }
            var selectedOption by remember(volumeType) { mutableStateOf(volumeType) }

            Row (
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
                            .fillMaxWidth()
                            .clickable(onClick = { expanded = true })
                            .padding(16.dp)
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
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
                                })
                        }
                    }
                }

                if (unselectedVolumeTypeOptions.isNotEmpty() && index == (volumeTypes.size - 1)) {
                    Icon(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clickable {
                                val currentBitmask = volumeTypes.fastMap {
                                    volumeTypeOptions[it]
                                }.sumOf {
                                    it?.bitMask ?: 0
                                }
                                val newBitmask = unselectedVolumeTypeOptions.first() + currentBitmask

                                onUpdateVolumeType(newBitmask)
                            },
                        imageVector = Icons.Filled.Add,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}