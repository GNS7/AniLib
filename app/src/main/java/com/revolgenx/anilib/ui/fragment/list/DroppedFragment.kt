package com.revolgenx.anilib.ui.fragment.list

import androidx.core.os.bundleOf
import com.revolgenx.anilib.data.meta.MediaListMeta
import com.revolgenx.anilib.type.MediaListStatus
import com.revolgenx.anilib.ui.viewmodel.list.DroppedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DroppedFragment : MediaListFragment() {
    override val viewModel by viewModel<DroppedViewModel>()
    override val mediaListStatus: Int = MediaListStatus.DROPPED.ordinal

    override val mediaListMeta: MediaListMeta? get() = arguments?.getParcelable(MEDIA_LIST_META_KEY)

    companion object{
        private const val MEDIA_LIST_META_KEY = "MEDIA_LIST_META_KEY"
        fun newInstance(meta: MediaListMeta) = DroppedFragment().also {
            it.arguments = bundleOf(MEDIA_LIST_META_KEY to meta)
        }
    }
}
