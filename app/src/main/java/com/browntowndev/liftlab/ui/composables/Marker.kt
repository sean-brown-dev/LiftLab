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
import androidx.core.graphics.ColorUtils
import com.browntowndev.liftlab.core.common.appendCompat
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.transformToSpannable
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shadow
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerMargins
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.LayeredComponent
import com.patrykandpatrick.vico.core.common.component.Shadow
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Companion.Pill
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Corner
import com.patrykandpatrick.vico.core.common.shape.DashedShape
import com.patrykandpatrick.vico.core.common.shape.MarkerCorneredShape
import kotlin.math.roundToInt

@Composable
internal fun rememberMarker(
    labelColor: Color = MaterialTheme.colorScheme.primary,
    labelPosition: DefaultCartesianMarker.LabelPosition = DefaultCartesianMarker.LabelPosition.Top,
    showIndicator: Boolean = true,
): CartesianMarker {
    val labelBackgroundColor = MaterialTheme.colorScheme.surface
    val labelBackground =
        rememberShapeComponent(
            fill = fill(labelBackgroundColor),
            shape = labelBackgroundShape,
            shadow = shadow(radius = LABEL_BACKGROUND_SHADOW_RADIUS_DP.dp, y = LABEL_BACKGROUND_SHADOW_DY_DP.dp),
        )
    val label = rememberTextComponent(
        background = labelBackground,
        lineCount = LABEL_LINE_COUNT,
        padding = labelPadding,
        typeface = Typeface.MONOSPACE,
    )
    val indicatorFrontComponent = rememberShapeComponent(shape = Pill, fill = fill(MaterialTheme.colorScheme.surface))
    val indicatorInnerComponent = rememberShapeComponent(shape = Pill, fill = fill(MaterialTheme.colorScheme.surface))
    val indicatorCenterComponent = rememberShapeComponent(shape = Pill, fill = fill(MaterialTheme.colorScheme.surface))
    val indicatorOuterComponent = rememberShapeComponent(shape = Pill, fill = fill(Color.White))
    val indicator = LayeredComponent(
        back = indicatorOuterComponent,
        front = LayeredComponent(
            back = indicatorCenterComponent,
            front = indicatorInnerComponent,
            padding = insets(indicatorInnerAndCenterComponentPaddingValue)
        ),
        padding = insets(indicatorCenterAndOuterComponentPaddingValue)
    )
    val guideline = rememberLineComponent(
        fill(MaterialTheme.colorScheme.onSurface.copy(GUIDELINE_ALPHA)),
        guidelineThickness,
        guidelineShape,
    )
    return remember(label, labelPosition, indicator, showIndicator, guideline)  {
        object : DefaultCartesianMarker(
            label = label,
            labelPosition = LabelPosition.Top,
            guideline = guideline,
            indicatorSizeDp = INDICATOR_SIZE_DP,
            indicator = if (showIndicator) {
                { color ->
                    LayeredComponent(
                        back =
                        ShapeComponent(Fill(ColorUtils.setAlphaComponent(color, 38)), Pill),
                        front =
                        LayeredComponent(
                            back =
                            ShapeComponent(
                                fill = Fill(color),
                                shape = Pill,
                                shadow = Shadow(radiusDp = 12f, color = color),
                            ),
                            front = indicatorFrontComponent,
                            padding = insets(5.dp),
                        ),
                        padding = insets(10.dp),
                    )
                }
            } else {
                null
            },
            valueFormatter = object: ValueFormatter {
                private val PATTERN = "%.02f"
                override fun format(
                    context: CartesianDrawingContext,
                    targets: List<CartesianMarker.Target>
                ): CharSequence = targets.transformToSpannable(
                    separator = "  ",
                ) { cartesianTarget ->
                    val y = when(cartesianTarget) {
                        is LineCartesianLayerMarkerTarget -> cartesianTarget.points.firstOrNull()?.entry?.y ?: 0.0
                        is ColumnCartesianLayerMarkerTarget -> cartesianTarget.columns.firstOrNull()?.entry?.y ?: 0.0
                        else -> 0.0
                    }

                    val color = when(context.model.models[0]) {
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
             override fun updateLayerMargins(
                 context: CartesianMeasuringContext,
                 layerMargins: CartesianLayerMargins,
                 layerDimensions: CartesianLayerDimensions,
                 model: CartesianChartModel,
            ) {
                 with(context) {
                     val baseShadowMarginDp =
                         CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER * LABEL_BACKGROUND_SHADOW_RADIUS_DP
                     var topMargin = (baseShadowMarginDp - LABEL_BACKGROUND_SHADOW_DY_DP).pixels
                     var bottomMargin = (baseShadowMarginDp + LABEL_BACKGROUND_SHADOW_DY_DP).pixels
                     when (labelPosition) {
                         LabelPosition.Top,
                         LabelPosition.AbovePoint -> topMargin += label.getHeight(context) + tickSizeDp.pixels
                         LabelPosition.Bottom -> bottomMargin += label.getHeight(context) + tickSizeDp.pixels
                         LabelPosition.AroundPoint -> {}
                     }
                     layerMargins.ensureValuesAtLeast(top = topMargin, bottom = bottomMargin)
                 }
            }
        }
    }
}

private const val LABEL_LINE_COUNT = 1
private const val GUIDELINE_ALPHA = .2f
private const val INDICATOR_SIZE_DP = 12f
private const val GUIDELINE_DASH_LENGTH_DP = 8f
private const val GUIDELINE_GAP_LENGTH_DP = 4f
private const val LABEL_BACKGROUND_SHADOW_RADIUS_DP = 4f
private const val LABEL_BACKGROUND_SHADOW_DY_DP = 2f
private const val CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER = 1.4f

private val labelBackgroundShape = MarkerCorneredShape(Corner.Rounded)
private val labelHorizontalPaddingValue = 8.dp
private val labelVerticalPaddingValue = 4.dp
private val labelPadding = insets(labelHorizontalPaddingValue, labelVerticalPaddingValue)
private val indicatorInnerAndCenterComponentPaddingValue = 1.dp
private val indicatorCenterAndOuterComponentPaddingValue = 1.dp
private val guidelineThickness = 2.dp
private val guidelineShape = DashedShape(Pill, GUIDELINE_DASH_LENGTH_DP, GUIDELINE_GAP_LENGTH_DP)