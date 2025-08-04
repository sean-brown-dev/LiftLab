package com.browntowndev.liftlab.core.domain.enums

enum class LiftMetricChartType {
    ESTIMATED_ONE_REP_MAX,
    VOLUME,
    RELATIVE_INTENSITY,
}

fun LiftMetricChartType.displayName(): String {
    return when (this) {
        LiftMetricChartType.ESTIMATED_ONE_REP_MAX -> "Estimated 1RM"
        LiftMetricChartType.VOLUME -> "Volume"
        LiftMetricChartType.RELATIVE_INTENSITY -> "Relative Intensity"
    }
}

fun String.toLiftMetricChartType(): LiftMetricChartType {
    return when (this) {
        LiftMetricChartType.ESTIMATED_ONE_REP_MAX.displayName() -> LiftMetricChartType.ESTIMATED_ONE_REP_MAX
        LiftMetricChartType.VOLUME.displayName() -> LiftMetricChartType.VOLUME
        LiftMetricChartType.RELATIVE_INTENSITY.displayName() -> LiftMetricChartType.RELATIVE_INTENSITY
        else -> throw IllegalArgumentException("Unknown liftEntity metric chart type: $this")
    }
}