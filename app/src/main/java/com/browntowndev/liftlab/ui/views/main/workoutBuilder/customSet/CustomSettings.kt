package com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.ui.views.utils.ConfirmationModal
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSettings(
    isVisible: Boolean,
    listState: LazyListState,
    detailExpansionStates: HashSet<Int>,
    customSets: List<GenericCustomLiftSet>,
    onAddSet: () -> Unit,
    onDeleteSet: (position: Int) -> Unit,
    onSetMatchingChanged: (position: Int, enabled: Boolean) -> Unit,
    onMatchSetGoalChanged: (position: Int, newMatchSetGoal: Int) -> Unit,
    onRepRangeBottomChanged: (position: Int, newRepRangeBottom: Int) -> Unit,
    onRepRangeTopChanged: (position: Int, newRepRangeTop: Int) -> Unit,
    onRepFloorChanged: (position: Int, newRepFloor: Int) -> Unit,
    onCustomSetTypeChanged: (position: Int, newSetType: SetType) -> Unit,
    onMaxSetsChanged: (position: Int, newMaxSets: Int) -> Unit,
    toggleRpePicker: (position: Int, visible: Boolean) -> Unit,
    togglePercentagePicker: (position: Int, visible: Boolean) -> Unit,
    toggleDetailsExpansion: (position: Int) -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        )
    ) {
        Column {
            var previousSetType: SetType? = null
            customSets.fastForEachIndexed { index, set ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(5.dp))
                }

                val coroutineScope = rememberCoroutineScope()
                var showConfirmationDialog by remember { mutableStateOf(false) }
                val dismissState = rememberDismissState()

                LaunchedEffect(dismissState.progress) {
                    Log.d(Log.DEBUG.toString(), "${dismissState.progress}")
                    if (dismissState.isDismissed(DismissDirection.EndToStart) && !showConfirmationDialog) {
                        showConfirmationDialog = true
                    }
                }

                if (showConfirmationDialog) {
                    ConfirmationModal(
                        header = "Delete the Set?",
                        body = "Are you sure you want to delete the set?",
                        onConfirm = {
                            coroutineScope.launch {
                                dismissState.dismiss(DismissDirection.EndToStart)
                                onDeleteSet(set.position)
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
                            }
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
                    dismissContent = {
                        val detailsExpanded = detailExpansionStates.contains(set.position)
                        when (set) {
                            is StandardSetDto -> {
                                StandardSet(
                                    listState = listState,
                                    detailsExpanded = detailsExpanded,
                                    position = set.position,
                                    rpeTarget = set.rpeTarget,
                                    repRangeBottom = set.repRangeBottom,
                                    repRangeTop = set.repRangeTop,
                                    onPixelOverflowChanged = onPixelOverflowChanged,
                                    onRepRangeBottomChanged = { onRepRangeBottomChanged(set.position, it) },
                                    onRepRangeTopChanged = { onRepRangeTopChanged(set.position, it) },
                                    toggleRpePicker = { toggleRpePicker(set.position, it) },
                                    toggleDetailsExpansion = { toggleDetailsExpansion(set.position) },
                                    isPreviousSetMyoRep = previousSetType == SetType.MYOREP_SET,
                                    onCustomSetTypeChanged = { setType ->
                                        onCustomSetTypeChanged(
                                            set.position,
                                            setType
                                        )
                                    },
                                )
                                previousSetType = SetType.STANDARD_SET
                            }
                            is MyoRepSetDto -> {
                                MyoRepSet(
                                    listState = listState,
                                    detailsExpanded = detailsExpanded,
                                    position = set.position,
                                    repFloor = set.repFloor,
                                    repRangeBottom = set.repRangeBottom,
                                    repRangeTop = set.repRangeTop,
                                    setMatching = set.setMatching,
                                    rpeTarget = set.rpeTarget,
                                    matchSetGoal = set.matchSetGoal,
                                    maxSets = set.maxSets,
                                    onPixelOverflowChanged = onPixelOverflowChanged,
                                    onSetMatchingChanged = { onSetMatchingChanged(set.position, it) },
                                    onRepFloorChanged = { onRepFloorChanged(set.position, it) },
                                    onRepRangeBottomChanged = { onRepRangeBottomChanged(set.position, it) },
                                    onRepRangeTopChanged = { onRepRangeTopChanged(set.position, it) },
                                    onMatchSetGoalChanged = { onMatchSetGoalChanged(set.position, it) },
                                    toggleDetailsExpansion = { toggleDetailsExpansion(set.position) },
                                    isPreviousSetMyoRep = previousSetType == SetType.MYOREP_SET,
                                    onMaxSetsChanged = { onMaxSetsChanged(set.position, it) },
                                    toggleRpePicker = { toggleRpePicker(set.position, it) },
                                    onCustomSetTypeChanged = { setType ->
                                        onCustomSetTypeChanged(
                                            set.position,
                                            setType
                                        )
                                    },
                                )
                                previousSetType = SetType.MYOREP_SET
                            }
                            is DropSetDto -> {
                                DropSet(
                                    position = set.position,
                                    detailsExpanded = detailsExpanded,
                                    dropPercentage = set.dropPercentage,
                                    rpeTarget = set.rpeTarget,
                                    repRangeBottom = set.repRangeBottom,
                                    repRangeTop = set.repRangeTop,
                                    listState = listState,
                                    onPixelOverflowChanged = onPixelOverflowChanged,
                                    onRepRangeBottomChanged = { onRepRangeBottomChanged(set.position, it) },
                                    onRepRangeTopChanged = { onRepRangeTopChanged(set.position, it) },
                                    toggleRpePicker = { toggleRpePicker(set.position, it) },
                                    togglePercentagePicker = { togglePercentagePicker(set.position, it) },
                                    toggleDetailsExpansion = { toggleDetailsExpansion(set.position) },
                                    isPreviousSetMyoRep = previousSetType == SetType.MYOREP_SET,
                                    onCustomSetTypeChanged = { setType ->
                                        onCustomSetTypeChanged(
                                            set.position,
                                            setType
                                        )
                                    },
                                )
                                previousSetType = SetType.DROP_SET
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(25.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.clickable { onAddSet() },
                    text = "Add Set",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
