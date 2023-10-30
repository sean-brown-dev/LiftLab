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

        fun <T, V> StateFlow<T>.debounce(
            timeoutInMillis: Long,
            coroutineScope: CoroutineScope,
            searchFunc: (state: T) -> V,
        ): StateFlow<V> {
            val debouncedValue = MutableStateFlow(searchFunc(value))

            coroutineScope.launch {
                collect { value ->
                    val newSearchValue: V = searchFunc(value)
                    if (newSearchValue != debouncedValue.value) {
                        coroutineContext.job.cancelChildren()
                        delay(timeoutInMillis)
                        debouncedValue.value = newSearchValue
                    }
                }
            }

            return debouncedValue
        }
    }
}