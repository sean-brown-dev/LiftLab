package com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.displayNameShort
import com.browntowndev.liftlab.ui.views.utils.DoubleTextField
import com.browntowndev.liftlab.ui.views.utils.IntegerTextField
import com.browntowndev.liftlab.ui.views.utils.LabeledCheckBox


@Composable
fun MyoRepSet(
    listState: LazyListState,
    detailsExpanded: Boolean,
    position: Int,
    rpeTarget: Double,
    repRangeBottom: Int,
    repRangeTop: Int,
    setMatching: Boolean,
    matchSetGoal: Int?,
    repFloor: Int?,
    maxSets: Int?,
    isPreviousSetMyoRep: Boolean,
    onSetMatchingChanged: (enabled: Boolean) -> Unit,
    onMatchSetGoalChanged: (Int) -> Unit,
    onMaxSetsChanged: (Int) -> Unit,
    toggleRpePicker: (Boolean) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onRepFloorChanged: (Int) -> Unit,
    onCustomSetTypeChanged: (SetType) -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit,
    toggleDetailsExpansion: () -> Unit,
) {
    val myoRepSetDisplayNameDisplayNameShort by remember { mutableStateOf(SetType.MYOREP_SET.displayNameShort()) }
    val myoRepSetDisplayNameDisplayName by remember { mutableStateOf(SetType.MYOREP_SET.displayName()) }
    val standardSetDisplayNameShort = (position + 1).toString()
    var showMaxSetLimit by remember { mutableStateOf(maxSets != null) }

    CustomSetBase(
        detailsExpanded = detailsExpanded,
        collapsedSetTypeDropdownText = myoRepSetDisplayNameDisplayNameShort,
        expandedSetTypeDropdownText = myoRepSetDisplayNameDisplayName,
        standardShortDisplayName = standardSetDisplayNameShort,
        leftSideSummaryText = "$repRangeBottom - $repRangeTop reps @$rpeTarget",
        centerIconResourceId = R.drawable.descend_icon,
        rightSideSummaryText = if(setMatching) "matched within ${matchSetGoal ?: "?"} sets" else "until ${repFloor ?: "?"} reps",
        onCustomSetTypeChanged = onCustomSetTypeChanged,
        toggleExpansion = toggleDetailsExpansion,
        isPreviousSetMyoRep = isPreviousSetMyoRep,
        isFirstSet = position == 0
    ) {
        Column {
            LabeledCheckBox(
                label = "Use Set Matching",
                checked = setMatching,
                onCheckedChanged = {
                    showMaxSetLimit = false
                    onSetMatchingChanged(it)
                }
            )
            Spacer(modifier = Modifier.width(2.dp))
            if(!setMatching) {
                LabeledCheckBox(
                    label = "Limit Sets Performed",
                    checked = showMaxSetLimit,
                    onCheckedChanged = { showMaxSetLimit = !showMaxSetLimit }
                )
            }
            IntegerTextField(
                vertical = false,
                listState = listState,
                value = repRangeBottom,
                label = "Activation Set Rep Range Bottom",
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                labelFontSize = 14.sp,
                onValueChanged = onRepRangeBottomChanged,
                onPixelOverflowChanged = onPixelOverflowChanged,
            )
            Spacer(modifier = Modifier.width(2.dp))
            IntegerTextField(
                vertical = false,
                listState = listState,
                value = repRangeTop,
                label = "Activation Set Rep Range Top",
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                labelFontSize = 14.sp,
                onValueChanged = onRepRangeTopChanged,
                onPixelOverflowChanged = onPixelOverflowChanged,
            )
            Spacer(modifier = Modifier.width(2.dp))
            if (!setMatching) {
                IntegerTextField(
                    vertical = false,
                    listState = listState,
                    value = repFloor,
                    label = "Rep Floor",
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    labelFontSize = 14.sp,
                    onValueChanged = onRepFloorChanged,
                    onPixelOverflowChanged = onPixelOverflowChanged,
                )
            } else {
                IntegerTextField(
                    vertical = false,
                    listState = listState,
                    value = matchSetGoal,
                    label = "Match Set Goal",
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    labelFontSize = 14.sp,
                    onValueChanged = onMatchSetGoalChanged,
                    onPixelOverflowChanged = onPixelOverflowChanged,
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            DoubleTextField(
                vertical = false,
                listState = listState,
                disableSystemKeyboard = true,
                value = rpeTarget,
                label = "RPE Target",
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                labelFontSize = 14.sp,
                onFocusChanged = toggleRpePicker,
                onPixelOverflowChanged = onPixelOverflowChanged,
            )
            if (showMaxSetLimit && !setMatching) {
                IntegerTextField(
                    vertical = false,
                    listState = listState,
                    value = maxSets,
                    label = "Max Sets to Perform",
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    labelFontSize = 14.sp,
                    onValueChanged = onMaxSetsChanged,
                    onPixelOverflowChanged = onPixelOverflowChanged,
                )
            }
        }
    }
}