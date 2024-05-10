package com.browntowndev.liftlab.ui.views.main.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.composables.IconDropdown
import com.browntowndev.liftlab.ui.composables.SectionLabel

@Composable
fun ChartCard(
    label: String,
    subHeaderLabel: String = "",
    labelFontSize: TextUnit = 14.sp,
    labelPadding: PaddingValues = PaddingValues(top = 10.dp),
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    onDelete: (() -> Unit)? = null,
    chart: @Composable () -> Unit,
) {
    Card(
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                if (subHeaderLabel.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp, start = 10.dp),
                        text = subHeaderLabel,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 12.sp,
                    )
                }
                SectionLabel(
                    modifier = if (subHeaderLabel.isEmpty()) Modifier.padding(labelPadding) else Modifier.padding(bottom = 10.dp),
                    text = label,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = labelFontSize,
                )
            }

            if (onDelete != null) {
                Spacer(modifier = Modifier.weight(1f))

                var isExpanded by remember { mutableStateOf(false) }
                IconDropdown(
                    isExpanded = isExpanded,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onToggleExpansion = { isExpanded = !isExpanded }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                contentDescription = stringResource(
                                    R.string.delete_lift_metric_chart
                                )
                            )
                        },
                        onClick = onDelete
                    )
                }
            }
        }
        chart()
    }
}