package com.revolgenx.anilib.torrent.state

data class TorrentActiveState(
    var serviceActive: Boolean = false,
    var fragmentActive: Boolean = false
)