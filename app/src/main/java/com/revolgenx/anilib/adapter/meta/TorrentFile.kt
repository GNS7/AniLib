package com.revolgenx.anilib.adapter.meta

import com.github.axet.androidlibrary.widgets.TreeListView
import org.libtorrent4j.Priority
import org.libtorrent4j.TorrentHandle

class TorrentFile(var handle: TorrentHandle, var index: Int) : TorrentName() {

    var parent: TorrentFolder? = null
    var node: TreeListView.TreeNode? = null

    var priority: Priority? = null
        set(value) {
            if (value == null) return
            field = value
            handle.filePriority(index, value)
        }

    init {
        handle.torrentFile().files()?.let {
            name = it.fileName(index)
            size = it.fileSize(index)
            path = it.filePath(index)
            priority = handle.filePriority(index)
        }

    }
}