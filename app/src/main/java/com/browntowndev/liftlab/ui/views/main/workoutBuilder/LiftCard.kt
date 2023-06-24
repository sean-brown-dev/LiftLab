package com.browntowndev.liftlab.ui.views.main.workoutBuilder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import kotlin.time.Duration


@Composable
fun LiftCard(
    liftName: String,
    increment: Float,
    restTime: Duration,
    restTimeAppliedAcrossWorkouts: Boolean,
    incrementAppliedAcrossWorkouts: Boolean,
    movementPattern: MovementPattern,
    hasCustomLiftSets: Boolean,
    showCustomSetsOption: Boolean,
    currentDeloadWeek: Int?,
    showDeloadWeekOption: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceLift: () -> Unit,
    onDeleteLift: () -> Unit,
    onChangeDeloadWeek: () -> Unit,
    onChangeIncrement: (Float) -> Unit,
    onChangeRestTime: (newRestTime: Duration, appliedAcrossSets: Boolean) -> Unit,
    onChangeRestTimeAppliedAcrossWorkouts: (Boolean) -> Unit,
    onChangeIncrementAppliedAcrossWorkouts: (Boolean) -> Unit,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Column (
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 5.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp, pressedElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            LiftCardHeader(
                category = movementPattern,
                liftName = liftName,
                increment = increment,
                restTime = restTime,
                restTimeAppliedAcrossWorkouts = restTimeAppliedAcrossWorkouts,
                incrementAppliedAcrossWorkouts = incrementAppliedAcrossWorkouts,
                hasCustomLiftSets = hasCustomLiftSets,
                showCustomSetsOption = showCustomSetsOption,
                currentDeloadWeek = currentDeloadWeek,
                showDeloadWeekOption = showDeloadWeekOption,
                onCustomLiftSetsToggled = onCustomLiftSetsToggled,
                onReplaceLift = onReplaceLift,
                onDeleteLift = onDeleteLift,
                onChangeIncrement = onChangeIncrement,
                onChangeRestTime = onChangeRestTime,
                onChangeDeloadWeek = onChangeDeloadWeek,
                onChangeIncrementAppliedAcrossWorkouts = onChangeIncrementAppliedAcrossWorkouts,
                onChangeRestTimeAppliedAcrossWorkouts = onChangeRestTimeAppliedAcrossWorkouts,
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
    restTime: Duration,
    increment: Float,
    restTimeAppliedAcrossWorkouts: Boolean,
    incrementAppliedAcrossWorkouts: Boolean,
    hasCustomLiftSets: Boolean,
    showCustomSetsOption: Boolean,
    currentDeloadWeek: Int?,
    showDeloadWeekOption: Boolean,
    onCustomLiftSetsToggled: CoroutineScope.(Boolean) -> Unit,
    onReplaceLift: () -> Unit,
    onDeleteLift: () -> Unit,
    onChangeIncrement: (Float) -> Unit,
    onChangeRestTime: (newRestTime: Duration, appliedAcrossSets: Boolean) -> Unit,
    onChangeDeloadWeek: () -> Unit,
    onChangeRestTimeAppliedAcrossWorkouts: (Boolean) -> Unit,
    onChangeIncrementAppliedAcrossWorkouts: (Boolean) -> Unit,
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
            restTime = restTime,
            increment = increment,
            restTimeAppliedAcrossWorkouts = restTimeAppliedAcrossWorkouts,
            incrementAppliedAcrossWorkouts = incrementAppliedAcrossWorkouts,
            hasCustomLiftSets = hasCustomLiftSets,
            showCustomSetsOption = showCustomSetsOption,
            currentDeloadWeek = currentDeloadWeek,
            showDeloadWeekOption = showDeloadWeekOption,
            onCustomLiftSetsToggled = onCustomLiftSetsToggled,
            onReplaceLift = onReplaceLift,
            onDeleteLift = onDeleteLift,
            onChangeIncrement = onChangeIncrement,
            onChangeRestTime = onChangeRestTime,
            onChangeDeloadWeek = onChangeDeloadWeek,
            onChangeIncrementAppliedAcrossWorkouts = onChangeIncrementAppliedAcrossWorkouts,
            onChangeRestTimeAppliedAcrossWorkouts = onChangeRestTimeAppliedAcrossWorkouts,
        )
    }
}