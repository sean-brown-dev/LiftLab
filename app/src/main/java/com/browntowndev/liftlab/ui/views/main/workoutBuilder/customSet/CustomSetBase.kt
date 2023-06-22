package com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns.CustomSetTypeDropdown
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun CustomSetBase(
    detailsExpanded: Boolean,
    collapsedSetTypeDropdownText: String? = null,
    expandedSetTypeDropdownText: String,
    standardShortDisplayName: String? = null,
    leftSideSummaryText: String,
    centerIconResourceId: Int,
    rightSideSummaryText: String,
    toggleExpansion: () -> Unit,
    isFirstSet: Boolean,
    isPreviousSetMyoRep: Boolean,
    onCustomSetTypeChanged: ((SetType) -> Unit)? = null,
    content: @Composable (BoxScope.() -> Unit),
) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        if (!detailsExpanded && onCustomSetTypeChanged != null &&
            standardShortDisplayName != null &&
            collapsedSetTypeDropdownText != null
        ) {
            CustomSetTypeDropdown(
                fontSize = 18.sp,
                text = collapsedSetTypeDropdownText,
                standardShortDisplayName = standardShortDisplayName,
                onCustomSetTypeChanged = onCustomSetTypeChanged,
                isFirstSet = isFirstSet,
                isPreviousSetMyoRep = isPreviousSetMyoRep,
            )
            Spacer(Modifier.width(20.dp))
        }
        CustomSetExpandableCard(
            isCollapsed = !detailsExpanded,
            leftSideSummaryText = leftSideSummaryText,
            centerIconResourceId = centerIconResourceId,
            rightSideSummaryText = rightSideSummaryText,
            toggleExpansion = toggleExpansion,
            headerContent = {
                if(onCustomSetTypeChanged != null && standardShortDisplayName != null) {
                    CustomSetTypeDropdown(
                        modifier = Modifier.wrapContentWidth(Alignment.Start),
                        text = expandedSetTypeDropdownText,
                        fontSize = 18.sp,
                        standardShortDisplayName = standardShortDisplayName,
                        isFirstSet = isFirstSet,
                        isPreviousSetMyoRep = isPreviousSetMyoRep,
                        onCustomSetTypeChanged = {
                            coroutineScope.launch {
                                delay(200)
                                onCustomSetTypeChanged(it)
                            }
                        },
                    )
                } else {
                    Text(
                        text = expandedSetTypeDropdownText,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        ) {
            content()
        }
    }
}