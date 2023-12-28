package com.browntowndev.liftlab.ui.views.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import com.browntowndev.liftlab.ui.models.LiftMetricOptionTree
import com.browntowndev.liftlab.ui.models.LiftMetricOptions

@Composable
fun RowMultiSelect(
    visible: Boolean,
    title: String,
    optionTree: LiftMetricOptionTree,
    selections: List<String>,
    onCancel: () -> Unit,
) {
    BackHandler(visible) {
        onCancel()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 100)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 100)
        ) + fadeOut(),
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(LocalConfiguration.current.screenHeightDp.dp.times(.45f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 5.dp
            ),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
        ) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = null,
                    )
                }
            }
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.padding(5.dp))

            var currentLeaf: LiftMetricOptions? by remember { mutableStateOf(null)}
            var nextLeaf: LiftMetricOptions? by remember { mutableStateOf(null) }
            var selectionsState by remember(selections) { mutableStateOf(selections) }
            val options = remember(currentLeaf) {
                currentLeaf?.options ?: optionTree.options.flatMap { it.options }
            }
            val isAnySelected by remember(selectionsState) {
                mutableStateOf(selectionsState.fastAny { options.contains(it) })
            }
            LazyColumn (
                modifier = Modifier.fillMaxWidth().height(200.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(options) { option ->
                    val isChecked by remember(selectionsState, option) { mutableStateOf(selectionsState.contains(option)) }
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable {
                                nextLeaf = if (currentLeaf != null) {
                                    if (currentLeaf?.onSelectionChanged != null) {
                                        currentLeaf!!.onSelectionChanged!!(option, !isChecked)
                                    }

                                    currentLeaf!!.child
                                } else {
                                    val selectedRootLeaf = optionTree.options.find { rootLeaf ->
                                        rootLeaf.options.contains(option)
                                    }
                                    selectionsState = selectionsState.toMutableList().apply {
                                        if (contains(option)) {
                                            remove(option)
                                        } else {
                                            add(option)
                                        }
                                    }
                                    selectedRootLeaf?.child
                                }
                            },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.padding(start = 10.dp, top = 15.dp, bottom = 15.dp),
                            text = option,
                            color = if (isChecked) MaterialTheme.colorScheme.onPrimary else  MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Row (
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                ConfirmationButton(
                    enabled = isAnySelected,
                    text = currentLeaf?.completionButtonText ?: optionTree.completionButtonText,
                    trailingIcon = currentLeaf?.completionButtonIcon ?: optionTree.completionButtonIcon,
                    onClick = currentLeaf?.onCompletion ?: { currentLeaf = nextLeaf },
                )
            }
        }
    }
}

@Composable
fun ConfirmationButton(
    text: String,
    enabled: Boolean,
    trailingIcon: ImageVector,
    onClick: () -> Unit,
) {
    TextButton(enabled = enabled, onClick = onClick) {
        Row (verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp
            )
            Icon(
                imageVector = trailingIcon,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
        }
    }
}