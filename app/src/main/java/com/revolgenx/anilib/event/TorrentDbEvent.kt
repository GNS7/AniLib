package com.revolgenx.anilib.event

import com.revolgenx.anilib.torrent.core.Torrent

data class TorrentDbEvent(val torrent: Torrent):BaseEvent()
