package com.revolgenx.anilib.fragment.torrent

import android.os.Bundle
import com.revolgenx.anilib.fragment.base.BaseLayoutFragment
import com.revolgenx.anilib.torrent.core.Torrent

abstract class BaseTorrentMetaFragment : BaseLayoutFragment() {
    companion object {
        val meta_key = "torrent_meta_key"
    }

    protected lateinit var torrent: Torrent

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.classLoader = Torrent::class.java.classLoader
        torrent = arguments?.getParcelable(meta_key) ?: return
    }

    fun checkValidity() =
        ::torrent.isInitialized && torrent.handle != null && torrent.handle?.isValid == true

}