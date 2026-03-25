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
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.DateFormat.LONG
import java.text.DateFormat.MEDIUM
import java.text.DateFormat.SHORT
import java.text.DateFormat.getDateInstance
import java.text.DateFormat.getDateTimeInstance
import java.text.DateFormat.getTimeInstance
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale.US
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * Rounds a float to one decimal place.
 *
 * @return The rounded float.
 */
fun Float.roundToOneDecimal(): Float = (this * 10f).roundToInt() / 10f

/**
 * Rounds a float to the given number of decimal places.
 *
 * @param decimalPlaces The number of decimal places to round to.
 * @return The rounded float.
 */
fun Float.roundToDecimalPlaces(decimalPlaces: Int): Float {
    val multiplier = 10.0.pow(decimalPlaces.toDouble()).toFloat()
    return (this * multiplier).roundToInt() / multiplier
}

/**
 * Converts a string to a float, rounding it to the given number of decimal places
 * and clamping it to the given range. If the string is NaN, returns null.
 *
 * @param precision The number of decimal places to round to.
 * @param minValue The minimum value to clamp to.
 * @param maxValue The maximum value to clamp to.
 * @return The clamped float, or null if the string is NaN.
 */
fun String.toFloatOrNullWithRoundingAndClamping(
    precision: Int,
    minValue: Float,
    maxValue: Float,
): Float?  =
    this
        .trim()
        .toFloatOrNull()
        ?.roundToDecimalPlaces(precision)
        ?.coerceIn(minValue, maxValue)

/**
 * Converts a string to an integer, clamping it to the given range. If the string is not a
 * valid integer, returns null.
 *
 * @param minValue The minimum value to clamp to.
 * @param maxValue The maximum value to clamp to.
 * @return The clamped integer, or null if the string is not a valid integer.
 */
fun String.toIntOrNullWithClamping(
    minValue: Int,
    maxValue: Int,
): Int? =
    this
        .trim()
        .toIntOrNull()
        ?.coerceIn(minValue, maxValue)

fun Float.toTwoDecimalString(): String {
    val numberFormat = NumberFormat.getNumberInstance()
    numberFormat.maximumFractionDigits = 2

    return if (this.isWholeNumber()) numberFormat.format(this.roundToInt())
    else numberFormat.format(this)
}

fun Pair<Date, Date>.getLastSevenWeeksInRange(): List<Pair<LocalDate, LocalDate>>  {
    return (0..7).map { i ->
        val monday = first.toLocalDate().plusDays(i * 7L)
        val sunday = monday.plusDays(6L)
        monday to sunday
    }
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> {
    return callbackFlow {
        // Create an AuthStateListener. This lambda will be called
        // immediately with the current state and then on any future changes.
        val listener = FirebaseAuth.AuthStateListener { auth ->
            // Offer the latest user object to the flow.
            // This can be the user or null if logged out.
            trySend(auth.currentUser)
        }

        // Register the listener with Firebase Auth
        addAuthStateListener(listener)

        // When the flow is cancelled, unregister the listener
        awaitClose { removeAuthStateListener(listener) }
    }
}

suspend fun<T>  List<T>.forEachParallel(maxDegreesOfParallelism: Int, transform: suspend CoroutineScope.(chunk: List<T>) -> Unit) = coroutineScope {
    if (this@forEachParallel.isEmpty()) return@coroutineScope emptyList()

    val thisList = this@forEachParallel
    val batchSize = thisList.size / maxDegreesOfParallelism +
            if (thisList.size % maxDegreesOfParallelism == 0) 0 else 1

    thisList.chunked(batchSize).map {
        async {
            transform(it)
        }
    }.awaitAll()
}

suspend fun<T>  List<T>.forEachParallel(transform: suspend CoroutineScope.(item: T) -> Unit) = coroutineScope {
    if (this@forEachParallel.isEmpty()) return@coroutineScope emptyList()
    map {
        async {
            transform(it)
        }
    }.awaitAll()
}

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

fun Double.isWholeNumber(): Boolean {
    return this % 1.0 == 0.0
}

fun Float.isWholeNumber(): Boolean {
    return this % 1.0 == 0.0
}

fun Double.roundToNearestFactor(factor: Float): Float {
    return abs((this / factor).roundToInt()) * factor
}

fun Float.roundToNearestFactor(factor: Float): Float {
    return if (factor != 0f) abs((this / factor).roundToInt()) * factor else this
}

fun Float.roundDownToNearestFactor(factor: Float): Float =
    if (factor != 0f) abs(floor(this / factor)) * factor else this

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

/**
 * Launch suspend work from a BroadcastReceiver safely.
 *
 * - Uses goAsync() so onReceive can return immediately.
 * - Finishes the PendingResult no matter what.
 * - Avoids GlobalScope.
 * - Adds an optional timeout (Android expects you to finish quickly).
 */
fun BroadcastReceiver.executeInCoroutineScope(
    context: CoroutineContext = Dispatchers.Default,
    timeoutMs: Long = 9_000L, // keep under the ~10s expectation
    block: suspend CoroutineScope.() -> Unit
) {
    val pending = goAsync()
    val scope = CoroutineScope(SupervisorJob() + context)
    scope.launch {
        try {
            withTimeout(timeoutMs) {
                block()
            }
        } finally {
            pending.finish()
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

/**
 * If patch is set, returns its value, otherwise returns the default value.
 *
 * @param default Default value to return if patch is unset.
 * @return The value of the patch or the default value.
 */
fun <T> Patch<T>.valueOrDefault(default: T): T = when (this) {
    is Patch.Unset -> default
    is Patch.Set -> value
}

fun <T> Patch<T>.overwrite(other: Patch<T>): Patch<T> = when (other) {
    is Patch.Unset -> this
    is Patch.Set -> other
}
