package com.revolgenx.anilib.dialog

import androidx.core.os.bundleOf
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.revolgenx.anilib.R
import com.revolgenx.anilib.meta.TorrentSpeedMeta
import kotlinx.android.synthetic.main.torrent_speed_limit_dialog_layout.*

class TorrentSpeedLimitDialog : BaseDialogFragment() {
    companion object {
        val tag = TorrentSpeedLimitDialog::class.java.simpleName
        const val metaKey = "torrent_speed_meta_key"
        fun newInstance(meta: TorrentSpeedMeta): TorrentSpeedLimitDialog {
            return TorrentSpeedLimitDialog().also {
                it.arguments = bundleOf(metaKey to meta)
            }
        }
    }

    override var viewRes: Int = R.layout.torrent_speed_limit_dialog_layout
    override var positiveText: Int = R.string.done
    override var negativeText: Int = R.string.cancel

    override fun onShowListener(alertDialog: DynamicDialog) {
        val metaData = arguments?.getParcelable<TorrentSpeedMeta>(metaKey) ?: return
        with(alertDialog) {
            this.uploadSpeedEt.setText(metaData.uploadSpeed.toString())
            this.downloadSpeedEt.setText(metaData.downloadSpeed.toString())
        }
    }
}