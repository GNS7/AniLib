package com.revolgenx.anilib.repository.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.revolgenx.anilib.repository.db.converter.DateConverter
import com.revolgenx.anilib.repository.db.converter.TorrentPriorityConverter
import com.revolgenx.anilib.repository.db.converter.TorrentStatusConverter
import com.revolgenx.anilib.repository.db.dao.TorrentDao
import com.revolgenx.anilib.repository.db.entity.TorrentEntity


@Database(entities = [TorrentEntity::class], version = 1)
@TypeConverters(
    DateConverter::class,
    TorrentStatusConverter::class,
    TorrentPriorityConverter::class
)
abstract class AnilibDatabase : RoomDatabase() {
    abstract fun torrentDao(): TorrentDao
}