package com.revolgenx.anilib.fragment.torrent

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.net.toFile
import androidx.recyclerview.widget.DividerItemDecoration
import com.github.axet.androidlibrary.widgets.HeaderRecyclerAdapter
import com.github.axet.androidlibrary.widgets.TreeListView
import com.revolgenx.anilib.R
import com.revolgenx.anilib.adapter.FilesTreeAdapter
import com.revolgenx.anilib.event.TorrentAddedEvent
import com.revolgenx.anilib.event.TorrentAddedEventTypes
import com.revolgenx.anilib.exception.TorrentException
import com.revolgenx.anilib.fragment.base.BaseLayoutFragment
import com.revolgenx.anilib.torrent.core.Torrent
import com.revolgenx.anilib.torrent.core.TorrentEngine
import com.revolgenx.anilib.util.*
import com.revolgenx.anilib.viewmodel.AddTorrentViewModel
import kotlinx.android.synthetic.main.add_torrent_fragment_layout.*
import kotlinx.android.synthetic.main.error_layout.*
import kotlinx.android.synthetic.main.loading_layout.*
import kotlinx.android.synthetic.main.resource_status_container_layout.*
import kotlinx.android.synthetic.main.torrent_file_header_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.libtorrent4j.AlertListener
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import timber.log.Timber
import java.io.File
import kotlin.coroutines.CoroutineContext

class AddTorrentFragment : BaseLayoutFragment(), AlertListener, CoroutineScope {

    companion object {
        const val uriKey = "uri_key"
    }


    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override var titleRes: Int? = R.string.add_torrent
    override var setHomeAsUp: Boolean = true
    override val layoutRes: Int = R.layout.add_torrent_fragment_layout
    private lateinit var headerLayout: View
    private val engine by inject<TorrentEngine>()
    private var rotation = false

    private var adapter: FilesTreeAdapter? = null

    private val viewModel by viewModel<AddTorrentViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        headerLayout = LayoutInflater.from(context).inflate(
            R.layout.torrent_file_header_layout,
            container,
            false
        )
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.classLoader = Uri::class.java.classLoader
        viewModel.uri = arguments?.getParcelable(uriKey) ?: return

        initListener()

        if (savedInstanceState == null) {
            viewModel.path = getDefualtStoragePath()
            decodeUri()
        }

        if (viewModel.handle == null) return

        adapter = FilesTreeAdapter(viewModel.handle!!) {
            headerLayout.torrentMetaTotalSizeTv.text = it
        }

        val div = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        treeRecyclerView.addItemDecoration(div)

        val headerRecyclerAdapter = HeaderRecyclerAdapter(adapter!!)
        headerRecyclerAdapter.setHeaderView(headerLayout)
        treeRecyclerView.adapter = headerRecyclerAdapter

