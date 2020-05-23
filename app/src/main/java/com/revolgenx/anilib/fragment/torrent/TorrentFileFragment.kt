package com.revolgenx.anilib.fragment.torrent

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import com.revolgenx.anilib.R
import com.revolgenx.anilib.adapter.FilesTreeAdapter
import com.revolgenx.anilib.torrent.core.TorrentProgressListener
import kotlinx.android.synthetic.main.torrent_file_header_layout.*
import kotlinx.android.synthetic.main.torrent_file_meta_layout.*

class TorrentFileFragment : BaseTorrentMetaFragment(), TorrentProgressListener {

    override val layoutRes: Int = R.layout.torrent_file_meta_layout
    private lateinit var adapter: FilesTreeAdapter


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!checkValidity())
            return

        adapter = FilesTreeAdapter(torrent.handle!!) {
            torrentMetaTotalSizeTv.text = it
        }
        adapter.setHasStableIds(true)
        torrent.addListener(this)
        val div = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        treeRecyclerView.addItemDecoration(div)
        treeRecyclerView.adapter = adapter
        torrentMetaFileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            adapter.checkAll(isChecked)
        }
        updateView()
    }

    private fun updateView() {
        if (!checkValidity()) return

        if (!torrent.handle!!.status().hasMetadata()) return

        if (adapter.folders.isEmpty()) {
            adapter.update {
                adapter.load()
                adapter.updateTotal()
            }
        } else {
            treeRecyclerView.post {
                adapter.updateItems()
            }
        }

    }

    override fun invoke() {
        updateView()
    }

    override fun onDestroy() {
        if (checkValidity()) {
            torrent.removeEngineListener()
            torrent.removeListener(this)
        }
        super.onDestroy()
    }


}