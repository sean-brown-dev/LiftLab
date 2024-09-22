package com.browntowndev.liftlab.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@Composable
fun CustomKeyboard(
    modifier: Modifier = Modifier,
    visible: Boolean,
    content: @Composable (ColumnScope.() -> Unit),
) {
    val focusManager = LocalFocusManager.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 100)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 100)
        ) + fadeOut(),
    ) {
        ElevatedCard(
            modifier = modifier.then(
                Modifier
                    .fillMaxWidth()
                    .height(LocalConfiguration.current.screenHeightDp.dp.times(.3f))
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 5.dp
            ),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    modifier = Modifier
                        .padding(top = 10.dp, end = 10.dp)
                        .height(55.dp)
                        .width(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50.dp)
                        ),
                    onClick = {
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(25.dp),
                        imageVector = Icons.Filled.Check,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = null
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))
            }
            content()
        }
    }
}