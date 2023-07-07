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

private fun getVolumeTypeMap(lifts: List<GenericWorkoutLift>):  HashMap<String, Pair<Int, Boolean>> {
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

private fun getVolumeTypeLabels(lifts: List<GenericWorkoutLift>): List<CharSequence> {
    return getVolumeTypeMap(lifts).map { (volumeType, totalVolume) ->
        val plainVolumeString = "$volumeType: ${totalVolume.first}"
        if(totalVolume.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

fun WorkoutDto.getVolumeTypeLabels(): List<CharSequence> {
    return getVolumeTypeLabels(this.lifts)
}

fun ProgramDto.getVolumeTypeLabels(): List<CharSequence> {
    return getVolumeTypeLabels(
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

fun Long.toMinutesSecondsString(format: String): String = String.format(
    format,
    TimeUnit.MILLISECONDS.toMinutes(this),
    TimeUnit.MILLISECONDS.toSeconds(this) % 60
)
