package com.revolgenx.anilib.repository.db.converter

import androidx.room.TypeConverter
import com.revolgenx.anilib.torrent.state.TorrentState

class TorrentStatusConverter {
    @TypeConverter
    fun fromStatus(value: TorrentState) = value.ordinal

    @TypeConverter
    fun fromInt(value: Int) = TorrentState.values()[value]
}