package com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet.CustomSetBase
import com.browntowndev.liftlab.ui.views.utils.IntegerTextField
import com.browntowndev.liftlab.ui.views.utils.LabeledCheckBox


@Composable
fun MyoRepSet(
    listState: LazyListState,
    detailsExpanded: Boolean,
    position: Int,
    repFloor: Int,
    repRangeBottom: Int,
    repRangeTop: Int,
    setMatching: Boolean,
    matchSetGoal: Int?,
    onSetMatchingChanged: (enabled: Boolean) -> Unit,
    onMatchSetGoalChanged: (Int) -> Unit,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onRepFloorChanged: (Int) -> Unit,
    onCustomSetTypeChanged: (SetType) -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit,
    toggleDetailsExpansion: () -> Unit,
) {
    var repRangeTopLabel by remember { mutableStateOf(if(!setMatching) "Activation Set Rep Range Top" else "Activation Set Reps") }
    val myoRepSetDisplayNameDisplayNameShort by remember { mutableStateOf(com.browntowndev.liftlab.core.common.enums.SetType.MYOREP_SET.displayNameShort()) }
    val myoRepSetDisplayNameDisplayName by remember { mutableStateOf(com.browntowndev.liftlab.core.common.enums.SetType.MYOREP_SET.displayName()) }
    var standardSetDisplayNameShort = (position + 1).toString()

    LaunchedEffect(setMatching) {
        repRangeTopLabel = if(!setMatching) "Activation Set Rep Range Top" else "Activation Set Reps"
    }

    CustomSetBase(
        detailsExpanded = detailsExpanded,
        collapsedSetTypeDropdownText = myoRepSetDisplayNameDisplayNameShort,
        expandedSetTypeDropdownText = myoRepSetDisplayNameDisplayName,
        standardShortDisplayName = standardSetDisplayNameShort,
        leftSideSummaryText = "Top Set $repRangeBottom - $repRangeTop reps",
        centerIconResourceId = R.drawable.descend_icon,
        rightSideSummaryText = "$repFloor reps",
        onCustomSetTypeChanged = onCustomSetTypeChanged,
        toggleExpansion = toggleDetailsExpansion,
    ) {
        Column {
            LabeledCheckBox(
                label = "Use Set Matching",
                checked = setMatching,
                onCheckedChanged = onSetMatchingChanged
            )
            Spacer(modifier = Modifier.width(2.dp))
            if (!setMatching) {
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
            }
            IntegerTextField(
                vertical = false,
                listState = listState,
                value = repRangeTop,
                label = repRangeTopLabel,
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
                    value = matchSetGoal!!,
                    label = "Match Set Goal",
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    labelFontSize = 14.sp,
                    onValueChanged = onMatchSetGoalChanged,
                    onPixelOverflowChanged = onPixelOverflowChanged,
                )
            }
        }
    }
}