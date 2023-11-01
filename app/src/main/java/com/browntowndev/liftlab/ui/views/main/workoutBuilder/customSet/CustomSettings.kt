package com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.ui.views.composables.DeleteableOnSwipeLeft


@Composable
fun CustomSettings(
    isVisible: Boolean,
    listState: LazyListState,
    detailExpansionStates: HashSet<Int>,
    customSets: List<GenericLiftSet>,
    onAddSet: () -> Unit,
    onDeleteSet: (position: Int) -> Unit,
    onSetMatchingChanged: (position: Int, enabled: Boolean) -> Unit,
    onMatchSetGoalChanged: (position: Int, newMatchSetGoal: Int) -> Unit,
    onRepRangeBottomChanged: (position: Int, newRepRangeBottom: Int) -> Unit,
    onRepRangeTopChanged: (position: Int, newRepRangeTop: Int) -> Unit,
    onConfirmRepRangeBottom: (position: Int) -> Unit,
    onConfirmRepRangeTop: (position: Int) -> Unit,
    onRepFloorChanged: (position: Int, newRepFloor: Int) -> Unit,
    onCustomSetTypeChanged: (position: Int, newSetType: SetType) -> Unit,
    onMaxSetsChanged: (position: Int, newMaxSets: Int?) -> Unit,
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

                DeleteableOnSwipeLeft(
                    confirmationDialogHeader = "Delete Set?",
                    confirmationDialogBody = "Confirm to delete the set.",
                    onDelete = { onDeleteSet(set.position) },
                ) {
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
                                onConfirmRepRangeBottom = { onConfirmRepRangeBottom(set.position) },
                                onConfirmRepRangeTop = { onConfirmRepRangeTop(set.position) },
                                toggleRpePicker = { toggleRpePicker(set.position, it) },
                                toggleDetailsExpansion = { toggleDetailsExpansion(set.position) },
                                isPreviousSetMyoRep = previousSetType == SetType.MYOREP,
                                onCustomSetTypeChanged = { setType ->
                                    onCustomSetTypeChanged(
                                        set.position,
                                        setType
                                    )
                                },
                            )
                            previousSetType = SetType.STANDARD
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
                                setGoal = set.setGoal,
                                maxSets = set.maxSets,
                                onPixelOverflowChanged = onPixelOverflowChanged,
                                onSetMatchingChanged = { onSetMatchingChanged(set.position, it) },
                                onRepFloorChanged = { onRepFloorChanged(set.position, it) },
                                onRepRangeBottomChanged = { onRepRangeBottomChanged(set.position, it) },
                                onRepRangeTopChanged = { onRepRangeTopChanged(set.position, it) },
                                onConfirmRepRangeBottom = { onConfirmRepRangeBottom(set.position) },
                                onConfirmRepRangeTop = { onConfirmRepRangeTop(set.position) },
                                onMatchSetGoalChanged = { onMatchSetGoalChanged(set.position, it) },
                                toggleDetailsExpansion = { toggleDetailsExpansion(set.position) },
                                isPreviousSetMyoRep = previousSetType == SetType.MYOREP,
                                onMaxSetsChanged = { onMaxSetsChanged(set.position, it) },
                                toggleRpePicker = { toggleRpePicker(set.position, it) },
                                onCustomSetTypeChanged = { setType ->
                                    onCustomSetTypeChanged(
                                        set.position,
                                        setType
                                    )
                                },
                            )
                            previousSetType = SetType.MYOREP
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
                                onConfirmRepRangeBottom = { onConfirmRepRangeBottom(set.position) },
                                onConfirmRepRangeTop = { onConfirmRepRangeTop(set.position) },
                                toggleRpePicker = { toggleRpePicker(set.position, it) },
                                togglePercentagePicker = { togglePercentagePicker(set.position, it) },
                                toggleDetailsExpansion = { toggleDetailsExpansion(set.position) },
                                isPreviousSetMyoRep = previousSetType == SetType.MYOREP,
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
