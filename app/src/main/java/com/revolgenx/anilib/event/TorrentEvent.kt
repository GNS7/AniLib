package com.revolgenx.anilib.event

import com.revolgenx.anilib.torrent.core.Torrent

data class TorrentEvent(val torrents: List<Torrent>, val type: TorrentEventType) : BaseEvent()

enum class TorrentEventType {
    TORRENT_PAUSED, TORRENT_RESUMED, TORRENT_FINISHED, TORRENT_RECHECK, TORRENT_ERROR
}