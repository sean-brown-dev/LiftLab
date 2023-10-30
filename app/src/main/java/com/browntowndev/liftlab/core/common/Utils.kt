package com.browntowndev.liftlab.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class Utils {
    companion object {
        fun percentageStringToFloat(percentageString: String): Float {
            return percentageString.removeSuffix("%").toFloat() / 100
        }

        fun getCurrentDate(): Date {
            val localDateTime = LocalDateTime.now()
            val zoneId = ZoneId.systemDefault()
            return Date.from(localDateTime.atZone(zoneId).toInstant())
        }

        fun <T> StateFlow<T>.debounce(
            timeoutInMillis: Long,
            coroutineScope: CoroutineScope
        ): StateFlow<T> {
            val debouncedValue = MutableStateFlow(value)
            coroutineScope.launch {
                collect {value ->
                    coroutineContext.job.cancelChildren()
                    delay(timeoutInMillis)
                    debouncedValue.value = value
                }
            }
            return debouncedValue
        }
    }
}