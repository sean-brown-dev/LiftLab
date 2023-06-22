package com.browntowndev.liftlab.ui.views.utils

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.FlowRowFilterChipSection

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterSelector(
    modifier: Modifier = Modifier,
    navigateBackClicked: Boolean,
    filterOptionSections: List<FlowRowFilterChipSection>,
    selectedFilters: List<String>,
    onConfirmSelections: (List<String>) -> Unit,
) {
    val mutableSelectedFilters = remember { selectedFilters.toMutableList() }

    BackHandler(true) {
        onConfirmSelections(mutableSelectedFilters)
    }

    LaunchedEffect(navigateBackClicked) {
        if (navigateBackClicked) {
            onConfirmSelections(mutableSelectedFilters)
        }
    }

    Column(
        modifier = modifier.padding(start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        filterOptionSections.fastForEach { section ->
            Text(
                modifier = Modifier.padding(top = 15.dp, bottom = 5.dp, start = 3.dp),
                text = section.sectionName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            FlowRow(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                section.filterChipOptions.value.fastForEach { movementPattern ->
                    var isSelected by remember {
                        mutableStateOf(
                            mutableSelectedFilters.contains(
                                movementPattern
                            )
                        )
                    }
                    FilterChip(
                        modifier = Modifier.padding(start = 3.dp, end = 3.dp),
                        selected = isSelected,
                        label = { Text(movementPattern) },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.primary,
                            selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        onClick = {
                            isSelected = if (mutableSelectedFilters.contains(movementPattern)) {
                                mutableSelectedFilters.remove(movementPattern)
                                false
                            } else {
                                mutableSelectedFilters.add(movementPattern)
                                true
                            }
                        }
                    )
                }
            }
        }
    }
}