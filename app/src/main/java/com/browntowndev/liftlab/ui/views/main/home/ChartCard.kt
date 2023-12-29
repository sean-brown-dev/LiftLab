package com.browntowndev.liftlab.ui.views.main.home

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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.views.composables.IconDropdown
import com.browntowndev.liftlab.ui.views.composables.SectionLabel

@Composable
fun ChartCard(
    label: String,
    labelTopPadding: Dp = 10.dp,
    onDelete: (() -> Unit)? = null,
    chart: @Composable () -> Unit,
) {
    Card(
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                modifier = Modifier.padding(top = labelTopPadding),
                text = label,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 14.sp,
            )

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