package com.browntowndev.liftlab.ui.views.main.workoutBuilder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet.CustomSetBase
import com.browntowndev.liftlab.ui.views.utils.DoubleTextField
import com.browntowndev.liftlab.ui.views.utils.IntegerTextField


@Composable
fun StandardSet(
    listState: LazyListState,
    detailsExpanded: Boolean,
    position: Int,
    rpeTarget: Double,
    repRangeBottom: Int,
    repRangeTop: Int,
    onRepRangeBottomChanged: (Int) -> Unit,
    onRepRangeTopChanged: (Int) -> Unit,
    onCustomSetTypeChanged: (SetType) -> Unit,
    toggleRpePicker: (Boolean) -> Unit,
    toggleDetailsExpansion: () -> Unit,
    onPixelOverflowChanged: (Dp) -> Unit,
) {
    val standardSetDisplayNameShort = (position + 1).toString()
    val standardSetDisplayName = "Set $standardSetDisplayNameShort"

    CustomSetBase(
        detailsExpanded = detailsExpanded,
        collapsedSetTypeDropdownText = standardSetDisplayNameShort,
        expandedSetTypeDropdownText = standardSetDisplayName,
        standardShortDisplayName = standardSetDisplayNameShort,
        leftSideSummaryText = "$repRangeBottom-$repRangeTop",
        centerIconResourceId = R.drawable.at_symbol_icon,
        rightSideSummaryText = if(rpeTarget == 10.0) "AMRAP" else rpeTarget.toString(),
        onCustomSetTypeChanged = onCustomSetTypeChanged,
        toggleExpansion = toggleDetailsExpansion,
    ) {
        Column {
            IntegerTextField(
                vertical = false,
                listState = listState,
                value = repRangeBottom,
                label = "Rep Range Bottom",
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
                label = "Rep Range Top",
                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                labelFontSize = 14.sp,
                onValueChanged = onRepRangeTopChanged,
                onPixelOverflowChanged = onPixelOverflowChanged,
            )
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
        }
    }
}