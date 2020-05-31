package com.revolgenx.anilib.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import org.libtorrent4j.TorrentHandle

class AddTorrentViewModel : ViewModel() {
    var handle: TorrentHandle? = null
    lateinit var uri: Uri
    lateinit var path:String
}
