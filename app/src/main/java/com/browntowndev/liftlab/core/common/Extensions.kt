package com.browntowndev.liftlab.core.common

import android.content.BroadcastReceiver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.getVolumeTypes
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormat.getDateInstance
import java.text.DateFormat.getDateTimeInstance
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt

fun String.appendSuperscript(
    superscript: String,
    superscriptSize: TextUnit = 10.sp,
): AnnotatedString {
    val currentString = this
    return buildAnnotatedString {
        val appendedString = "$currentString$superscript"
        append(appendedString)
        addStyle(
            style = SpanStyle(
                fontSize = superscriptSize,
                baselineShift = BaselineShift.Superscript
            ),
            start = currentString.length,
            end = appendedString.length
        )
    }
}

fun String.insertSuperscript(
    superscript: String,
    insertAt: Int,
    superscriptSize: TextUnit = 10.sp,
): AnnotatedString {
    val currentString = this
    return buildAnnotatedString {
        val appendedString = "${currentString.substring(0, insertAt + 1)}$superscript${currentString.substring(insertAt + 1, currentString.length)}"
        append(appendedString)
        addStyle(
            style = SpanStyle(
                fontSize = superscriptSize,
                baselineShift = BaselineShift.Superscript
            ),
            start = insertAt + 1,
            end = insertAt + 1 + superscript.length
        )
    }
}

private fun getVolumeTypeMapForGenericWorkoutLifts(lifts: List<GenericWorkoutLift>, impact: VolumeTypeImpact):  HashMap<String, Pair<Int, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Int, Boolean>>()
    lifts.fastForEach { lift ->
        val volumeTypes = when(impact) {
            VolumeTypeImpact.PRIMARY -> lift.liftVolumeTypes
            VolumeTypeImpact.SECONDARY -> lift.liftSecondaryVolumeTypes
            VolumeTypeImpact.COMBINED -> lift.liftVolumeTypes + (lift.liftSecondaryVolumeTypes ?: 0)
        }

        volumeTypes?.getVolumeTypes()?.fastForEach { volumeType ->
            val displayName = volumeType.displayName()
            val currTotalVolume: Pair<Int, Boolean>? = volumeCounts.getOrDefault(displayName, null)
            val hasMyoReps = (lift as? CustomWorkoutLiftDto)?.customLiftSets?.any { it is MyoRepSetDto } ?: false
            var newTotalVolume: Int = lift.setCount

            if (currTotalVolume != null) {
                newTotalVolume += currTotalVolume.first
            }

            volumeCounts[displayName] = Pair(newTotalVolume, hasMyoReps || currTotalVolume?.second ?: false )
        }
    }

    return volumeCounts
}

private fun getVolumeTypeMapForLoggingWorkoutLifts(lifts: List<LoggingWorkoutLiftDto>, impact: VolumeTypeImpact):  HashMap<String, Pair<Int, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Int, Boolean>>()
    lifts.fastForEach { lift ->
        val volumeTypes = when(impact) {
            VolumeTypeImpact.PRIMARY -> lift.liftVolumeTypes
            VolumeTypeImpact.SECONDARY -> lift.liftSecondaryVolumeTypes
            VolumeTypeImpact.COMBINED -> lift.liftVolumeTypes + (lift.liftSecondaryVolumeTypes ?: 0)
        }

        volumeTypes?.getVolumeTypes()?.fastForEach { volumeType ->
            val displayName = volumeType.displayName()
            val currTotalVolume: Pair<Int, Boolean>? = volumeCounts.getOrDefault(displayName, null)
            val hasMyoReps = lift.sets.any { it is LoggingMyoRepSetDto }
            var newTotalVolume: Int = lift.setCount

            if (currTotalVolume != null) {
                newTotalVolume += currTotalVolume.first
            }

            volumeCounts[displayName] = Pair(newTotalVolume, hasMyoReps || currTotalVolume?.second ?: false )
        }
    }

    return volumeCounts
}

