package com.revolgenx.anilib.event


data class TorrentEngineEvent(val engineEventTypes: TorrentEngineEventTypes) : BaseEvent()
enum class TorrentEngineEventTypes {
    ENGINE_STARTING, ENGINE_STARTED, ENGINE_STOPPING, ENGINE_STOPPED, ENGINE_FAULT
}