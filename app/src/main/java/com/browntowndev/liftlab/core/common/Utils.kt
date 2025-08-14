package com.browntowndev.liftlab.core.common

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date
import kotlin.math.roundToInt

class Utils {
    sealed class General {
        companion object {
            fun percentageStringToFloat(percentageString: String): Float {
                return percentageString.removeSuffix("%").toFloat() / 100
            }

            fun getCurrentDate(): Date {
                val localDateTime = LocalDateTime.now()
                val zoneId = ZoneId.systemDefault()
                return Date.from(localDateTime.atZone(zoneId).toInstant())
            }

            fun getSevenWeeksDateRange(): Pair<Date, Date> {
                val today = LocalDate.now()
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                return monday.minusWeeks(7).toStartOfDate() to today.toEndOfDate()
            }

            /**
             * Rounds a float to one decimal place.
             *
             * @return The rounded float.
             */
            fun Float.roundToOneDecimal(): Float = (this * 10f).roundToInt() / 10f
        }
    }

}