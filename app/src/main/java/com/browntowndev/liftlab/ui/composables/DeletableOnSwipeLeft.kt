package com.browntowndev.liftlab.ui.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteableOnSwipeLeft(
    confirmationDialogHeader: String,
    confirmationDialogBody: String,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(bottomEnd = 14.dp, topEnd = 14.dp),
    onDelete: () -> Unit,
    dismissContent: @Composable (RowScope.() -> Unit),
) {
    if (enabled) {
        val coroutineScope = rememberCoroutineScope()
        var showConfirmationDialog by remember { mutableStateOf(false) }
        val dismissState = rememberSwipeToDismissBoxState()

        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart && !showConfirmationDialog) {
                showConfirmationDialog = true
            }
        }

        if (showConfirmationDialog) {
            ConfirmationDialog(
                header = confirmationDialogHeader,
                body = confirmationDialogBody,
                onConfirm = {
                    coroutineScope.launch {
                        onDelete()
                        dismissState.reset()
                        showConfirmationDialog = false
                    }
                },
                onCancel = {
                    coroutineScope.launch {
                        dismissState.reset()
                        showConfirmationDialog = false
                    }
                }
            )
        }

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromEndToStart = true,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.errorContainer
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }, label = "Swipe Left to Delete"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 10.dp)
                        .clip(shape)
                        .background(color),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 10.dp),
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            },
            content = dismissContent,
        )
    } else {
        Row {
            dismissContent()
        }
    }
}