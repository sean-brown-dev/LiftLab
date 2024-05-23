package com.browntowndev.liftlab.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConfirmationDialog(
    header: String,
    body: String,
    confirmButtonText: String = "OK",
    cancelButtonText: String = "Cancel",
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
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
                    text = cancelButtonText,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(onClick = onConfirm) {
                Text(text = confirmButtonText)
            }
        }
    }
}


@Composable
fun ConfirmationDialog(header: String, body: String, onConfirm: () -> Unit) {
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