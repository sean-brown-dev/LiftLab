package com.browntowndev.liftlab.core.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
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
import java.util.concurrent.TimeUnit
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

private fun getVolumeTypeMapForGenericWorkoutLifts(lifts: List<GenericWorkoutLift>):  HashMap<String, Pair<Int, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Int, Boolean>>()
    lifts.fastForEach { lift ->
        lift.liftVolumeTypes.getVolumeTypes().fastForEach { volumeType ->
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

private fun getVolumeTypeMapForLoggingWorkoutLifts(lifts: List<LoggingWorkoutLiftDto>):  HashMap<String, Pair<Int, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Int, Boolean>>()
    lifts.fastForEach { lift ->
        lift.liftVolumeTypes.getVolumeTypes().fastForEach { volumeType ->
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

private fun getVolumeTypeLabelsForGenericWorkoutLifts(lifts: List<GenericWorkoutLift>): List<CharSequence> {
    return getVolumeTypeMapForGenericWorkoutLifts(lifts).map { (volumeType, totalVolume) ->
        val plainVolumeString = "$volumeType: ${totalVolume.first}"
        if(totalVolume.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

fun WorkoutDto.getVolumeTypeLabels(): List<CharSequence> {
    return getVolumeTypeLabelsForGenericWorkoutLifts(this.lifts)
}

private fun getVolumeTypeLabelsForLoggingWorkoutLifts(lifts: List<LoggingWorkoutLiftDto>): List<CharSequence> {
    return getVolumeTypeMapForLoggingWorkoutLifts(lifts).map { (volumeType, totalVolume) ->
        val plainVolumeString = "$volumeType: ${totalVolume.first}"
        if(totalVolume.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

fun LoggingWorkoutDto.getVolumeTypeLabels(): List<CharSequence> {
    return getVolumeTypeLabelsForLoggingWorkoutLifts(this.lifts)
}

fun ProgramDto.getVolumeTypeLabels(): List<CharSequence> {
    return getVolumeTypeLabelsForGenericWorkoutLifts(
        this.workouts.flatMap { workout ->
            workout.lifts
        }
    )
}

fun Float.isWholeNumber(): Boolean {
    return this.toInt().toFloat() == this
}

fun Float.toFloorAndCeiling(): Iterable<Int> {
    val roundedDown = this.toInt()
    val roundedUp = roundedDown + 1
    return roundedDown..roundedUp
}

fun Double.roundToNearestFactor(factor: Float): Float {
    return abs((this / factor).roundToInt()) * factor
}

fun Float.roundToNearestFactor(factor: Float): Float {
    return abs((this / factor).roundToInt()) * factor
}

fun Long.toTimeString(format: String): String {
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