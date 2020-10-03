package com.revolgenx.anilib.viewmodel

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val torrentViewModelModules = module {
    //torrent
    viewModel { TorrentViewModel(get(), get(), get(), get()) }
    viewModel { AddTorrentViewModel() }
}