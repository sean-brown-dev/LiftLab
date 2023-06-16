package com.browntowndev.liftlab.ui.views.utils

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

suspend fun scrollToYLocation(
    listState: LazyListState,
    screenInPixels: Float,
    pickerHeightInPixels: Float,
    screenDensity: Density,
    currentYLocation: () -> Float,
    positionBuffer: Float,
    onPixelOverflowChanged: (Dp) -> Unit,
) {
    val topOfPickerInPixels = screenInPixels - pickerHeightInPixels
    var bottomOfTextField = with(screenDensity) {
        currentYLocation() + positionBuffer.dp.toPx()
    }

    Log.d(Log.DEBUG.toString(), "currentYLocation: ${currentYLocation()}")
    Log.d(Log.DEBUG.toString(), "bottomOfTextField: $bottomOfTextField")
    if (topOfPickerInPixels < bottomOfTextField) {
        var bottomOfTextDistanceFromBottom = (screenInPixels - bottomOfTextField)
        val pixelsToScroll = pickerHeightInPixels - bottomOfTextDistanceFromBottom
        Log.d(Log.DEBUG.toString(), "pixelsToScroll: $pixelsToScroll")
        listState.scroll {
            if (pixelsToScroll > 0) {
                this.scrollBy(pixelsToScroll)
                bottomOfTextField = with(screenDensity) {
                    currentYLocation() + positionBuffer.dp.toPx()
                }
                bottomOfTextDistanceFromBottom = (screenInPixels - bottomOfTextField)
                Log.d(Log.DEBUG.toString(), "AFTER SCROLL currentYLocation: ${currentYLocation()}")
                Log.d(Log.DEBUG.toString(), "AFTER SCROLL bottomOfTextField: $bottomOfTextField")
            }
            if (topOfPickerInPixels < bottomOfTextField) {
                val pixelOverflow = pickerHeightInPixels - bottomOfTextDistanceFromBottom
                val dpOverflow = Dp(pixelOverflow / screenDensity.density)
                onPixelOverflowChanged(dpOverflow)

                Log.d(Log.DEBUG.toString(), "pixelOverflow: $pixelOverflow")
            }
        }
    }
}