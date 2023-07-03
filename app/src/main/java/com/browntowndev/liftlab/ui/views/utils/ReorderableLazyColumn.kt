package com.browntowndev.liftlab.ui.views.utils

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.ReorderableListItem
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@ExperimentalFoundationApi
@Composable
fun ReorderableLazyColumn(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    items: List<ReorderableListItem>,
    saveReorder: (List<ReorderableListItem>) -> Unit,
    cancelReorder: () -> Unit,
) {
    var reorderableItems by remember { mutableStateOf(items) }
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            reorderableItems = reorderableItems.toMutableList().apply {
                add(to.index -1, removeAt(from.index - 1))
            }
        },
    )

    BackHandler(true) {
        cancelReorder()
    }

    LaunchedEffect(items) {
        reorderableItems = items
    }

    LazyColumn(
        state = reorderableState.listState,
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .reorderable(reorderableState)
            .detectReorderAfterLongPress(reorderableState),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "Press, Hold & Drag to Reorder",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = "After holding, please start the drag slowly.",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 10.sp,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(25.dp))
            }
        }
        items(reorderableItems, { it.key }) { reorderableItem ->
            ReorderableItem(reorderableState = reorderableState, key = reorderableItem.key) { isDragging ->
                val elevation: State<Dp> = if (isDragging) animateDpAsState(16.dp) else animateDpAsState(0.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 0.dp)
                        .shadow(
                            elevation = elevation.value,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = MaterialTheme.colorScheme.primary,
                            spotColor = MaterialTheme.colorScheme.primary,
                            clip = true
                        )
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = reorderableItem.label,
                        modifier = Modifier
                            .padding(0.dp, 25.dp)
                            .offset(x = (-10).dp)
                    )
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
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 15.dp, 0.dp)
                        .clickable { saveReorder(reorderableItems.toList()) },
                    text = "Confirm Reorder",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 15.dp, 0.dp)
                        .clickable { cancelReorder() },
                    text = "Cancel Reorder",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 18.sp
                )
            }
        }
    }
}