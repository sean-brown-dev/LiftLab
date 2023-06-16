package com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun CustomSetDetails(
    headerContent: @Composable (BoxScope.() -> Unit),
    detailContent: @Composable (BoxScope.() -> Unit),
) {
    Column {
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            headerContent()
        }
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            detailContent()
        }
    }
}