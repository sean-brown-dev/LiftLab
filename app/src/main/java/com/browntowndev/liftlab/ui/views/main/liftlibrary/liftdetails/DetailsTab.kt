package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.displayName
import com.browntowndev.liftlab.ui.composables.text.FocusableRoundTextField
import com.browntowndev.liftlab.ui.composables.text.SectionLabel

@Composable
fun DetailsTab(
    liftName: String,
    liftNamePlaceholder: String = "",
    movementPattern: MovementPattern,
    volumeTypeOptions: List<VolumeType>,
    volumeTypes: List<VolumeType>,
    secondaryVolumeTypes: List<VolumeType>,
    onLiftNameChanged: (newName: String) -> Unit,
    onAddVolumeType: (newVolumeType: VolumeType) -> Unit,
    onAddSecondaryVolumeType: (newVolumeType: VolumeType) -> Unit,
    onRemoveVolumeType: (toRemove: VolumeType) -> Unit,
    onRemoveSecondaryVolumeType: (toRemove: VolumeType) -> Unit,
    onUpdateVolumeType: (index: Int, newVolumeType: VolumeType) -> Unit,
    onUpdateSecondaryVolumeType: (index: Int, newVolumeType: VolumeType) -> Unit,
    onUpdateMovementPattern: (MovementPattern) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 10.dp, end = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionLabel(text = "DETAILS", fontSize = 14.sp)
            FocusableRoundTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 10.dp),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(.95f),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        item {
            SectionLabel(text = "VOLUME TYPES", fontSize = 14.sp)
        }

        volumeTypeMenu(
            lazyListScope = this,
            sectionHeader = "PRIMARY",
            allowDeleteAll = false,
            volumeTypes = volumeTypes,
            volumeTypeOptions = volumeTypeOptions,
            onUpdateVolumeType = onUpdateVolumeType,
            onAddVolumeType = onAddVolumeType,
            onRemoveVolumeType = onRemoveVolumeType,
        )
        volumeTypeMenu(
            lazyListScope = this,
            sectionHeader = "SECONDARY",
            allowDeleteAll = true,
            volumeTypes = secondaryVolumeTypes,
            volumeTypeOptions = volumeTypeOptions,
            onUpdateVolumeType = onUpdateSecondaryVolumeType,
            onAddVolumeType = onAddSecondaryVolumeType,
            onRemoveVolumeType = onRemoveSecondaryVolumeType,
        )
    }
}