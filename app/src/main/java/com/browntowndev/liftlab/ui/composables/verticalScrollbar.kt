package com.browntowndev.liftlab.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun Modifier.verticalScrollbar(
    lazyListState: LazyListState,
    verticalScrollbarColors: VerticalScrollbarColors = VerticalScrollbarDefaults.colors(),
    onRequestBubbleTextForScrollLocation: (index: Int) -> String,
): Modifier {
    val handleWidth = remember { 3.dp }

    // Alpha value and colors
    val targetAlpha = remember(lazyListState.isScrollInProgress) { if (lazyListState.isScrollInProgress) 1f else 0f }
    val duration = remember(lazyListState.isScrollInProgress) { if (lazyListState.isScrollInProgress) 150 else 500 }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "Scrollbar handle animation"
    )
    val needDrawScrollbar = remember(lazyListState.isScrollInProgress, alpha) { lazyListState.isScrollInProgress || alpha > 0.0f }
    val bubbleColorWithAlpha = remember(alpha) { verticalScrollbarColors.bubbleColor.copy(alpha = alpha)}
    val textColorWithAlpha = remember(alpha) { verticalScrollbarColors.textColor.copy(alpha = alpha)}
    val handleColorWithAlpha = remember(alpha) { verticalScrollbarColors.handleColor.copy(alpha = alpha) }

    // Scroll index values and bubble text
    val layoutInfo by remember { derivedStateOf { lazyListState.layoutInfo } }
    val firstVisibleElementIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    val lastVisibleElementIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    val middleItemIndex = (firstVisibleElementIndex + lastVisibleElementIndex) / 2
    val bubbleText = remember(middleItemIndex) { onRequestBubbleTextForScrollLocation(middleItemIndex) }

    // Scrollbar height and animation
    var elementHeight by remember { mutableFloatStateOf(0f) }
    val scrollbarOffsetY by animateFloatAsState(
        targetValue = firstVisibleElementIndex * elementHeight,
        animationSpec = tween(durationMillis = 100),
        label = "Scrollbar offset animation"
    )
    var isHandleHeightSet by remember { mutableStateOf(false) }
    var handleHeight by remember { mutableFloatStateOf(0f) }

    return drawWithContent {
        drawContent()

        if (needDrawScrollbar) {
            elementHeight = this.size.height / lazyListState.layoutInfo.totalItemsCount
            handleHeight = if (!isHandleHeightSet) lazyListState.layoutInfo.visibleItemsInfo.size * elementHeight else handleHeight
            isHandleHeightSet = true

            // Draw scrollbar
            drawRoundRect(
                color = handleColorWithAlpha,
                cornerRadius = CornerRadius(10f, 10f),
                topLeft = Offset(this.size.width - handleWidth.toPx(), scrollbarOffsetY),
                size = Size(handleWidth.toPx(), handleHeight),
            )

            // Calculate bubble circle size
            val bubbleRadius = handleWidth.toPx() * 6f
            val bubbleCenter = Offset(
                this.size.width - handleWidth.toPx() * 10f,
                scrollbarOffsetY + handleHeight / 2
            )

            // Draw the bubble
            val bubblePath = Path().apply {
                // Add the circle to the path
                addOval(Rect(center = bubbleCenter, radius = bubbleRadius))

                // Calculate the intersection points between the triangle lines and the circle
                val triangleSize = handleWidth.toPx() * 2f
                val triangleBasePoint = Offset(bubbleCenter.x + bubbleRadius + triangleSize, bubbleCenter.y)
                val triangleTopPoint = Offset(bubbleCenter.x, bubbleCenter.y + bubbleRadius)
                val triangleBottomPoint = Offset(bubbleCenter.x, bubbleCenter.y - bubbleRadius)

                val topIntersectionPoint = calculateLineCircleIntersection(triangleBasePoint, triangleTopPoint, bubbleCenter, bubbleRadius)
                val bottomIntersectionPoint = calculateLineCircleIntersection(triangleBasePoint, triangleBottomPoint, bubbleCenter, bubbleRadius)

                // Draw the triangle path
                moveTo(triangleBasePoint.x, triangleBasePoint.y)
                lineTo(topIntersectionPoint.x, topIntersectionPoint.y)
                val startAngleDegrees = atan2(topIntersectionPoint.y - bubbleCenter.y, topIntersectionPoint.x - bubbleCenter.x)
                arcTo(
                    rect = Rect(bubbleCenter.x - bubbleRadius, bubbleCenter.y - bubbleRadius, bubbleCenter.x + bubbleRadius, bubbleCenter.y + bubbleRadius),
                    startAngleDegrees = startAngleDegrees,
                    sweepAngleDegrees = atan2(bottomIntersectionPoint.y - bubbleCenter.y, bottomIntersectionPoint.x - bubbleCenter.x) - startAngleDegrees,
                    forceMoveTo = false
                )
                lineTo(bottomIntersectionPoint.x, bottomIntersectionPoint.y)
                close()
            }

            // Draw the bubble
            drawPath(
                path = bubblePath,
                color = bubbleColorWithAlpha,
            )

            // Draw the text inside the bubble
            val textPaint = android.graphics.Paint().apply {
                textSize = bubbleRadius
                setColor(textColorWithAlpha.value.toLong())
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    bubbleText,
                    bubbleCenter.x - textPaint.measureText(bubbleText) / 2,
                    bubbleCenter.y + (textPaint.textSize * .8f) / 2,
                    textPaint
                )
            }
        }
    }
}

@Immutable
object VerticalScrollbarDefaults {
    @Composable fun colors(): VerticalScrollbarColors = defaultVerticalScrollbarColors

    @Composable fun colors(handleColor: Color, bubbleColor: Color, textColor: Color) =
        defaultVerticalScrollbarColors.copy(
            handleColor = handleColor,
            bubbleColor = bubbleColor,
            textColor = textColor,
        )

    private var defaultColorsCached: VerticalScrollbarColors? = null
    private val defaultVerticalScrollbarColors: VerticalScrollbarColors
        @Composable
        get() = defaultColorsCached ?: VerticalScrollbarColors(
            handleColor = MaterialTheme.colorScheme.outline,
            bubbleColor = MaterialTheme.colorScheme.tertiaryContainer,
            textColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ).also {
            defaultColorsCached = it
            TextFieldDefaults.colors()
        }
}

@Immutable
data class VerticalScrollbarColors(
    val handleColor: Color,
    val bubbleColor: Color,
    val textColor: Color,
)

private fun calculateLineCircleIntersection(
    lineStart: Offset,
    lineEnd: Offset,
    circleCenter: Offset,
    circleRadius: Float
): Offset {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val a = dx * dx + dy * dy
    val b = 2 * (dx * (lineStart.x - circleCenter.x) + dy * (lineStart.y - circleCenter.y))
    val c = (lineStart.x - circleCenter.x) * (lineStart.x - circleCenter.x) +
            (lineStart.y - circleCenter.y) * (lineStart.y - circleCenter.y) -
            circleRadius * circleRadius

    val discriminant = b * b - 4 * a * c
    if (discriminant < 0) {
        // No intersection
        return lineEnd
    }

    val t = (-b - sqrt(discriminant.toDouble())) / (2 * a)
    val intersectionX = lineStart.x + t * dx
    val intersectionY = lineStart.y + t * dy

    return Offset(intersectionX.toFloat(), intersectionY.toFloat())
}