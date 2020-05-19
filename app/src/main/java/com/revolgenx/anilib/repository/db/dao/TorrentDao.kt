package com.revolgenx.anilib.repository.db.dao

import androidx.room.*
import com.revolgenx.anilib.repository.db.entity.TorrentEntity


@Dao
interface TorrentDao {
    @Insert
    suspend fun insert(entity: TorrentEntity): Long

    @Insert
    suspend fun insertAll(entities: List<TorrentEntity>): List<Long>


    @Update
    suspend fun update(entity: TorrentEntity): Int

    @Update
    suspend fun updateAll(entities: List<TorrentEntity>): Int

    @Delete
    suspend fun delete(entity: TorrentEntity): Int

    @Delete
    suspend fun deleteAll(entities: List<TorrentEntity>): Int

    @Query("SELECT * FROM torrent_table")
    suspend fun selectAll(): List<TorrentEntity>

    @Query("SELECT * FROM torrent_table WHERE hash NOT IN (:ids)")
    suspend fun selectAllNotIn(ids: List<String>): List<TorrentEntity>

    @Query("DELETE FROM torrent_table WHERE hash IN (:ids)")
    suspend fun deleteAllWithIds(ids: List<String>): Int

}