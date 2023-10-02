package com.browntowndev.liftlab.ui.views.composables

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
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.rememberDismissState
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteableOnSwipeLeft(
    confirmationDialogHeader: String,
    confirmationDialogBody: String,
    dismissContent: @Composable (RowScope.() -> Unit),
    onDelete: () -> Unit,
    enabled: Boolean = true,
) {
    if (enabled) {
        val coroutineScope = rememberCoroutineScope()
        var showConfirmationDialog by remember { mutableStateOf(false) }
        val dismissState = rememberDismissState()

        LaunchedEffect(dismissState.progress) {
            if (dismissState.isDismissed(DismissDirection.EndToStart) && !showConfirmationDialog) {
                showConfirmationDialog = true
            }
        }

        if (showConfirmationDialog) {
            ConfirmationModal(
                header = confirmationDialogHeader,
                body = confirmationDialogBody,
                onConfirm = {
                    coroutineScope.launch {
                        dismissState.dismiss(DismissDirection.EndToStart)
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

        SwipeToDismiss(
            state = dismissState,
            directions = setOf(DismissDirection.EndToStart),
            background = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        DismissValue.Default -> MaterialTheme.colorScheme.errorContainer
                        DismissValue.DismissedToStart -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }, label = "Swipe Left to Delete"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(bottomEnd = 14.dp, topEnd = 14.dp))
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
            dismissContent = dismissContent,
        )
    } else {
        Row {
            dismissContent()
        }
    }
}