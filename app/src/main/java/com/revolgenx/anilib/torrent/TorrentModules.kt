package com.revolgenx.anilib.torrent

import com.revolgenx.anilib.preference.TorrentPreference
import com.revolgenx.anilib.torrent.core.TorrentEngine
import com.revolgenx.anilib.torrent.service.ServiceConnector
import com.revolgenx.anilib.torrent.state.TorrentActiveState
import org.koin.dsl.module

val torrentModules = module {
    //torrent engine
    single { TorrentEngine(get()) }
    single { TorrentActiveState() }

    //service module
    factory { ServiceConnector(get()) }

    single{ TorrentPreference.getTorrentPreferenceInstance(get())}
}