package com.browntowndev.liftlab.ui.views.utils

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.R
import com.browntowndev.liftlab.ui.viewmodels.CountdownTimerViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProgressCountdownTimer(
    start: Boolean = false,
    startTimeInMillis: Long? = null,
    onComplete: (ranToCompletion: Boolean) -> Unit,
) {
    val viewModel: CountdownTimerViewModel = koinViewModel {
        parametersOf(
            onComplete,
        )
    }
    val state by viewModel.state.collectAsState()
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    LaunchedEffect(key1 = start) {
        Log.d(Log.DEBUG.toString(), "New Start Time: $startTimeInMillis")
        if (start && startTimeInMillis != null) {
            viewModel.start(startTimeInMillis)
        }
    }

    AnimatedVisibility(
        visible = state.running,
        enter = expandHorizontally(
            expandFrom = Alignment.Start,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        ),
        exit = shrinkHorizontally(
            shrinkTowards = Alignment.Start,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        ),
    ) {
        Box(
            modifier = Modifier.width(125.dp),
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp)),
                progress = animatedProgress,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(25.dp),
                    painter = painterResource(id = R.drawable.stopwatch_icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = state.timeRemaining)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            viewModel.cancel()
                        },
                    painter = painterResource(id = R.drawable.skip_icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}