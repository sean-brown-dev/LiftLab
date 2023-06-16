package com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.displayNameShort
import com.browntowndev.liftlab.ui.views.utils.TextDropdown
import com.browntowndev.liftlab.ui.views.utils.TextDropdownTextAnchor


@Composable
fun ProgressionSchemeDropdown(
    modifier: Modifier = Modifier,
    text: String,
    onChangeProgressionScheme: (ProgressionScheme) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val progressionSchemes by remember { mutableStateOf(ProgressionScheme.values().sortedBy { it.displayName() }) }

    Row(
        modifier = Modifier.animateContentSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier.width(10.dp))
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = R.drawable.three_bars),
            tint = MaterialTheme.colorScheme.outline,
            contentDescription = null
        )
        Spacer(modifier.width(4.dp))
        TextDropdown(
            isExpanded = isExpanded,
            onToggleExpansion = { isExpanded = !isExpanded },
            text = text,
            fontSize = 18.sp
        ) {
            for (progressionScheme in progressionSchemes) {
                DropdownMenuItem(
                    text = { Text(progressionScheme.displayName()) },
                    onClick = {
                        isExpanded = false
                        onChangeProgressionScheme(progressionScheme)
                    },
                    leadingIcon = {
                        TextDropdownTextAnchor(
                            text = progressionScheme.displayNameShort(),
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }
    }
}