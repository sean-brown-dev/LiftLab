package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.views.composables.FocusableRoundTextField

@Composable
fun DetailsTab(
    liftName: String,
    liftNamePlaceholder: String = "",
    movementPattern: MovementPattern,
    volumeTypes: List<String>,
    secondaryVolumeTypes: List<String>,
    onLiftNameChanged: (newName: String) -> Unit,
    onAddVolumeType: (newVolumeType: VolumeType) -> Unit,
    onAddSecondaryVolumeType: (newVolumeType: VolumeType) -> Unit,
    onRemoveVolumeType: (toRemove: VolumeType) -> Unit,
    onRemoveSecondaryVolumeType: (toRemove: VolumeType) -> Unit,
    onUpdateVolumeType: (index: Int, newVolumeType: VolumeType) -> Unit,
    onUpdateSecondaryVolumeType: (index: Int, newVolumeType: VolumeType) -> Unit,
    onUpdateMovementPattern: (MovementPattern) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FocusableRoundTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 5.dp),
            focus = false,
            value = liftName,
            placeholder = liftNamePlaceholder,
            shape = RoundedCornerShape(5.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.tertiaryContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            supportingText = { Text(text = "Name", color = MaterialTheme.colorScheme.onBackground) },
            onValueChange = {
                onLiftNameChanged(it)
            },
        )

        MovementPatternDropdown(
            movementPatternDisplay = movementPattern.displayName(),
            onUpdateMovementPattern = onUpdateMovementPattern,
        )

        val volumeTypeOptions: Map<String, VolumeType> = remember {
            val sortedVolumeTypeOptions = VolumeType.values().sortedBy { it.displayName() }
            sortedVolumeTypeOptions.associateBy { it.displayName() }
        }

        val unselectedVolumeTypeOptions: List<VolumeType> = remember(key1 = volumeTypes, key2 = secondaryVolumeTypes) {
            val unselectedTypes = volumeTypeOptions.keys
                .filterNot {
                    volumeTypes
                        .toMutableList()
                        .apply { addAll(secondaryVolumeTypes) }
                        .contains(it)
                }

            unselectedTypes.fastMap {
                volumeTypeOptions[it]!!
            }
        }

        VolumeTypeMenu(
            sectionHeader = "Primary Volume Types",
            allowDeleteAll = false,
            volumeTypes = volumeTypes,
            volumeTypeOptions = volumeTypeOptions,
            unselectedVolumeTypeOptions = unselectedVolumeTypeOptions,
            onUpdateVolumeType = onUpdateVolumeType,
            onAddVolumeType = onAddVolumeType,
            onRemoveVolumeType = onRemoveVolumeType,
        )
        VolumeTypeMenu(
            sectionHeader = "Secondary Volume Types",
            allowDeleteAll = true,
            volumeTypes = secondaryVolumeTypes,
            volumeTypeOptions = volumeTypeOptions,
            unselectedVolumeTypeOptions = unselectedVolumeTypeOptions,
            onUpdateVolumeType = onUpdateSecondaryVolumeType,
            onAddVolumeType = onAddSecondaryVolumeType,
            onRemoveVolumeType = onRemoveSecondaryVolumeType,
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}