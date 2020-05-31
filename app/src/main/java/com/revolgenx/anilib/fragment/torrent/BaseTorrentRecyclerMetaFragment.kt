package com.revolgenx.anilib.fragment.torrent

import android.os.Bundle
import android.os.Parcelable
import androidx.recyclerview.widget.*
import com.revolgenx.anilib.R
import kotlinx.android.synthetic.main.torrent_base_recycler_layout.*

open class BaseTorrentRecyclerMetaFragment <T, VH : RecyclerView.ViewHolder> : BaseTorrentMetaFragment() {
    lateinit var adapter: ListAdapter<T, VH>
    private val recyclerStateKey = "recycler_state_key"
    protected lateinit var baseMetaRecyclerView: RecyclerView

    override val layoutRes: Int = R.layout.torrent_base_recycler_layout

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!checkValidity()) return

        baseMetaRecyclerView = torrentMetaRecyclerView
        baseMetaRecyclerView.layoutManager = LinearLayoutManager(context)
        baseMetaRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this.context,
                DividerItemDecoration.VERTICAL
            )
        )
        baseMetaRecyclerView.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
        baseMetaRecyclerView.adapter = adapter
        savedInstanceState?.let {
            it.getParcelable<Parcelable>(recyclerStateKey)?.let { parcel ->
                baseMetaRecyclerView.layoutManager?.onRestoreInstanceState(parcel)
            }
        }
    }

    fun canUpdateView(): Boolean {
        return ::adapter.isInitialized || checkValidity()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(
            recyclerStateKey,
            baseMetaRecyclerView.layoutManager?.onSaveInstanceState()
        )
        super.onSaveInstanceState(outState)
    }

}