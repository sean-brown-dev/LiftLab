package com.browntowndev.liftlab.ui.views.composables

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.ReorderableListItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

@ExperimentalFoundationApi
@Composable
fun ReorderableLazyColumn(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    items: List<ReorderableListItem>,
    saveReorder: (List<ReorderableListItem>) -> Unit,
    cancelReorder: () -> Unit,
) {
    val view = LocalView.current
    var reorderableItems by remember { mutableStateOf(items) }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyColumnState(lazyListState) { from, to ->
        reorderableItems = reorderableItems.toMutableList().apply {
            // Top item adds 1 to index, hence need to subtract 1
            add(to.index - 1, removeAt(from.index - 1))
        }

        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    BackHandler(true) {
        cancelReorder()
    }

    LaunchedEffect(items) {
        reorderableItems = items
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "Drag to Reorder",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(modifier = Modifier.height(25.dp))
            }
        }
        items(reorderableItems, { it.key }) { reorderableItem ->
            ReorderableItem(reorderableState, key = reorderableItem.key) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "dragElevationAnimation")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 2.dp)
                        .shadow(
                            elevation = elevation,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = MaterialTheme.colorScheme.primary,
                            spotColor = MaterialTheme.colorScheme.primary,
                            clip = true
                        )
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.padding(start = 10.dp, top = 25.dp, bottom = 25.dp),
                            text = reorderableItem.label,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            modifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    view.performHapticFeedback(HapticFeedbackConstants.DRAG_START)
                                },
                                onDragStopped = {
                                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                },
                            ),
                            onClick = {},
                        ) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Reorder")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        item {
            Spacer(modifier = Modifier.height(15.dp))
            Column (
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(
                    onClick = { saveReorder(reorderableItems.toList()) }
                ) {
                    Text(
                        text = "Confirm Reorder",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                }
                TextButton(
                    modifier = Modifier.padding(bottom = 15.dp),
                    onClick = { cancelReorder() }
                ) {
                    Text(
                        text = "Cancel Reorder",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}