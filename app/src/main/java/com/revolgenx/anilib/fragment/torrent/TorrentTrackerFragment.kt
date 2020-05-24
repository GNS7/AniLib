package com.revolgenx.anilib.fragment.torrent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.revolgenx.anilib.R
import com.revolgenx.anilib.model.torrent.TrackerModel
import com.revolgenx.anilib.model.torrent.TrackerStatus
import com.revolgenx.anilib.preference.TorrentPreference
import com.revolgenx.anilib.torrent.core.TorrentProgressListener
import com.revolgenx.anilib.util.ThreadUtil.runOnUiThread
import kotlinx.android.synthetic.main.tracker_adapter_layout.view.*
import org.koin.android.ext.android.inject

class TorrentTrackerFragment :
    BaseTorrentRecyclerMetaFragment<TrackerModel, TorrentTrackerFragment.TrackerRecyclerAdapter.TrackerViewHolder>(),
    TorrentProgressListener {

    private val lsd = "LSD"
    private val dht = "DHT"
    private val pex = "PeX"
    private val torrentPref by inject<TorrentPreference>()
    private val trackerModels = mutableMapOf<String, TrackerModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TrackerRecyclerAdapter()
    }

    override fun setHasOptionsMenu(hasMenu: Boolean) {
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!checkValidity()) return

        torrent.addListener(this)
        updateView()
    }

    override fun invoke() {
        updateView()
    }

    private fun updateView() {
        runOnUiThread {
            if (!canUpdateView()) return@runOnUiThread

            torrent.handle!!.status().let {
                trackerModels[lsd] = trackerModels[lsd]?.apply {
                    working =
                        if (torrentPref.lsdEnabled) TrackerStatus.WORKING else TrackerStatus.NOT_WORKING
                } ?: TrackerModel(
                    lsd,
                    if (torrentPref.lsdEnabled) TrackerStatus.WORKING else TrackerStatus.NOT_WORKING,
                    ""
                )

                trackerModels[dht] = trackerModels[dht]?.apply {
                    working =
                        if (torrentPref.dhtEnabled) TrackerStatus.WORKING else TrackerStatus.NOT_WORKING
                } ?: TrackerModel(
                    dht,
                    if (torrentPref.dhtEnabled) TrackerStatus.WORKING else TrackerStatus.NOT_WORKING,
                    ""
                )

                trackerModels[pex] = trackerModels[pex]?.apply {
                    working = TrackerStatus.WORKING
                } ?: TrackerModel(
                    pex,
                    TrackerStatus.WORKING,
                    ""
                )

            }
            torrent.handle!!.trackers().map { entry ->
                val name = entry.url()
                var message = ""
                var status = TrackerStatus.NOT_WORKING
                if (entry.endpoints().isNotEmpty()) {
                    entry.endpoints().sortedWith(compareBy { it.fails() }).first().let { endP ->
                        message = endP.message()
                        status = if (entry.isVerified && endP.isWorking) TrackerStatus.WORKING
                        else if (endP.fails() == 0 && endP.updating()) TrackerStatus.UPDATING
                        else if (endP.fails() == 0) TrackerStatus.NOT_CONTACTED
                        else TrackerStatus.NOT_WORKING
                    }
                }

                trackerModels[name]?.let {
                    it.name = name
                    it.working = status
                    it.message = message
                    it
                } ?: let {
                    trackerModels[name] =
                        TrackerModel(
                            name,
                            status,
                            message
                        )
                }

                trackerModels[name]

            }

            adapter.submitList(trackerModels.values.toList())
        }
    }


    override fun onDestroy() {
        if (checkValidity()) {
            torrent.removeEngineListener()
            torrent.removeListener(this)
        }
        super.onDestroy()
    }

    inner class TrackerRecyclerAdapter :
        ListAdapter<TrackerModel, TrackerRecyclerAdapter.TrackerViewHolder>(object :
            DiffUtil.ItemCallback<TrackerModel>() {
            override fun areItemsTheSame(oldItem: TrackerModel, newItem: TrackerModel): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TrackerModel, newItem: TrackerModel): Boolean {
                return oldItem == newItem
            }
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder {
            return TrackerViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.tracker_adapter_layout,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: TrackerViewHolder, position: Int) {
            holder.bind(getItem(position))
        }


        inner class TrackerViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            fun bind(tracker: TrackerModel) {
                itemView.trackerTv.let {
                    it.title = tracker.name
                    it.subtitle = tracker.working.name
                }
            }
        }
    }
}