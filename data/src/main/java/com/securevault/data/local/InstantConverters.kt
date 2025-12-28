package com.securevault.data.local

import androidx.room.TypeConverter
import java.time.Instant

class InstantConverters {
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(epochMillis: Long?): Instant? = epochMillis?.let { Instant.ofEpochMilli(epochMillis) }
}