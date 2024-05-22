package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConfirmationModal(header: String, body: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    LiftLabDialog(
        isVisible = true, // Managed via if/else at caller
        header = header,
        textAboveContent = body,
        textAboveContentFontSize = 18.sp,
        onDismiss = onCancel
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                modifier = Modifier.padding(end = 15.dp),
                onClick = onCancel,
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(onClick = onConfirm) {
                Text(text = "OK")
            }
        }
    }
}


@Composable
fun ConfirmationModal(header: String, body: String, onConfirm: () -> Unit) {
    LiftLabDialog(
        isVisible = true, // Managed via if/else at caller
        header = header,
        textAboveContent = body,
        onDismiss = onConfirm,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onConfirm) {
                Text(text = "OK")
            }
        }
    }
}