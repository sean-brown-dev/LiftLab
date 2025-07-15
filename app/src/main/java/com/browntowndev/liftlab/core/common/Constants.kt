package com.browntowndev.liftlab.core.common

import android.os.Environment
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import kotlin.time.Duration.Companion.milliseconds


// Time
const val ONE_MINUTE_IN_MILLIS = 60000L
const val TEN_MINUTES_IN_MILLIS = 10 * ONE_MINUTE_IN_MILLIS
const val ONE_HOUR_IN_MILLIS = 60 * ONE_MINUTE_IN_MILLIS
const val TEN_HOURS_IN_MILLIS = 10 * ONE_HOUR_IN_MILLIS
const val TWENTY_FOUR_HOURS_IN_MILLIS = 24 * ONE_HOUR_IN_MILLIS
const val NINETY_NINE_DAYS_IN_MILLIS = 99 * TWENTY_FOUR_HOURS_IN_MILLIS
const val MAX_TIME_IN_WHOLE_MILLISECONDS = 9223372036854775000L
const val SINGLE_MINUTES_SECONDS_FORMAT = "%01d:%02d"
const val DOUBLE_MINUTES_SECONDS_FORMAT = "%02d:%02d"
const val SINGLE_HOURS_MINUTES_SECONDS_FORMAT = "%01d:%02d:%02d"
const val DOUBLE_HOURS_MINUTES_SECONDS_FORMAT = "%02d:%02d:%02d"
const val DAYS_HOURS_MINUTES_SECONDS_FORMAT = "%dd:%02d:%02d:%02d"

// Increment
val INCREMENT_OPTIONS = listOf(.5f, 1f, 2.5f, 5f, 10f, 20f)
val REST_TIME_RANGE = 0..6L

// Deload
val DELOAD_WEEK_OPTIONS = listOf(3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f)
val NULLABLE_DELOAD_WEEK_OPTIONS =
    DELOAD_WEEK_OPTIONS
        .toMutableList<Float?>()
        .apply { add(0, null) }

// Navigation
const val LIFT_METRIC_CHART_IDS = "liftMetricChartIds"
const val SHOW_WORKOUT_LOG = "showWorkoutLog"

// Donation
const val THANK_YOU_DIALOG_BODY = "Thank you for your support!"

// Firebase
object FirebaseConstants {
    const val CUSTOM_LIFT_SETS_COLLECTION = "customLiftSets"
    const val HISTORICAL_WORKOUT_NAMES_COLLECTION = "historicalWorkoutNames"
    const val LIFT_METRIC_CHARTS_COLLECTION = "liftMetricCharts"
    const val LIFTS_COLLECTION = "lifts"
    const val PREVIOUS_SET_RESULTS_COLLECTION = "previousSetResults"
    const val PROGRAMS_COLLECTION = "programs"
    const val REST_TIMER_IN_PROGRESS_COLLECTION = "restTimerInProgress"
    const val SET_LOG_ENTRIES_COLLECTION = "setLogEntries"
    const val VOLUME_METRIC_CHARTS_COLLECTION = "volumeMetricCharts"
    const val WORKOUT_IN_PROGRESS_COLLECTION = "workoutInProgress"
    const val WORKOUT_LIFTS_COLLECTION = "workoutLifts"
    const val WORKOUT_LOG_ENTRIES_COLLECTION = "workoutLogEntries"
    const val WORKOUTS_COLLECTION = "workouts"
}
