package com.browntowndev.liftlab.ui.views.main.liftlibrary.liftdetails

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.views.composables.TextDropdown

@Composable
fun MovementPatternDropdown(
    movementPatternDisplay: String,
    onUpdateMovementPattern: (MovementPattern) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        var isExpanded by remember { mutableStateOf(false) }
        val movementPatternOptions by remember { mutableStateOf(MovementPattern.values().sortedBy { it.displayName() }) }

        Row(
            modifier = Modifier
                .clickable { isExpanded = true }
                .background(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(5.dp))
                .fillMaxWidth()
                .padding(13.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextDropdown(
                isExpanded = isExpanded,
                onToggleExpansion = { isExpanded = !isExpanded },
                text = movementPatternDisplay,
                fontSize = 18.sp
            ) {
                movementPatternOptions.fastForEach { movementPattern ->
                    DropdownMenuItem(
                        text = { Text(movementPattern.displayName()) },
                        onClick = {
                            isExpanded = false
                            onUpdateMovementPattern(movementPattern)
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { isExpanded = true },
                imageVector = Icons.Filled.ArrowDropDown,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
        }
        Text(
            modifier = Modifier.padding(start = 15.dp, top = 5.dp, bottom = 45.dp),
            text = "Movement Pattern",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
        )
    }
}