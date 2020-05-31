package com.revolgenx.anilib.repository.db.converter

import androidx.room.TypeConverter
import org.libtorrent4j.Priority

class TorrentPriorityConverter {

    @TypeConverter
    fun fromPriority(priorities: Array<Priority>?): String?{
        return priorities?.joinToString { it.ordinal.toString() }
    }

    @TypeConverter
    fun fromString(priorities: String?): Array<Priority>? {
        return priorities?.split(",")?.filter { it.isNotEmpty() }?.map { Priority.values()[it.trim().toInt()] }?.toTypedArray()
    }

}