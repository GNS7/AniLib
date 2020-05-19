package com.revolgenx.anilib.torrent.state

enum class TorrentState {
    PAUSED,
    DOWNLOADING,
    CHECKING,
    QUEUE,
    SEEDING,
    CHECKING_FILES,
    DOWNLOADING_METADATA,
    ALLOCATING,
    CHECKING_RESUME_DATA,
    COMPLETED,
    UNKNOWN
}