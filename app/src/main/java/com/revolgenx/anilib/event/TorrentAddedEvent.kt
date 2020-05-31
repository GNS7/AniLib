package com.revolgenx.anilib.event

import com.revolgenx.anilib.torrent.core.Torrent

data class TorrentAddedEvent(var torrent: Torrent, var type: TorrentAddedEventTypes):BaseEvent()

enum class TorrentAddedEventTypes {
    TORRENT_ADDED, TORRENT_ADD_ERROR
}
