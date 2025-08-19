package com.browntowndev.liftlab.ui.composables

import android.util.Log
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

suspend fun scrollToYLocation(
    listState: LazyListState,
    screenInPixels: Float,
    pickerHeightInPixels: Float,
    screenDensity: Density,
    currentYLocation: () -> Float,
    positionBuffer: Float,
    onPixelOverflowChanged: (Dp) -> Unit,
    onScrollComplete: () -> Unit,
) {
    val bottomOfTextField = with(screenDensity) {
        currentYLocation() + positionBuffer.dp.toPx()
    }

    Log.d(Log.DEBUG.toString(), "currentYLocation: ${currentYLocation()}")
    Log.d(Log.DEBUG.toString(), "bottomOfTextField: $bottomOfTextField")

    val bottomOfTextDistanceFromBottom = (screenInPixels - bottomOfTextField)
    val pixelsToScroll = pickerHeightInPixels - bottomOfTextDistanceFromBottom
    Log.d(Log.DEBUG.toString(), "pixelsToScroll: $pixelsToScroll")

    if (pixelsToScroll > 0) {
        listState.scroll (scrollPriority = MutatePriority.UserInput) {
            val remainingSpace = pixelsToScroll - this.scrollBy(pixelsToScroll)

            if (remainingSpace > 0) {
                Log.d(Log.DEBUG.toString(), "remainingSpace: $remainingSpace")
                onPixelOverflowChanged(Dp(remainingSpace / screenDensity.density))

                var scrolled: Float
                do {
                    scrolled = this.scrollBy(remainingSpace)

                    if (scrolled == 0f)
                        delay(50)
                } while (scrolled == 0f)
            }

            delay(50)
            onScrollComplete()
        }
    } else {
        onScrollComplete()
    }
}