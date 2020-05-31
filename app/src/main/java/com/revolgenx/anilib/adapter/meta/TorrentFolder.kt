package com.revolgenx.anilib.adapter.meta

import com.github.axet.androidlibrary.widgets.TreeListView
import org.libtorrent4j.Priority


class TorrentFolder : TorrentName() {
    var node: TreeListView.TreeNode? = null
    var check = true
        get() {
            for (n in node!!.nodes) {
                val m = n.tag as TorrentFile
                if (m.priority == Priority.IGNORE)
                    return false
            }
            return true
        }
        set(value) {
            field = value
            for (n in node!!.nodes) {
                val m = n.tag as TorrentFile
                m.priority = if (value) {
                    Priority.DEFAULT
                } else {
                    Priority.IGNORE
                }
            }
        }
}