        updateView()
    }

    private fun initListener() {
        engine.addListener(this)
        headerLayout.torrentMetaFileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            adapter?.checkAll(isChecked)
        }
    }

    private fun updateView() {
        if (viewModel.handle == null) return

        viewModel.handle.let { handle ->
            val hash = handle!!.infoHash().toHex()
            torrentMetaNameTv.subtitle = handle.name()
            torrentMetaHashTv.subtitle = hash
            torrentPathMetaTv.subtitle = viewModel.path

            if (handle.status().hasMetadata()) {
                headerLayout.torrentMetaTotalSizeTv.text =
                    handle.torrentFile().totalSize().formatSize()

                torrentPiecesTv.subtitle =
                    handle.torrentFile().numPieces().toString() + "/" + handle.torrentFile().pieceLength().toLong().formatSize()

                torrentSizeTv.subtitle = handle.torrentFile().totalSize().formatSize()
            }

            launch(Dispatchers.IO) {
                adapter?.update {
                    launch(Dispatchers.Main) {
                        adapter!!.load()
                        adapter!!.updateTotal()
                    }
                }

            }
        }
    }

    private fun decodeUri() {
        viewModel.uri.let { uri ->
            when (uri.scheme) {
                FILE_PREFIX -> {
                    try {
                        viewModel.handle =
                            engine.loadTorrent(
                                TorrentInfo(uri.toFile()),
                                File(viewModel.path),
                                null,
                                null,
                                null
                            )
                    } catch (e: Exception) {
                        error()
                        makeToast(msg = e.message ?: "error", icon = R.drawable.ic_error)
                        return
                    }
                }

                MAGNET_PREFIX -> {
                    viewModel.handle = try {
                        engine.fetchMagnet(uri.toString(), File(viewModel.path))
                    } catch (e: Exception) {
                        error()
                        makeToast(msg = e.message ?: "error", icon = R.drawable.ic_error)
                        return
                    }
                    loading()
                }

                CONTENT_PREFIX -> {
                    try {
                        viewModel.handle = engine.loadTorrent(
                            TorrentInfo(uriContentToByteArray(requireContext(), uri)),
                            File(viewModel.path),
                            null,
                            null,
                            null
                        )
                    } catch (e: Exception) {
                        error()
                        makeToast(msg = e.message ?: "error", icon = R.drawable.ic_error)
                        return
                    }
                }
                else -> {
                    error()
                    requireContext().makeToast(R.string.unknown_scheme, icon = R.drawable.ic_error)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_torrent_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addTorrentMenu -> {
                if (viewModel.handle == null) {
                    finishActivity()
                    return false
                }

                return when (viewModel.uri.scheme) {
                    MAGNET_PREFIX -> {
                        TorrentAddedEvent(
                            Torrent().also {
                                it.path = torrentPathMetaTv.subtitle.toString()
                                it.hash = torrentMetaHashTv.subtitle.toString()
                                it.simpleState = true
                                if (viewModel.handle!!.status().hasMetadata()) {
                                    it.source = viewModel.handle!!.torrentFile().bencode()!!
                                }else{

                                }
                                it.magnet = arguments?.getParcelable<Uri>(uriKey).toString()
                                it.handle = engine.loadTorrent(it.magnet, File(viewModel.path))
                            },
                            TorrentAddedEventTypes.TORRENT_ADDED
                        ).postEvent
                        rotation = true
                        finishActivity()
                        true
                    }
                    else -> {
                        TorrentAddedEvent(
                            Torrent().also {
                                it.path = torrentPathMetaTv.subtitle.toString()
                                it.hash = torrentMetaHashTv.subtitle.toString()
                                it.handle = viewModel.handle
                                it.simpleState = true
                                it.magnet = viewModel.handle!!.makeMagnetUri()
                                it.source = viewModel.handle!!.torrentFile().bencode()!!
                            },
                            TorrentAddedEventTypes.TORRENT_ADDED
                        ).postEvent
                        rotation = true
                        finishActivity()
                        true
                    }
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun alert(alert: Alert<*>?) {
        Handler(Looper.getMainLooper()).post {
            if (alert == null) return@post
            when (alert.type()) {
                AlertType.METADATA_RECEIVED -> {
                    updateView()
                    done()
                }
                AlertType.METADATA_FAILED -> {
                    error()
                }
                else -> {
                    Timber.d(alert.type().name)
                }
            }
        }
    }

    private fun loading() {
        resourceStatusContainer.visibility = View.VISIBLE
        progressLayout.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
    }

    private fun error() {
        resourceStatusContainer.visibility = View.VISIBLE
        progressLayout.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
    }

    private fun done() {
        resourceStatusContainer.visibility = View.GONE
        progressLayout.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
    }


    override fun types(): IntArray {
        return intArrayOf(AlertType.METADATA_RECEIVED.swig(), AlertType.METADATA_FAILED.swig())
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        rotation = true
    }

    override fun onDestroy() {
        if (!rotation && viewModel.handle != null) {
            engine.cancelFetchMagnet(viewModel.handle!!.infoHash().toHex())
        }
        engine.removeListener(this)
        job.cancel()
        super.onDestroy()
    }
}