package com.revolgenx.anilib.dialog

import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.pranavpandey.android.dynamic.utils.DynamicPackageUtils
import com.revolgenx.anilib.R
import com.revolgenx.anilib.markwon.MarkwonImpl
import kotlinx.android.synthetic.main.release_info_dialog_layout.*

class ReleaseInfoDialog : BaseDialogFragment() {
    companion object {
        val tag = ReleaseInfoDialog::class.java.name
    }

    override var viewRes: Int? = R.layout.release_info_dialog_layout
    override var positiveText: Int? = R.string.close

    override fun onShowListener(alertDialog: DynamicDialog) {
        super.onShowListener(alertDialog)
        with(alertDialog) {
            MarkwonImpl.createInstance(requireContext())
                .setMarkdown(this.releaseInfo, releaseInfoString)
        }
    }

    private val releaseInfoString
        get() =
            "## What's new!!!. \n" +
                    "### -" + DynamicPackageUtils.getAppVersion(requireContext()) + "\n" +
                    "* New Design. Customizable theme.\n" +
                    "* Browse media with complete detail.\n" +
                    "* Browse Suggestion.\n" +
                    "* Search Anime, Manga, Studio, Staff with customization\n" +
                    "* Airing Schedules.\n" +
                    "* No Adult filter for media. \n" +
                    "* Torrent Download with dynamic service. \n" +
                    "* Responsive bookmark icon.\n" +
                    "* Supports Spoiler text, images, youtube, etc\n" +
                    "* Support markdown for image, youtube, video, gif, etc\n" +
                    "* See all the reviews and give review on media\n" +
                    "* See user list, favourites, stats.\n" +
                    "* User Info.\n" +
                    "* In-built login.\n"

}