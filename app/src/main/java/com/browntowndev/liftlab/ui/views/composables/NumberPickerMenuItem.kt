package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumberPickerMenuItem(
    initialValue: Float,
    label: String,
    options: List<Float>,
    onChanged: (Float) -> Unit,
    onBackPressed: () -> Unit,
    headerContent: @Composable (() -> Unit) = {},
) {
    Column(modifier = Modifier.width(200.dp)) {
        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentDescription = null
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 10.sp,
            )
        }
        headerContent()
        NumberPickerSpinner(options = options, initialValue = initialValue, onChanged = onChanged)
    }
}