package com.revolgenx.anilib.fragment.torrent

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import com.revolgenx.anilib.R
import com.revolgenx.anilib.dialog.InputDialog
import com.revolgenx.anilib.dialog.TorrentSpeedLimitDialog
import com.revolgenx.anilib.event.TorrentRecheckEvent
import com.revolgenx.anilib.exception.TorrentPauseException
import com.revolgenx.anilib.exception.TorrentResumeException
import com.revolgenx.anilib.meta.TorrentSpeedMeta
import com.revolgenx.anilib.torrent.core.TorrentProgressListener
import com.revolgenx.anilib.torrent.state.TorrentState
import com.revolgenx.anilib.util.*
import com.revolgenx.anilib.util.ThreadUtil.runOnUiThread
import kotlinx.android.synthetic.main.torrent_meta_fragment_layout.*
import kotlinx.android.synthetic.main.torrent_speed_limit_dialog_layout.*
import org.libtorrent4j.AnnounceEntry
import timber.log.Timber
import java.io.File
import java.util.*

//todo change name
class TorrentMetaFragment : BaseTorrentMetaFragment(), TorrentProgressListener {
    override val layoutRes: Int = R.layout.torrent_meta_fragment_layout

    private val date = Date()

    private var pausePlayMenu: MenuItem? = null


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!checkValidity()) return
        setHasOptionsMenu(true)
        torrent.addListener(this)
        addListener()
        updateView()


        if (savedInstanceState != null) {
            (childFragmentManager.findFragmentByTag(TorrentSpeedLimitDialog.tag) as? TorrentSpeedLimitDialog)?.let {
                it.addListener { upload, download ->
                    if (!checkValidity()) return@addListener
                    torrent.uploadLimit = upload * 1024
                    torrent.downloadLimit = download * 1024
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.torrent_meta_fragment_menu, menu)
        pausePlayMenu = menu.findItem(R.id.torrentPauseItem)
        if (!checkValidity()) return

        pausePlayMenu?.setIcon(
            if (torrent.isPausedWithState()) {
                R.drawable.ic_play
            } else {
                R.drawable.ic_pause
            }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!checkValidity()) return false

        return when (item.itemId) {
            R.id.torrentPauseItem -> {
                if (torrent.isPausedWithState()) {
                    try {
                        torrent.resume()
                    } catch (e: TorrentResumeException) {
                        makeToast(msg = e.message)
                    }
                } else {
                    try {
                        torrent.pause()
                    } catch (e: TorrentPauseException) {
                        makeToast(msg = e.message)
                    }
                }

                true
            }
            R.id.recheckItem -> {
                TorrentRecheckEvent(listOf(torrent.hash)).postEvent
                true
            }
            R.id.reannounceItem -> {
                torrent.forceReannounce()
                true
            }
            R.id.speedLimitItem -> {
                makeSpeedLimitDialog(
                    torrent.uploadLimit,
                    torrent.downloadLimit
                ) { upload, download ->
                    torrent.uploadLimit = upload * 1024
                    torrent.downloadLimit = download * 1024
                }
                true
            }
            R.id.addTrackerItem -> {
                InputDialog.newInstance(R.string.add_space_between_trackers).let {
                    it.onInputDoneListener = doneListener@{
                        if (!checkValidity()) return@doneListener
                        it.split(" ").map { AnnounceEntry(it) }
                            .let { torrent.torrentAddTracker(it) }
                    }
                    it.show(childFragmentManager, InputDialog.tag)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun makeSpeedLimitDialog(
        uploadLimit: Int,
        downloadLimit: Int,
        callback: ((Int, Int) -> Unit)
    ) {
        TorrentSpeedLimitDialog.newInstance(TorrentSpeedMeta(uploadLimit, downloadLimit)).also {
            it.addListener(callback)
            it.show(childFragmentManager, TorrentSpeedLimitDialog.tag)
        }
    }

    private fun TorrentSpeedLimitDialog.addListener(callback: ((Int, Int) -> Unit)) {
        onButtonClickedListener = call@{ dialogInterface, which ->
            if (which == AlertDialog.BUTTON_POSITIVE) {
                if (dialogInterface is AlertDialog) {
                    val upload = dialogInterface.uploadSpeedEt?.text!!.toString().toIntOrNull()
                        ?: return@call
                    val download =
                        dialogInterface.downloadSpeedEt?.text!!.toString().toIntOrNull()
                            ?: return@call
                    callback.invoke(upload, download)
                }
            }
        }
    }


    private fun addListener() {
        torrentNameTv.setOnClickListener {
            //            MaterialDialog(this.context!!).show {
//                title(R.string.torrent_name)
//                input(prefill = this@TorrentMetaFragment.torrentNameTv.description) { _, charSequence ->
//                    val newPath = torrent.path + "$charSequence"
//                    if (File(newPath).exists()) {
//                        makeToast(string(R.string.file_exists))
//                        return@input
//                    }
//                    if (torrent.checkValidity())
//                        torrent.handle!!.torrentFile().files().name(charSequence.toString())
//                }
//
//                negativeButton()
//            }
        }

        torrentHashTv.setOnClickListener {
            requireContext().copyToClipBoard(torrentHashTv.subtitle?.toString())
        }
    }


    override fun invoke() {
        updateView()
    }

    @UiThread
    private fun updateView() {
        runOnUiThread {
            if (!checkValidity()) return@runOnUiThread
            if (context == null) return@runOnUiThread


            pausePlayMenu?.setIcon(
                if (torrent.isPausedWithState()) {
                    R.drawable.ic_play
                } else {
                    R.drawable.ic_pause
                }
            )


            val handle = torrent.handle
            val status = torrent.torrentStatus()
            torrentNameTv.subtitle = torrent.name
            torrentHashTv.subtitle = torrent.hash
            torrentPathTv.subtitle = torrent.path

            torrentPiecesTv.subtitle =
                (if (status.hasMetadata()) {
                    status.numPieces().toString() + "/" + handle!!.torrentFile().numPieces().toString() + " (" +
                            handle.torrentFile().pieceLength().toLong().formatSize() + ")"
                } else "").naText()

            torrentSpeedTv.subtitle =
                (if (torrent.state == TorrentState.DOWNLOADING || torrent.state == TorrentState.DOWNLOADING_METADATA) {
                    "↓ ${torrent.downloadSpeed.formatSpeed()} · ↑ ${torrent.uploadSpeed.formatSpeed()}"
                } else "").naText()

            torrentFileSizeTv.title =
                requireContext().string(R.string.size_free)
                    .format(getFree(File(torrent.path)).formatSize())

            torrentFileSizeTv.subtitle = torrent.totalSize.formatSize()
            torrentSeedersLeechersTv.subtitle =
                "${torrent.connectedSeeders()} (${torrent.totalSeeders()}) · ${torrent.connectedLeechers()} (${torrent.totalLeechers()})"
            torrentPeersTv.subtitle = "${torrent.connectedPeers()} (${torrent.totalPeers()})"

            torrentDownloadedUploadedTv.subtitle =
                "${status.allTimeDownload().formatSize()} (D) · ${status.allTimeUpload().formatSize()} (U)"

            val eta = torrent.eta()
            torrentEtaTv.subtitle =
                (if (eta == 0L) "" else torrent.eta().formatRemainingTime()).naText()

            torrentAddedTv.subtitle = torrent.createDate.toString()

            val createdDate = if (torrent.torrentStatus().hasMetadata()) {
                handle!!.torrentFile().creationDate()
            } else 0

            torrentCreatedTv.subtitle =
                (if (createdDate == 0L) "" else date.apply {
                    time = createdDate * 1000
                }.toString()).naText()

        }
    }

    override fun onDestroy() {
        if (checkValidity()) {
            torrent.removeListener(this)
            torrent.removeEngineListener()
        }
        super.onDestroy()
    }

}