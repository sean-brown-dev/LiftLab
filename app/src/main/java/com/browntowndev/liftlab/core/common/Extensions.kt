package com.browntowndev.liftlab.core.common

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.common.enums.displayName
import com.browntowndev.liftlab.core.common.enums.getVolumeTypes
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormat.LONG
import java.text.DateFormat.MEDIUM
import java.text.DateFormat.SHORT
import java.text.DateFormat.getDateInstance
import java.text.DateFormat.getDateTimeInstance
import java.text.DateFormat.getTimeInstance
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale.US
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration

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

private fun getVolumeTypeMapForGenericWorkoutLifts(lifts: List<GenericWorkoutLift>, impact: VolumeTypeImpact):  HashMap<String, Pair<Float, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Float, Boolean>>()
    lifts.fastForEach { lift ->
        val volumeTypes = when(impact) {
            VolumeTypeImpact.PRIMARY -> lift.liftVolumeTypes
            VolumeTypeImpact.SECONDARY -> lift.liftSecondaryVolumeTypes
            VolumeTypeImpact.COMBINED -> lift.liftVolumeTypes + (lift.liftSecondaryVolumeTypes ?: 0)
        }
        val secondaryVolumeTypes = lift.liftSecondaryVolumeTypes?.getVolumeTypes()?.toHashSet()

        volumeTypes?.getVolumeTypes()?.fastForEach { volumeType ->
            val displayName = volumeType.displayName()
            val currTotalVolume: Pair<Float, Boolean>? = volumeCounts.getOrDefault(displayName, null)
            val hasMyoReps = (lift as? CustomWorkoutLiftDto)?.customLiftSets?.any { it is MyoRepSetDto } ?: false
            var newTotalVolume: Float = if(secondaryVolumeTypes?.contains(volumeType) == true)
                lift.setCount / 2f
            else
                lift.setCount.toFloat()

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
        val volume = if (totalVolume.first % 1.0 == 0.0) {
            String.format(US, "%.0f", totalVolume.first) // No decimals if the value is a whole number
        } else {
            String.format(US, "%.1f", totalVolume.first) // One decimal if there is a non-zero decimal part
        }

        val plainVolumeString = "$volumeType: $volume"
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

fun Double.isWholeNumber(): Boolean {
    return this % 1.0 == 0.0
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

fun Float.toWholeNumberOrOneDecimalString() =
    if (this % 1 == 0f) {
        String.format(locale = US, format = "%.0f", this) // Format as whole number if no decimal places
    } else {
        String.format(locale = US, format = "%.1f", this) // Format with 1 decimal place if there are non-zero decimals
    }

fun Duration.toTimeString(): String =
    "${this.inWholeMinutes}:${
        String.format(locale = US, format = "%02d", this.inWholeSeconds % 60)
    }"

fun Long.toTimeString(): String {
    // TODO: Unit Tests
    return if (this < TEN_MINUTES_IN_MILLIS) {
        String.format(
            locale = US,
            format = SINGLE_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    }
    else if (this < ONE_HOUR_IN_MILLIS ) {
        String.format(
            locale = US,
            format = DOUBLE_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    }
    else if (this < TEN_HOURS_IN_MILLIS) {
        String.format(
            locale = US,
            format = SINGLE_HOURS_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toHours(this),
            TimeUnit.MILLISECONDS.toMinutes(this) % 60,
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    }
    else if (this < TWENTY_FOUR_HOURS_IN_MILLIS) {
        String.format(
            locale = US,
            format = DOUBLE_HOURS_MINUTES_SECONDS_FORMAT,
            TimeUnit.MILLISECONDS.toHours(this),
            TimeUnit.MILLISECONDS.toMinutes(this) % 60,
            TimeUnit.MILLISECONDS.toSeconds(this) % 60
        )
    } else if (this < NINETY_NINE_DAYS_IN_MILLIS) {
        String.format(
            locale = US,
            format = DAYS_HOURS_MINUTES_SECONDS_FORMAT,
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

fun Date.toShortTimeString(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val formatter = getTimeInstance(SHORT)
    formatter.timeZone = TimeZone.getTimeZone(zoneId)
    return formatter.format(this)
}

fun Date.toMediumDateString(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val formatter = getDateInstance(MEDIUM)
    formatter.timeZone = TimeZone.getTimeZone(zoneId)
    return formatter.format(this)
}

fun Date.toSimpleDateTimeString(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val formatter = getDateTimeInstance(LONG, SHORT)
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

fun SpannableStringBuilder.appendCompat(
    text: CharSequence,
    what: Any,
    flags: Int,
): SpannableStringBuilder =
    append(text, what, flags)

fun <T> Iterable<T>.transformToSpannable(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "…",
    transform: SpannableStringBuilder.(T) -> Unit,
): Spannable {
    val buffer = SpannableStringBuilder()
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) buffer.transform(element) else break
    }
    if (limit in 0..<count) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

private const val ALPHA_BIT_SHIFT = 24
private const val RED_BIT_SHIFT = 16
private const val GREEN_BIT_SHIFT = 8
private const val BLUE_BIT_SHIFT = 0
private const val COLOR_MASK = 0xff
internal const val MAX_HEX_VALUE = 255f

fun Int.copyColor(
    alpha: Int = this.extractColorChannel(ALPHA_BIT_SHIFT),
    red: Int = this.extractColorChannel(RED_BIT_SHIFT),
    green: Int = this.extractColorChannel(GREEN_BIT_SHIFT),
    blue: Int = this.extractColorChannel(BLUE_BIT_SHIFT),
): Int =
    alpha shl ALPHA_BIT_SHIFT or
            (red shl RED_BIT_SHIFT) or
            (green shl GREEN_BIT_SHIFT) or
            (blue shl BLUE_BIT_SHIFT)

fun Int.copyColor(
    alpha: Float = this.extractColorChannel(ALPHA_BIT_SHIFT) / MAX_HEX_VALUE,
    red: Float = this.extractColorChannel(RED_BIT_SHIFT) / MAX_HEX_VALUE,
    green: Float = this.extractColorChannel(GREEN_BIT_SHIFT) / MAX_HEX_VALUE,
    blue: Float = this.extractColorChannel(BLUE_BIT_SHIFT) / MAX_HEX_VALUE,
): Int =
    copyColor(
        alpha = (alpha * MAX_HEX_VALUE).toInt(),
        red = (red * MAX_HEX_VALUE).toInt(),
        green = (green * MAX_HEX_VALUE).toInt(),
        blue = (blue * MAX_HEX_VALUE).toInt(),
    )

internal val Int.alpha: Int
    get() = extractColorChannel(ALPHA_BIT_SHIFT)

private fun Int.extractColorChannel(bitShift: Int): Int = this shr bitShift and COLOR_MASK

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }

    return null
}

fun Int.toFriendlyMessage(): String {
    return when (this) {
        BillingResponseCode.USER_CANCELED -> "The donation was cancelled."
        BillingResponseCode.ITEM_ALREADY_OWNED -> "You already have an active subscription. Thank you!"
        BillingResponseCode.ITEM_UNAVAILABLE -> "This item is not available."
        BillingResponseCode.ERROR -> "An error occurred processing the donation."
        BillingResponseCode.BILLING_UNAVAILABLE -> "The Billing Service is unavailable at the moment. Please try again later."
        BillingResponseCode.NETWORK_ERROR,
        BillingResponseCode.SERVICE_DISCONNECTED,
        BillingResponseCode.SERVICE_UNAVAILABLE -> "There was a network issue processing the donation. Please try again later."
        else -> "An error occurred during the purchase."
    }
}

fun GenericLoggingSet.copyGeneric(
    position: Int = this.position,
    myoRepSetPosition: Int? = (this as? LoggingMyoRepSetDto)?.myoRepSetPosition,
    rpeTarget: Float = this.rpeTarget,
    repRangeBottom: Int? = this.repRangeBottom,
    repRangeTop: Int? = this.repRangeTop,
    weightRecommendation: Float? = this.weightRecommendation,
    hadInitialWeightRecommendation: Boolean = this.hadInitialWeightRecommendation,
    previousSetResultLabel: String = this.previousSetResultLabel,
    repRangePlaceholder: String = this.repRangePlaceholder,
    setNumberLabel: String = this.setNumberLabel,
    completedWeight: Float? = this.completedWeight,
    completedReps: Int? = this.completedReps,
    completedRpe: Float? = this.completedRpe,
    complete: Boolean = this.complete
): GenericLoggingSet = when(this) {
    is LoggingStandardSetDto -> this.copy(
        position = position,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom!!,
        repRangeTop = repRangeTop!!,
        weightRecommendation = weightRecommendation,
        hadInitialWeightRecommendation = hadInitialWeightRecommendation,
        previousSetResultLabel = previousSetResultLabel,
        repRangePlaceholder = repRangePlaceholder,
        setNumberLabel = setNumberLabel,
        completedWeight = completedWeight,
        completedReps = completedReps,
        completedRpe = completedRpe,
        complete = complete
    )
    is LoggingMyoRepSetDto -> this.copy(
        position = position,
        myoRepSetPosition = myoRepSetPosition,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
        weightRecommendation = weightRecommendation,
        hadInitialWeightRecommendation = hadInitialWeightRecommendation,
        previousSetResultLabel = previousSetResultLabel,
        repRangePlaceholder = repRangePlaceholder,
        setNumberLabel = setNumberLabel,
        completedWeight = completedWeight,
        completedReps = completedReps,
        completedRpe = completedRpe,
        complete = complete
    )
    is LoggingDropSetDto -> this.copy(
        position = position,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom!!,
        repRangeTop = repRangeTop!!,
        weightRecommendation = weightRecommendation,
        hadInitialWeightRecommendation = hadInitialWeightRecommendation,
        previousSetResultLabel = previousSetResultLabel,
        repRangePlaceholder = repRangePlaceholder,
        setNumberLabel = setNumberLabel,
        completedWeight = completedWeight,
        completedReps = completedReps,
        completedRpe = completedRpe,
        complete = complete
    )
    else -> throw Exception("${this::class.simpleName} is not defined.")
}
