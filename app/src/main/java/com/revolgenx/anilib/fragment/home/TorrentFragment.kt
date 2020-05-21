package com.revolgenx.anilib.fragment.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.lifecycle.observe
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener
import com.revolgenx.anilib.R
import com.revolgenx.anilib.activity.MainActivity
import com.revolgenx.anilib.adapter.SelectableAdapter
import com.revolgenx.anilib.event.*
import com.revolgenx.anilib.exception.TorrentPauseException
import com.revolgenx.anilib.exception.TorrentResumeException
import com.revolgenx.anilib.fragment.base.BaseFragment
import com.revolgenx.anilib.repository.util.Status
import com.revolgenx.anilib.torrent.core.Torrent
import com.revolgenx.anilib.torrent.core.TorrentEngine
import com.revolgenx.anilib.torrent.core.TorrentProgressListener
import com.revolgenx.anilib.torrent.state.TorrentActiveState
import com.revolgenx.anilib.torrent.state.TorrentState
import com.revolgenx.anilib.util.*
import com.revolgenx.anilib.viewmodel.TorrentViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.download_fragment_layout.*
import kotlinx.android.synthetic.main.torrent_recycler_adapter_layout.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TorrentFragment : BaseFragment() {

    companion object {
        private const val recyclerStateKey = "recycler_state_key"
    }

    private val viewModel by viewModel<TorrentViewModel>()
    private val torrentEngine by inject<TorrentEngine>()
    private val torrentActiveState by inject<TorrentActiveState>()
    private lateinit var adapter: TorrentRecyclerAdapter

    private var rotating = false
    private var forceShutdown = false


    private var actionMode: ActionMode? = null
    private var inActionMode = false
        set(value) {
            field = value

            actionMode = if (value) {
                (activity as? MainActivity)?.startSupportActionMode(actionModeCallback)
            } else {
                actionMode?.finish()
                null
            }
        }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.torrentDeleteItem -> {
//                    MaterialDialog(this@TorrentFragment.context!!).show {
//                        var withFiles = false
//                        checkBoxPrompt(R.string.delete_with_files) {
//                            withFiles = it
//                        }
//                        message(R.string.are_you_sure)
//                        title(R.string.delete_files)
//                        positiveButton(R.string.yes) {
//                            postEvent(
//                                TorrentRemovedEvent(
//                                    (adapter as TorrentRecyclerAdapter).getSelectedHashes(),
//                                    withFiles
//                                )
//                            )
//                            inActionMode = false
//                        }
//                        negativeButton(R.string.no)
//                    }
                    true
                }

                R.id.recheckTorrentItem -> {
                    TorrentRecheckEvent(adapter.getSelectedHashes()).postEvent
                    adapter.clearSelection()
                    inActionMode = false
                    true
                }

                R.id.torrentSelectAllItem -> {
                    adapter.selectAll()
                    true
                }
                android.R.id.home -> {
                    false
                }
                else -> false
            }
        }

        @SuppressLint("RestrictedApi")
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.torrent_action_menu, menu)
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode?) {
            inActionMode = false
            adapter.clearSelection()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.download_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TorrentRecyclerAdapter()

        torrentRecyclerview.layoutManager =
            GridLayoutManager(
                this.context,
                if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
            )

        torrentRecyclerview.addItemDecoration(
            DividerItemDecoration(
                this.context,
                DividerItemDecoration.VERTICAL
            )
        )

        torrentRecyclerview.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.torrent_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addTorrentMenu -> {
                PopupMenu(
                    requireContext(),
                    requireActivity().findViewById(R.id.addTorrentMenu)
                ).let {
                    it.inflate(R.menu.torrent_add_menu)
                    it.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.addTorrentFileMenu -> {
                                (activity as? MainActivity)?.checkPermission()
                                true
                            }
                            R.id.addTorrentMagnetMenu -> {

                                true
                            }
                            else -> false
                        }
                    }
                    it.show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.torrentLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    progressText.showProgress(R.string.loading, false)
                    progressText.visibility = View.GONE
                    adapter.submitList(resource.data)
                }

                Status.ERROR -> {
                    progressText.showProgress(R.string.loading, false)
                    progressText.visibility = View.GONE
                    makeToast(R.string.unable_to_load_torrent)
                }

                Status.LOADING -> {
                    progressText.visibility = View.VISIBLE
                    progressText.showProgress(R.string.loading, true)
                }
            }
        }

        if (savedInstanceState == null)
            torrentEngine.start()

        torrentRecyclerview.adapter = adapter

        savedInstanceState?.let {
            it.getParcelable<Parcelable>(recyclerStateKey)?.let { parcel ->
                torrentRecyclerview.layoutManager?.onRestoreInstanceState(parcel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionMenu()
        setHasOptionsMenu(true)
        updateToolbarTitle()
    }

    override fun onStart() {
        super.onStart()
        registerForEvent()
    }

    override fun onStop() {
        super.onStop()
        unRegisterForEvent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        rotating = true
        outState.putParcelable(
            recyclerStateKey,
            torrentRecyclerview.layoutManager?.onSaveInstanceState()
        )
        super.onSaveInstanceState(outState)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun torrentEngineEvent(event: TorrentEngineEvent) {
        when (event.engineEventTypes) {
            TorrentEngineEventTypes.ENGINE_STARTING -> {
                progressText.showProgress(R.string.starting_engine, true)
            }
            TorrentEngineEventTypes.ENGINE_STARTED -> {
                makeToast(msg = "Engine started")
                progressText.visibility = View.GONE
                viewModel.getAllTorrents()
            }
            TorrentEngineEventTypes.ENGINE_STOPPING -> {
                progressText.visibility = View.VISIBLE
                progressText.showProgress(R.string.engine_stopping, false)
                viewModel.removeAllTorrentEngineListener()
            }
            TorrentEngineEventTypes.ENGINE_FAULT -> {
                progressText.showProgress(R.string.unable_to_start_engine)
                makeToast(R.string.unable_to_start_engine, icon = R.drawable.ic_error)
            }
            TorrentEngineEventTypes.ENGINE_STOPPED -> {
                progressText.visibility = View.VISIBLE
                progressText.showProgress(R.string.engine_stopped, false)
            }
        }
    }

    private fun updateToolbarTitle() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setTitle(R.string.app_name)
            it.supportActionBar?.setSubtitle(0)
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShutdownEvent(event: ShutdownEvent) {
        forceShutdown = true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSessionEvent(event: SessionEvent) {
        rotating = true
    }

    override fun onDestroy() {
        adapter.currentList.forEach { it.removeAllListener() }
        if ((!rotating && !torrentActiveState.serviceActive) || forceShutdown) {
            torrentEngine.stop()
        }
        super.onDestroy()
    }


    inner class TorrentRecyclerAdapter :
        SelectableAdapter<TorrentRecyclerAdapter.TorrentViewHolder, Torrent>(object :
            DiffUtil.ItemCallback<Torrent>() {
            override fun areItemsTheSame(oldItem: Torrent, newItem: Torrent): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: Torrent, newItem: Torrent): Boolean =
                oldItem == newItem
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorrentViewHolder =
            TorrentViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.torrent_recycler_adapter_layout,
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: TorrentViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun performFiltering(constraint: CharSequence?) {
            if (constraint?.length == 0) {
                if (searchTempList.isNotEmpty()) {
                    submitList(mutableListOf<Torrent>().apply { addAll(searchTempList) })
                    searchTempList.clear()
                }
            } else {
                if (searchTempList.isEmpty()) {
                    searchTempList.addAll(currentList)
                }
                submitList(emptyList())
                constraint?.toString()?.toLowerCase()?.trim()?.let { pattern ->
                    searchTempList.filter { it.name.toLowerCase().contains(pattern) }
                        .takeIf { it.isNotEmpty() }?.let {
                            submitList(it)
                        }
                }
            }
        }


        override fun onViewRecycled(holder: TorrentViewHolder) {
            holder.unbind()
            super.onViewRecycled(holder)
        }

        fun getSelectedHashes() = getSelectedItems().map { currentList[it].hash }

        inner class TorrentViewHolder(private val v: View) : RecyclerView.ViewHolder(v),
            TorrentProgressListener {
            private var torrent: Torrent? = null
            private var currentState: TorrentState = TorrentState.UNKNOWN

            fun bind(item: Torrent) {
                torrent = item
                torrent!!.addListener(this)
                v.apply {
                    torrentAdapterConstraintLayout.isSelected = isSelected(adapterPosition)
                    pausePlayIv.setOnClickListener {
                        if (torrent!!.isPausedWithState()) {
                            try {
                                torrent!!.resume()
                            } catch (e: TorrentResumeException) {
                                makeToast(msg = e.message)
                            }
                        } else {
                            try {
                                torrent!!.pause()
                            } catch (e: TorrentPauseException) {
                                makeToast(msg = e.message)
                            }
                        }
                    }

                    setOnClickListener {
                        if (selectedItemCount > 0) {
                            toggleSelection(adapterPosition)
                            return@setOnClickListener
                        }

                        if (selectedItemCount <= 0) {
                            if (inActionMode) {
                                inActionMode = false
                                return@setOnClickListener
                            }
                        }

//                        startActivity(Intent(context, TorrentMetaActivity::class.java).apply {
//                            putExtra(TorrentMetaActivity.torrentKey, torrent!!)
//                        })
                    }

                    setOnLongClickListener {
                        toggleSelection(adapterPosition)

                        if (!inActionMode) {
                            inActionMode = true
                        }

                        if (selectedItemCount <= 0) {
                            if (inActionMode) inActionMode = false
                        }

                        true
                    }
                }
                updateView()
            }

            @SuppressLint("SetTextI18n")
            private fun updateView() {
                v.apply {
                    if (context == null) return

                    torrentNameTv.text = torrent!!.name
                    val progress = torrent!!.progress
                    torrentProgressBar.progress = progress.toInt()

//                    if (torrent!!.hasError) {
//                        indicatorView.setBackgroundColor(context.color(R.color.errorColor))
//                    }

                    val state = torrent!!.state
                    torrentFirstTv.text =
                        "${torrent!!.state.name} · S:${torrent!!.connectedSeeders()} · L:${torrent!!.connectedLeechers()}${
                        if (state == TorrentState.DOWNLOADING) {
                            " · ET: ${torrent!!.eta().formatRemainingTime()}"
                        } else ""}"

                    torrentSecondTv.text =
                        if (state == TorrentState.COMPLETED || state == TorrentState.SEEDING) {
                            "${torrent!!.totalCompleted.formatSize()}/${torrent!!.totalSize.formatSize()} · " +
                                    "↑ ${torrent!!.uploadSpeed.formatSpeed()}"
                        } else
                            "${torrent!!.totalCompleted.formatSize()}/${torrent!!.totalSize.formatSize()} · " +
                                    "↓ ${torrent!!.downloadSpeed.formatSpeed()} · ↑ ${torrent!!.uploadSpeed.formatSpeed()}"

                    if (currentState == state) return

                    currentState = state

//                    indicatorView.setBackgroundColor(
//                        when (currentState) {
//                            TorrentState.PAUSED -> {
//                                context.color(R.color.pausedColor)
//                            }
//                            TorrentState.UNKNOWN -> {
//                                context.color(R.color.red)
//                            }
//
//                            TorrentState.DOWNLOADING
//                                , TorrentState.CHECKING
//                                , TorrentState.QUEUE
//                                , TorrentState.CHECKING_FILES
//                                , TorrentState.DOWNLOADING_METADATA
//                                , TorrentState.ALLOCATING
//                                , TorrentState.CHECKING_RESUME_DATA -> {
//                                context.color(R.color.downloadingColor)
//                            }
//
//                            TorrentState.SEEDING -> {
//                                context.color(R.color.seedingColor)
//                            }
//                            TorrentState.COMPLETED -> {
//                                context.color(R.color.completedColor)
//                            }
//                        }
//                    )

                    pausePlayIv.setImageResource(
                        if (torrent!!.isPausedWithState()) {
                            R.drawable.ic_play
                        } else {
                            R.drawable.ic_pause
                        }
                    )

                }
            }

            override fun invoke() {
                activity?.runOnUiThread {
                    updateView()
                }
            }


            fun unbind() {
                torrent!!.removeListener(this)
                torrent = null
            }
        }
    }


}
