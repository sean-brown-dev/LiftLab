package com.browntowndev.liftlab.ui.views.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.FilterChipOption

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InputChipFlowRow(
    filters: List<FilterChipOption>,
    onRemove: (FilterChipOption) -> Unit,
) {
    FlowRow(
        Modifier
            .fillMaxWidth(1f)
            .wrapContentHeight(align = Alignment.Top)
            .padding(start = 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        for(filter in filters) {
            InputChip(
                modifier = Modifier.height(35.dp).padding(2.dp),
                shape = RoundedCornerShape(25.dp),
                border = InputChipDefaults.inputChipBorder(
                    borderColor = MaterialTheme.colorScheme.onTertiary,
                ),
                colors = InputChipDefaults.inputChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    labelColor = MaterialTheme.colorScheme.onTertiary,
                    trailingIconColor = MaterialTheme.colorScheme.primary,
                ),
                selected = false,
                onClick = { },
                label = { Text(
                    text = filter.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp,
                ) },
                trailingIcon = {
                    IconButton(
                        onClick = { onRemove(filter) }
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp).padding(end = 0.dp),
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                        )
                    }
                }
            )
        }
    }
}