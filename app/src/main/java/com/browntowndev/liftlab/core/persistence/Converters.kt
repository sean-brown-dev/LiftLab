package com.browntowndev.liftlab.core.persistence

import androidx.room.TypeConverter
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }


    @TypeConverter
    fun fromDuration(duration: Duration): Long {
        return duration.inWholeMilliseconds
    }

    @TypeConverter
    fun toDuration(milliseconds: Long): Duration {
        return milliseconds.milliseconds
    }
}
