package com.revolgenx.anilib.repository.db

import com.revolgenx.anilib.repository.db.dao.TorrentDao
import com.revolgenx.anilib.repository.util.Resource
import com.revolgenx.anilib.torrent.core.Torrent
import timber.log.Timber

class TorrentRepository(private val torrentDao: TorrentDao)  :BaseRepository<Torrent> {
    override suspend fun add(obj: Torrent): Resource<Long> {
        return try {

            Resource.success(torrentDao.insert(obj.toEntity()))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", -1)
        }
    }

    override suspend fun remove(obj: Torrent): Resource<Int> {
        return try {
            Resource.success(torrentDao.delete(obj.toEntity()))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", -1)
        }
    }

    override suspend fun update(obj: Torrent): Resource<Int> {
        return try {
            Resource.success(torrentDao.update(obj.toEntity()))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", -1)
        }
    }

    override suspend fun getAll(): Resource<List<Torrent>> {
        return try {
            Resource.success(torrentDao.selectAll().map { it.toTorrent() })
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <E> getAllNotIn(obj: List<E>): Resource<List<Torrent>> {
        return try {
            Resource.success(torrentDao.selectAllNotIn(obj as List<String>).map { it.toTorrent() })
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, null)
        }
    }

    override suspend fun removeAll(obj: List<Torrent>): Resource<Int> {
        return try {
            Resource.success(torrentDao.deleteAll(obj.map { it.toEntity() }))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, -1)
        }
    }

    override suspend fun removeAllWithIds(obj: List<Torrent>): Resource<Int> {
        return try {
            Resource.success(torrentDao.deleteAllWithIds(obj.map { it.hash }))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, -1)
        }
    }

    override suspend fun updateAll(obj: List<Torrent>): Resource<Int> {
        return try {
            Resource.success(torrentDao.updateAll(obj.map { it.toEntity() }))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, -1)
        }
    }

}