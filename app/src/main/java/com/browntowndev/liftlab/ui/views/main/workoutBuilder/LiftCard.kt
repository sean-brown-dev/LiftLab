package com.browntowndev.liftlab.ui.views.main.workoutBuilder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.ui.views.main.workoutBuilder.dropdowns.LiftDropdown
import kotlinx.coroutines.CoroutineScope


@Composable
fun LiftCard(
    liftName: String,
    category: MovementPattern,
    hasCustomLiftSets: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceMovementPattern: () -> Unit,
    onReplaceLift: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Column (
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 15.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp, pressedElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(8.dp),
        ) {
            LiftCardHeader(
                category = category,
                liftName = liftName,
                hasCustomLiftSets = hasCustomLiftSets,
                onCustomLiftSetsToggled = onCustomLiftSetsToggled,
                onReplaceMovementPattern = onReplaceMovementPattern,
                onReplaceLift = onReplaceLift,
            )
            Spacer(modifier = Modifier.height(15.dp))
            content()
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun LiftCardHeader(
    category: MovementPattern,
    liftName: String,
    hasCustomLiftSets: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceMovementPattern: () -> Unit,
    onReplaceLift: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.padding(15.dp, 5.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.displayName(),
                fontSize = 25.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = liftName,
                overflow = TextOverflow.Ellipsis,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        LiftDropdown(
            hasCustomLiftSets = hasCustomLiftSets,
            onCustomLiftSetsToggled = onCustomLiftSetsToggled,
            onReplaceLift = onReplaceLift,
            onReplaceMovementPattern = onReplaceMovementPattern,
        )
    }
}