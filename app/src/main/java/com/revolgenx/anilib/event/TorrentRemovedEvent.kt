package com.revolgenx.anilib.event

data class TorrentRemovedEvent(var hashes: List<String>, var withFiles: Boolean) : BaseEvent()
