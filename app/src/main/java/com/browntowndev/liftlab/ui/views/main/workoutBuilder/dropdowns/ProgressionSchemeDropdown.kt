package com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.ui.composables.TextDropdown
import com.browntowndev.liftlab.ui.composables.TextDropdownTextAnchor
import com.browntowndev.liftlab.ui.extensions.displayName
import com.browntowndev.liftlab.ui.extensions.shortDisplayName


@Composable
fun ProgressionSchemeDropdown(
    modifier: Modifier = Modifier,
    text: String,
    hasCustomSets: Boolean,
    progressionSchemes: List<ProgressionScheme>,
    onChangeProgressionScheme: (ProgressionScheme) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.then(Modifier.animateContentSize()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = R.drawable.three_bars),
            tint = MaterialTheme.colorScheme.outline,
            contentDescription = null
        )
        TextDropdown(
            modifier = Modifier.padding(start = 4.dp),
            isExpanded = isExpanded,
            onToggleExpansion = { isExpanded = !isExpanded },
            text = text,
            fontSize = 18.sp
        ) {
            progressionSchemes
                .filter {
                    // Show all if no custom sets, otherwise only show if it can have custom sets
                    !hasCustomSets || it.canHaveCustomSets
                }.fastForEach { progressionScheme ->
                    DropdownMenuItem(
                        text = { Text(progressionScheme.displayName()) },
                        onClick = {
                            isExpanded = false
                            onChangeProgressionScheme(progressionScheme)
                        },
                        leadingIcon = {
                            TextDropdownTextAnchor(
                                text = progressionScheme.shortDisplayName(),
                                fontSize = 14.sp
                            )
                        }
                    )
                }
        }
    }
}