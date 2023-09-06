package com.browntowndev.liftlab.ui.views.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CircularIcon(
    modifier: Modifier = Modifier,
    size: Dp,
    imageVector: ImageVector,
    circleBackgroundColorScheme: Color,
    iconTint: Color,
) {
    Box(
        modifier = modifier.then(
            Modifier
            .size(size)
            .background(
                color = circleBackgroundColorScheme,
                shape = RoundedCornerShape(20.dp)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            tint = iconTint,
            contentDescription = null,
        )
    }
}