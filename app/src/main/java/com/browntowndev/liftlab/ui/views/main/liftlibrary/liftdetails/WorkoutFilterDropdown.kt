package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.browntowndev.liftlab.R


@Composable
fun WorkoutFilterDropdown(
    selectedFilters: Set<Long>,
    filterOptions: Map<Long, String>,
    onApplyFilter: (historicalWorkoutIds: Set<Long>) -> Unit
) {
    var filterDropdownExpanded by remember { mutableStateOf(false) }
    Icon(
        modifier = Modifier
            .size(24.dp)
            .clickable { filterDropdownExpanded = true },
        painter = painterResource(id = R.drawable.filter_icon),
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = stringResource(id = R.string.accessibility_filter),
    )
    if (filterDropdownExpanded) {
        Dialog(onDismissRequest = { filterDropdownExpanded = false }) {
            val workoutFilters = remember(selectedFilters) {
                selectedFilters.toMutableSet()
            }
            LazyColumn(
                modifier = Modifier
                    .width(250.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(15.dp)
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Filter by Workout",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                        )
                    }
                }
                items(filterOptions.keys.toList(), { it }) { id ->
                    val currentWorkoutName = remember(id) {
                        filterOptions[id]!!
                    }
                    var isSelected by remember(
                        key1 = id,
                        key2 = selectedFilters
                    ) {
                        mutableStateOf(selectedFilters.contains(id))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Checkbox(
                            modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                isSelected = checked
                                if (isSelected) {
                                    workoutFilters.add(id)
                                } else {
                                    workoutFilters.remove(id)
                                }
                            }
                        )
                        Text(
                            text = currentWorkoutName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                        )
                    }
                }
                item {
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            modifier = Modifier
                                .padding(bottom = 5.dp, end = 5.dp),
                            onClick = {
                                filterDropdownExpanded = false
                                if (!selectedFilters.containsAll(workoutFilters) ||
                                    !workoutFilters.containsAll(selectedFilters)
                                ) {
                                    onApplyFilter(
                                        workoutFilters.toSet()
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = "Filter",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}