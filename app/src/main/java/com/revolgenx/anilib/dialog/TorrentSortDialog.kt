package com.revolgenx.anilib.dialog

import android.content.DialogInterface
import androidx.core.os.bundleOf
import com.pranavpandey.android.dynamic.support.adapter.DynamicSpinnerImageAdapter
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.pranavpandey.android.dynamic.support.model.DynamicSpinnerItem
import com.revolgenx.anilib.R
import com.revolgenx.anilib.preference.torrentSort
import com.revolgenx.anilib.util.onItemSelected
import kotlinx.android.synthetic.main.torrent_sort_dialog_layout.*

class TorrentSortDialog : BaseDialogFragment() {
    override var titleRes: Int = R.string.sort
    override var positiveText: Int = R.string.done
    override var negativeText: Int = R.string.cancel
    override var viewRes: Int = R.layout.torrent_sort_dialog_layout

    companion object {
        val tag = TorrentSortDialog::class.java.simpleName
        const val sortKey = "torrent_sort_key"
        fun newInstance(sort: Int) = TorrentSortDialog().also {
            it.arguments = bundleOf(sortKey to sort)
        }
    }

    private val torrentSortItems by lazy {
        requireContext().resources.getStringArray(R.array.torrent_sort).map {
            DynamicSpinnerItem(
                null, it
            )
        }
    }

    override fun onShowListener(alertDialog: DynamicDialog) {
        with(alertDialog) {
            this.torrentSortSpinner.adapter = makeSpinnerAdapter(torrentSortItems)
            arguments?.getInt(sortKey)?.let {
                torrentSortSpinner.setSelection(it)
                torrentSortSpinner.onItemSelected {
                    arguments = bundleOf(sortKey to it)
                }
            }
        }
    }

    override fun onPositiveClicked(dialogInterface: DialogInterface, which: Int) {
        if (dialogInterface is DynamicDialog) {
            dialogInterface.torrentSortSpinner.selectedItemPosition.takeIf { it >= 0 }?.let {
                torrentSort(requireContext(), it)
            }
        }
        super.onPositiveClicked(dialogInterface, which)
    }

    private fun makeSpinnerAdapter(items: List<DynamicSpinnerItem>) =
        DynamicSpinnerImageAdapter(
            requireContext(),
            R.layout.ads_layout_spinner_item,
            R.id.ads_spinner_item_icon,
            R.id.ads_spinner_item_text, items
        )

}