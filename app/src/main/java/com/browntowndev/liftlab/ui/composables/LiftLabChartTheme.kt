package com.browntowndev.liftlab.ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.patrykandpatrick.vico.compose.common.VicoTheme

@Composable
fun rememberLiftLabChartTheme(): VicoTheme {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val outlineColor = MaterialTheme.colorScheme.outline

    val theme = remember {
        VicoTheme(
            candlestickCartesianLayerColors = VicoTheme.CandlestickCartesianLayerColors(bullish = primaryColor, bearish = secondaryColor, neutral = tertiaryColor),
            columnCartesianLayerColors = listOf(secondaryColor),
            lineCartesianLayerColors = listOf(primaryColor, tertiaryColor),
            elevationOverlayColor = secondaryColor,
            lineColor = outlineColor,
            textColor = onBackgroundColor
        )
    }

    return theme
}
