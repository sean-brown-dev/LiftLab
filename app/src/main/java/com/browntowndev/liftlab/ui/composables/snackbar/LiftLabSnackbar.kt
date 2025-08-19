package com.browntowndev.liftlab.ui.composables

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun LiftLabSnackbar(snackbarHostState: SnackbarHostState) {
    SnackbarHost(snackbarHostState) { snackbarData ->
        Snackbar(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            dismissActionContentColor = MaterialTheme.colorScheme.primary,
            actionContentColor = MaterialTheme.colorScheme.primary,
            action = {
                snackbarData.visuals.actionLabel?.let { actionLabel ->
                    TextButton(
                        onClick = { snackbarData.performAction() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(actionLabel)
                    }
                }
            },
            dismissAction = {
                TextButton(
                    onClick = { snackbarData.dismiss() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("DISMISS")
                }
            }
        ) {
            Text(
                text = snackbarData.visuals.message,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}