package com.revolgenx.anilib.fragment.torrent

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.revolgenx.anilib.R
import com.revolgenx.anilib.dialog.InputDialog
import com.revolgenx.anilib.dialog.TorrentSpeedLimitDialog
import com.revolgenx.anilib.event.TorrentRecheckEvent
import com.revolgenx.anilib.exception.TorrentPauseException
import com.revolgenx.anilib.exception.TorrentResumeException
import com.revolgenx.anilib.fragment.base.BaseLayoutFragment
import com.revolgenx.anilib.meta.TorrentSpeedMeta
import com.revolgenx.anilib.torrent.core.Torrent
import com.revolgenx.anilib.util.makeToast
import kotlinx.android.synthetic.main.torrent_speed_limit_dialog_layout.*
import org.libtorrent4j.AnnounceEntry

abstract class BaseTorrentMetaFragment : BaseLayoutFragment() {
    companion object {
        const val meta_key = "torrent_meta_key"
    }

    protected lateinit var torrent: Torrent
    protected var pausePlayMenu: MenuItem? = null

    override fun onResume() {
        super.onResume()
        invalidateOptionMenu()
        setHasOptionsMenu(true)
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



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.classLoader = Torrent::class.java.classLoader
        torrent = arguments?.getParcelable(meta_key) ?: return

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

    fun checkValidity() =
        ::torrent.isInitialized && torrent.handle != null && torrent.handle?.isValid == true

}