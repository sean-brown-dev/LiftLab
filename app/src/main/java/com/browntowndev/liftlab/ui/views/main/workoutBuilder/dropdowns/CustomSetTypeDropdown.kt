package com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.displayNameShort
import com.browntowndev.liftlab.ui.views.utils.TextDropdown
import com.browntowndev.liftlab.ui.views.utils.TextDropdownTextAnchor


@Composable
fun CustomSetTypeDropdown(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 14.sp,
    standardShortDisplayName: String,
    onCustomSetTypeChanged: (newSetType: SetType) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val setTypes by remember { mutableStateOf(SetType.values().sortedBy { it.displayName() }) }

    TextDropdown(
        modifier = modifier,
        isExpanded = isExpanded,
        onToggleExpansion = { isExpanded = !isExpanded },
        text = text,
        fontSize = fontSize
    ) {
        for (setType in setTypes) {
            DropdownMenuItem(
                text = { Text(setType.displayName()) },
                onClick = {
                    isExpanded = false
                    onCustomSetTypeChanged(setType)
                },
                leadingIcon = {
                    TextDropdownTextAnchor(
                        text = setType.displayNameShort(standardShortDisplayName),
                        fontSize = 14.sp
                    )
                }
            )
        }
    }
}