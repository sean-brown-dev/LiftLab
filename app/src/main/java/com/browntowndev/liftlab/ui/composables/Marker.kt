package com.browntowndev.liftlab.ui.composables

import android.graphics.Typeface
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.browntowndev.liftlab.core.common.appendCompat
import com.browntowndev.liftlab.core.common.copyColor
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.transformToSpannable
import com.patrykandpatrick.vico.compose.common.component.rememberLayeredComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasureContext
import com.patrykandpatrick.vico.core.cartesian.HorizontalDimensions
import com.patrykandpatrick.vico.core.cartesian.Insets
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.shape.Corner
import com.patrykandpatrick.vico.core.common.shape.DashedShape
import com.patrykandpatrick.vico.core.common.shape.MarkerCorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import kotlin.math.roundToInt

@Composable
internal fun rememberMarker(labelColor: Color = MaterialTheme.colorScheme.primary): CartesianMarker {
    val labelBackgroundColor = MaterialTheme.colorScheme.surface
    val labelBackground = remember(labelBackgroundColor) {
        ShapeComponent(labelBackgroundShape, labelBackgroundColor.toArgb()).setShadow(
            radius = LABEL_BACKGROUND_SHADOW_RADIUS,
            dy = LABEL_BACKGROUND_SHADOW_DY,
            applyElevationOverlay = true,
        )
    }
    val label = rememberTextComponent(
        background = labelBackground,
        lineCount = LABEL_LINE_COUNT,
        padding = labelPadding,
        typeface = Typeface.MONOSPACE,
    )
    val indicatorInnerComponent = rememberShapeComponent(Shape.Pill, MaterialTheme.colorScheme.surface)
    val indicatorCenterComponent = rememberShapeComponent(Shape.Pill, MaterialTheme.colorScheme.surface)
    val indicatorOuterComponent = rememberShapeComponent(Shape.Pill, Color.White)
    val indicator = rememberLayeredComponent(
        rear = indicatorOuterComponent,
        front = rememberLayeredComponent(
            rear = indicatorCenterComponent,
            front = indicatorInnerComponent,
            padding = Dimensions(allDp = indicatorInnerAndCenterComponentPaddingValue.value)
        ),
        padding = Dimensions(allDp = indicatorCenterAndOuterComponentPaddingValue.value)
    )
    val guideline = rememberLineComponent(
        MaterialTheme.colorScheme.onSurface.copy(GUIDELINE_ALPHA),
        guidelineThickness,
        guidelineShape,
    )
    return remember(label, indicator, guideline) {
        object : DefaultCartesianMarker(
            label = label,
            labelPosition = LabelPosition.Top,
            indicator = indicator,
            guideline = guideline,
            indicatorSizeDp = INDICATOR_SIZE_DP,
            setIndicatorColor = { argbColor: Int ->
                indicatorInnerComponent.color = argbColor.copyColor(alpha = INDICATOR_OUTER_COMPONENT_ALPHA)
                indicatorCenterComponent.color = argbColor
                indicatorCenterComponent.setShadow(radius = INDICATOR_CENTER_COMPONENT_SHADOW_RADIUS, color = argbColor)
            },
            valueFormatter = object: CartesianMarkerValueFormatter {
                private val PATTERN = "%.02f"
                override fun format(
                    context: CartesianDrawContext,
                    targets: List<CartesianMarker.Target>
                ): CharSequence = targets.transformToSpannable(
                    separator = "  ",
                ) { cartesianTarget ->
                    val y = when(cartesianTarget) {
                        is LineCartesianLayerMarkerTarget -> cartesianTarget.points.firstOrNull()?.entry?.y ?: 0f
                        is ColumnCartesianLayerMarkerTarget -> cartesianTarget.columns.firstOrNull()?.entry?.y ?: 0f
                        else -> 0f
                    }

                    val color = when(context.chartValues.model.models[0]) {
                        is LineCartesianLayerModel -> (cartesianTarget as LineCartesianLayerMarkerTarget).points[0].color
                        is ColumnCartesianLayerModel -> (cartesianTarget as ColumnCartesianLayerMarkerTarget).columns[0].color
                        else -> labelColor.toArgb()
                    }

                    appendCompat(
                        if (y.isWholeNumber()) y.roundToInt().toString()
                        else PATTERN.format(y),
                        ForegroundColorSpan(color),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }
        ) {
            override fun getInsets(
                context: CartesianMeasureContext,
                outInsets: Insets,
                horizontalDimensions: HorizontalDimensions,
            ) {
                with(context) {
                    outInsets.top = (
                            (CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER * LABEL_BACKGROUND_SHADOW_RADIUS_DP)
                                    - LABEL_BACKGROUND_SHADOW_DY_DP
                    ).pixels

                    if (labelPosition == LabelPosition.AroundPoint) return
                    outInsets.top += label.getHeight(context) + labelBackgroundShape.tickSizeDp.pixels
                }
            }
        }
    }
}

private const val LABEL_BACKGROUND_SHADOW_RADIUS = 4f
private const val LABEL_BACKGROUND_SHADOW_DY = 2f
private const val LABEL_LINE_COUNT = 1
private const val GUIDELINE_ALPHA = .2f
private const val INDICATOR_SIZE_DP = 12f
private const val INDICATOR_OUTER_COMPONENT_ALPHA = 32
private const val INDICATOR_CENTER_COMPONENT_SHADOW_RADIUS = 12f
private const val GUIDELINE_DASH_LENGTH_DP = 8f
private const val GUIDELINE_GAP_LENGTH_DP = 4f
private const val LABEL_BACKGROUND_SHADOW_RADIUS_DP = 4f
private const val LABEL_BACKGROUND_SHADOW_DY_DP = 2f
private const val CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER = 1.4f

private val labelBackgroundShape = MarkerCorneredShape(Corner.FullyRounded)
private val labelHorizontalPaddingValue = 8.dp
private val labelVerticalPaddingValue = 4.dp
private val labelPadding = Dimensions(labelHorizontalPaddingValue.value, labelVerticalPaddingValue.value)
private val indicatorInnerAndCenterComponentPaddingValue = 1.dp
private val indicatorCenterAndOuterComponentPaddingValue = 1.dp
private val guidelineThickness = 2.dp
private val guidelineShape = DashedShape(Shape.Pill, GUIDELINE_DASH_LENGTH_DP, GUIDELINE_GAP_LENGTH_DP)