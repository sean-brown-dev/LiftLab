package com.browntowndev.liftlab.ui.views.main.workoutBuilder.customSet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun CustomSetSummary(
    leftSideSummaryText: String,
    centerIconResourceId: Int,
    rightSideSummaryText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = leftSideSummaryText,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(id = centerIconResourceId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = rightSideSummaryText,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}