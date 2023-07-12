package com.browntowndev.liftlab.core.common

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
    }
}