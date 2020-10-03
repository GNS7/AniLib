package com.revolgenx.anilib.app

import com.revolgenx.anilib.repository.databaseModules
import com.revolgenx.anilib.torrent.torrentModules
import org.koin.core.module.Module

class TorrentApp : App() {

    override fun getKoinModules(): List<Module> {
        return super.getKoinModules().toMutableList().also {
            it.add(databaseModules)
            it.add(torrentModules)
        }
    }
}