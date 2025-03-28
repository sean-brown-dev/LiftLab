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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.displayNameShort
import com.browntowndev.liftlab.core.common.enums.toDropPercentageString
import com.browntowndev.liftlab.ui.composables.FloatTextField
import com.browntowndev.liftlab.ui.composables.IntegerTextField
import com.browntowndev.liftlab.ui.composables.ScrollableTextField


@Composable
fun DropSet(
    listState: LazyListState,
    detailsExpanded: Boolean,
    position: Int,
    dropPercentage: Float,
    rpeTarget: Float,
    repRangeBottom: Int?,
    repRangeTop: Int?,
    isPreviousSetMyoRep: Boolean,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onConfirmRepRangeBottom: () -> Unit,
    onConfirmRepRangeTop: () -> Unit,
    onCustomSetTypeChanged: (SetType) -> Unit,
    toggleRpePicker: (Boolean) -> Unit,
    togglePercentagePicker: (Boolean) -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit,
    toggleDetailsExpansion: () -> Unit,
) {
    val dropSetDisplayNameShort by remember { mutableStateOf(SetType.DROP_SET.displayNameShort()) }
    val dropSetDisplayName by remember { mutableStateOf(SetType.DROP_SET.displayName()) }
    val standardSetDisplayNameShort = (position + 1).toString()

    CustomSetBase(
        detailsExpanded = detailsExpanded,
        collapsedSetTypeDropdownText = dropSetDisplayNameShort,
        expandedSetTypeDropdownText = dropSetDisplayName,
        standardShortDisplayName = standardSetDisplayNameShort,
        leftSideSummaryText =
        if (repRangeTop != null && repRangeBottom != null) "$repRangeBottom - $repRangeTop reps @$rpeTarget"
        else if (rpeTarget != 10f) "@$rpeTarget"
        else "AMRAP",
        centerIconResourceId = R.drawable.descend_icon,
        rightSideSummaryText = dropPercentage.toDropPercentageString(),
        onCustomSetTypeChanged = onCustomSetTypeChanged,
        toggleExpansion = toggleDetailsExpansion,
        isPreviousSetMyoRep = isPreviousSetMyoRep,
        isFirstSet = position == 0
    ) {
        Column {
            ScrollableTextField(
                value = dropPercentage.toDropPercentageString(),
                vertical = false,
                disableSystemKeyboard = true,
                hideCursor = true,
                label = "Drop Percentage",
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                labelFontSize = 14.sp,
                listState = listState,
                onFocusChanged = togglePercentagePicker,
                onPixelOverflowChanged = onPixelOverflowChanged,
            )
            Spacer(modifier = Modifier.width(2.dp))
            IntegerTextField(
                vertical = false,
                listState = listState,
                value = repRangeBottom!!,
                label = "Rep Range Bottom",
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                labelFontSize = 14.sp,
                onNonNullValueChanged = onRepRangeBottomChanged,
                onPixelOverflowChanged = onPixelOverflowChanged,
                onFocusChanged = {
                    if (!it) onConfirmRepRangeBottom()
                }
            )
            Spacer(modifier = Modifier.width(2.dp))
            IntegerTextField(
                vertical = false,
                listState = listState,
                value = repRangeTop!!,
                label = "Rep Range Top",
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                labelFontSize = 14.sp,
                onNonNullValueChanged = onRepRangeTopChanged,
                onPixelOverflowChanged = onPixelOverflowChanged,
                onFocusChanged = {
                    if (!it) onConfirmRepRangeTop()
                }
            )
            Spacer(modifier = Modifier.width(2.dp))
            FloatTextField(
                vertical = false,
                listState = listState,
                disableSystemKeyboard = true,
                hideCursor = true,
                value = rpeTarget,
                label = "RPE Target",
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                labelFontSize = 14.sp,
                onFocusChanged = toggleRpePicker,
                onPixelOverflowChanged = onPixelOverflowChanged,
            )
        }
    }
}