private fun getVolumeTypeLabelsForGenericWorkoutLifts(lifts: List<GenericWorkoutLift>, impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeMapForGenericWorkoutLifts(lifts, impact).map { (volumeType, totalVolume) ->
        val plainVolumeString = "$volumeType: ${totalVolume.first}"
        if(totalVolume.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

fun WorkoutDto.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForGenericWorkoutLifts(this.lifts, impact)
}

private fun getVolumeTypeLabelsForLoggingWorkoutLifts(lifts: List<LoggingWorkoutLiftDto>, impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeMapForLoggingWorkoutLifts(lifts, impact).map { (volumeType, totalVolume) ->
        val plainVolumeString = "$volumeType: ${totalVolume.first}"
        if(totalVolume.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

fun LoggingWorkoutDto.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForLoggingWorkoutLifts(this.lifts, impact)
}

fun ProgramDto.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForGenericWorkoutLifts(
        lifts = this.workouts.flatMap { workout ->
            workout.lifts
        },
        impact = impact,
    )
}

fun Float.isWholeNumber(): Boolean {
    return this % 1.0 == 0.0
}

fun Float.toFloorAndCeiling(): Iterable<Int> {
    return if (this.isWholeNumber()) {
        listOf(this.roundToInt())
    } else {
        val roundedDown = this.toInt()
        val roundedUp = roundedDown + 1

        roundedDown..roundedUp
    }
}

fun Double.roundToNearestFactor(factor: Float): Float {
    return abs((this / factor).roundToInt()) * factor
}

fun Float.roundToNearestFactor(factor: Float): Float {
    return if (factor != 0f) abs((this / factor).roundToInt()) * factor else this
}

fun Long.toTimeString(): String {
    // TODO: Unit Tests
    return if (this < TEN_MINUTES_IN_MILLIS) {
        String.format(
            SINGLE_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    }
    else if (this < ONE_HOUR_IN_MILLIS ) {
        String.format(
            DOUBLE_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    }
    else if (this < TEN_HOURS_IN_MILLIS) {
        String.format(
            SINGLE_HOURS_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toHours(this),
            TimeUnit.MILLISECONDS.toMinutes(this) % 60,
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    }
    else if (this < TWENTY_FOUR_HOURS_IN_MILLIS) {
        String.format(
            DOUBLE_HOURS_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toHours(this),
            TimeUnit.MILLISECONDS.toMinutes(this) % 60,
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    } else if (this < NINETY_NINE_DAYS_IN_MILLIS) {
        String.format(
            DAYS_HOURS_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toDays(this),
            TimeUnit.MILLISECONDS.toHours(this) % 24,
            TimeUnit.MILLISECONDS.toMinutes(this) % 60,
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    } else {
        return ">99 Days"
    }
}

fun Long.toDate(): Date {
    return Date(this)
}

fun LocalDate.toStartOfDate(): Date {
    return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

fun LocalDate.toEndOfDate(): Date {
    val endOfDay = this.atTime(23, 59).atZone(ZoneId.systemDefault())
    return Date.from(endOfDay.toInstant())
}

fun Date.toSimpleDateString(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val formatter = getDateInstance()
    formatter.timeZone = TimeZone.getTimeZone(zoneId)
    return formatter.format(this)
}

fun Date.toSimpleDateTimeString(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val formatter = getDateTimeInstance()
    formatter.timeZone = TimeZone.getTimeZone(zoneId)
    return formatter.format(this)
}

fun Date.toLocalDate(): LocalDate {
    return this.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

fun BroadcastReceiver.executeInCoroutineScope(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) {
    val pendingResult = goAsync()
    @OptIn(DelicateCoroutinesApi::class) // Must run globally; there's no teardown callback.
    GlobalScope.launch(context) {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}

fun Job.runOnCompletion(action: () -> Unit) {
    invokeOnCompletion { action() }
}
