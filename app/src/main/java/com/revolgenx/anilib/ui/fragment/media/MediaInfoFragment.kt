package com.revolgenx.anilib.ui.fragment.media

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.pranavpandey.android.dynamic.theme.Theme
import com.revolgenx.anilib.R
import com.revolgenx.anilib.activity.SimpleDraweeViewerActivity
import com.revolgenx.anilib.app.theme.*
import com.revolgenx.anilib.common.preference.loadLegacyMediaBrowseTheme
import com.revolgenx.anilib.common.preference.loggedIn
import com.revolgenx.anilib.common.ui.adapter.makePagerAdapter
import com.revolgenx.anilib.common.ui.fragment.BaseLayoutFragment
import com.revolgenx.anilib.data.field.ToggleFavouriteField
import com.revolgenx.anilib.data.meta.DraweeViewerMeta
import com.revolgenx.anilib.data.meta.ListEditorMeta
import com.revolgenx.anilib.data.meta.MediaInfoMeta
import com.revolgenx.anilib.data.meta.type.MediaListStatus
import com.revolgenx.anilib.data.meta.type.MediaListStatusEditor
import com.revolgenx.anilib.data.model.EntryListEditorMediaModel
import com.revolgenx.anilib.data.model.media_info.MediaBrowseModel
import com.revolgenx.anilib.databinding.MediaInfoFragmentLayoutBinding
import com.revolgenx.anilib.infrastructure.event.ListEditorResultEvent
import com.revolgenx.anilib.infrastructure.event.OpenMediaListEditorEvent
import com.revolgenx.anilib.infrastructure.event.OpenReviewComposerEvent
import com.revolgenx.anilib.infrastructure.repository.util.Resource
import com.revolgenx.anilib.infrastructure.repository.util.Status.*
import com.revolgenx.anilib.type.MediaType
import com.revolgenx.anilib.ui.fragment.browse.*
import com.revolgenx.anilib.ui.view.drawable.DynamicBackgroundGradientDrawable
import com.revolgenx.anilib.ui.view.makeArrayPopupMenu
import com.revolgenx.anilib.ui.view.makeToast
import com.revolgenx.anilib.ui.viewmodel.media.MediaInfoViewModel
import com.revolgenx.anilib.util.*
import com.revolgenx.anilib.util.EventBusListener
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class MediaInfoFragment : BaseLayoutFragment<MediaInfoFragmentLayoutBinding>(), EventBusListener {

    override fun bindView(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): MediaInfoFragmentLayoutBinding {
        return MediaInfoFragmentLayoutBinding.inflate(inflater, parent, false)
    }


    override val setHomeAsUp: Boolean = true
    override val menuRes: Int? = R.menu.media_info_menu
    override fun getBaseToolbar(): Toolbar {
        return binding.mediaBrowserToolbar
    }

    companion object {
        const val MEDIA_INFO_META_KEY = "MEDIA_INFO_META_KEY"
        const val COLLAPSED = 0
        const val EXPANDED = 1

        fun newInstance(meta: MediaInfoMeta): MediaInfoFragment = MediaInfoFragment().also {
            it.arguments = bundleOf(MEDIA_INFO_META_KEY to meta)
        }
    }

    private var isFavourite = false
    private var toggling = false
    private val mediaInfoMeta: MediaInfoMeta?
        get() = arguments?.getParcelable(
            MEDIA_INFO_META_KEY
        )

    private var circularProgressDrawable: CircularProgressDrawable? = null

    private var browseMediaBrowseModel: MediaBrowseModel? = null

    private val seasons by lazy {
        resources.getStringArray(R.array.media_season)
    }

    private val viewModel by viewModel<MediaInfoViewModel>()
    private val pageChangeListener by lazy {
        object :
            ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (isBindingEmpty()) return

                binding.apply {
                    if (position == 0) return
                    appbarLayout.setExpanded(false)
                }
            }
        }
    }

    private var state = COLLAPSED

    private val offSetChangeListener =
        AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (isBindingEmpty()) return@OnOffsetChangedListener

            val appBarOffsetHeight =
                appBarLayout.totalScrollRange - binding.mediaBrowserToolbar.height

            val collapsingToolbarContentHideRange = appBarOffsetHeight / 1.4f
            val verticalOff = abs(verticalOffset)
            if (verticalOff > collapsingToolbarContentHideRange) {
                binding.collapsingBarContentLayout.visibility = View.INVISIBLE
            } else {
                binding.collapsingBarContentLayout.visibility = View.VISIBLE
            }

            if (verticalOffset == 0) {
                if (state != EXPANDED) {
                    state = EXPANDED
                    updateToolbar()
                }
            } else if (verticalOff >= appBarOffsetHeight) {
                if (state != COLLAPSED) {
                    state = COLLAPSED
                    updateToolbar()

                    binding.mediaBrowserCollapsingToolbar.layoutParams.let {
                        if (it is AppBarLayout.LayoutParams) {
                            val flags =
                                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS_COLLAPSED or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                            if (it.scrollFlags != flags) {
                                it.scrollFlags = flags
                            }

                        }
                    }
                }
            }
        }

    override fun onResume() {
        super.onResume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val mediaInfo = mediaInfoMeta ?: return

        viewModel.mediaLiveData.observe(this.viewLifecycleOwner) {
            when (it.status) {
                SUCCESS -> {
                    browseMediaBrowseModel = it.data

                    if (mediaInfo.coverImage == null || mediaInfo.bannerImage == null) {
                        mediaInfo.coverImage = it.data?.coverImage?.large ?: ""
                        mediaInfo.coverImageLarge = it.data?.coverImage?.largeImage
                        mediaInfo.bannerImage =
                            it.data?.bannerImage ?: it.data?.coverImage?.largeImage
                        mediaInfo.title = it.data?.title?.romaji ?: ""
                    }
                    mediaInfo.type = it.data?.type
                    binding.updateView()
                }
                else -> {
                }
            }
        }


        if (savedInstanceState == null) {
            viewModel.getMediaInfo(mediaInfo.mediaId)
        }


        binding.initListener()
        binding.initTabLayout()
        binding.updateTheme()
        binding.updateView()

        val mediaInfoList = mutableListOf(
            MediaOverviewFragment.newInstance(mediaInfo),
            MediaWatchFragment.newInstance(mediaInfo),
            MediaCharacterFragment.newInstance(mediaInfo),
            MediaStaffFragment.newInstance(mediaInfo),
            MediaReviewFragment.newInstance(mediaInfo),
            MediaStatsFragment.newInstance(mediaInfo)
        )

        if(requireContext().loggedIn()){
            mediaInfoList.add(MediaSocialContainerFragment.newInstance(mediaInfo))
        }

        binding.browseMediaViewPager.addOnPageChangeListener(pageChangeListener)
        binding.browseMediaViewPager.addOnPageChangeListener(
            TabLayout.TabLayoutOnPageChangeListener(
                binding.browseMediaTabLayout
            )
        )


        viewModel.isFavourite(mediaInfo.mediaId).observe(this.viewLifecycleOwner) {
            when (it.status) {
                SUCCESS -> {
                    isFavourite = it.data!!
                    binding.mediaFavButton.setImageResource(if (isFavourite) R.drawable.ic_favourite else R.drawable.ic_not_favourite)
                    updateToolbar()
                }
                else -> {

                }
            }
        }

        viewModel.toggleFavMediaLiveData.observe(this.viewLifecycleOwner, {
            toggling = when (it.status) {
                SUCCESS -> {
                    isFavourite = !isFavourite
                    viewModel.isFavourite(mediaInfo.mediaId).value =
                        Resource.success(isFavourite)
                    binding.mediaFavButton.setImageResource(if (isFavourite) R.drawable.ic_favourite else R.drawable.ic_not_favourite)
                    updateToolbar()
                    false
                }
                ERROR -> {
                    makeToast(R.string.failed_to_toggle, icon = R.drawable.ic_error)
                    false
                }
                LOADING -> {
                    true
                }
            }
        })

        viewModel.saveMediaListEntryLiveData.observe(viewLifecycleOwner) {
            if (it.status == SUCCESS) {
                val data = it.data ?: return@observe
                browseMediaBrowseModel?.mediaListStatus = data.status
                browseMediaBrowseModel?.mediaListStatus?.let { status ->
                    updateMediaStatusView(status)
                }
            }
        }


        binding.browseMediaViewPager.adapter = makePagerAdapter(mediaInfoList)
        binding.browseMediaViewPager.offscreenPageLimit = 5
        binding.browseMediaTabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                binding.browseMediaViewPager.setCurrentItem(position, false)
                if (position == 0) {
                    return
                }
                binding.appbarLayout.setExpanded(false)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        binding.browseMediaViewPager.setCurrentItem(0, false)
        binding.browseMediaViewPager.post {
            pageChangeListener.onPageSelected(binding.browseMediaViewPager.currentItem)
        }

    }


    override fun updateToolbar() {
        if (state == EXPANDED) {
            getBaseToolbar().menu.clear()
        } else {
            super.updateToolbar()
        }
    }

    override fun onStart() {
        super.onStart()
        registerForEvent()
    }

    private fun updateMediaStatusView(status: Int) {
        if (loadLegacyMediaBrowseTheme()) {
            binding.updateLegacyListStatusView(status)
        } else {
            binding.updateListStatusView(status)
        }
    }

    private fun MediaInfoFragmentLayoutBinding.updateListStatusView(status: Int) {
        val mediaListStatus =
            if (browseMediaBrowseModel?.type == MediaType.MANGA.ordinal) resources.getStringArray(R.array.manga_list_status) else resources.getStringArray(
                R.array.anime_list_status
            )
        val fromStatus = MediaListStatus.from(status).ordinal
        mediaAddButton.text =
            mediaListStatus[fromStatus]
    }

    private fun MediaInfoFragmentLayoutBinding.updateLegacyListStatusView(status: Int) {
        val mediaListStatus =
            if (browseMediaBrowseModel?.type == MediaType.MANGA.ordinal) resources.getStringArray(R.array.manga_list_status) else resources.getStringArray(
                R.array.anime_list_status
            )
        val fromStatus = MediaListStatus.from(status).ordinal

        legacyMediaAddButton.text =
            mediaListStatus[fromStatus]
    }


    private fun MediaInfoFragmentLayoutBinding.updateTheme() {

        if (loadLegacyMediaBrowseTheme()) {
            mediaBrowseContentLayout.visibility = View.GONE
            legacyMediaBrowseContentLayout.visibility = View.VISIBLE
            legacyBlurLayout.background = DynamicBackgroundGradientDrawable(
                orientation = GradientDrawable.Orientation.LEFT_RIGHT,
                alpha = 180
            )
        } else {
            binding.mediaBrowserCollapsingToolbar.setCollapsedTitleTextColor(
                dynamicTextColorPrimary
            )

            mediaBrowseContentLayout.visibility = View.VISIBLE
            legacyMediaBrowseContentLayout.visibility = View.GONE
            collapsingContentBlur.background = DynamicBackgroundGradientDrawable(
                orientation = GradientDrawable.Orientation.BOTTOM_TOP,
                alpha = 255
            )
            mediaAddLayout.updateChildTheme()
        }
        setToolbarTheme()

    }

    private fun MediaInfoFragmentLayoutBinding.updateView() {
        val mediaInfo = mediaInfoMeta ?: return
        getBaseToolbar().title = mediaInfo.title

        if (loadLegacyMediaBrowseTheme()) {

            legacyMediaTitleTv.text = mediaInfo.title
            legacyMediaBrowserCoverImage.setImageURI(mediaInfo.coverImage)
            legacyMediaBrowserBannerImage.setImageURI(mediaInfo.bannerImage)

            browseMediaBrowseModel?.mediaListStatus?.let {
                updateLegacyListStatusView(it)
            }
        } else {
            mediaTitleTv.text = mediaInfo.title
            mediaBrowserCoverImage.setImageURI(mediaInfo.coverImage)
            mediaBrowserBannerImage.setImageURI(mediaInfo.bannerImage)

            browseMediaBrowseModel?.let { model ->
                mediaPopularityTv.text =
                    model.popularity?.prettyNumberFormat().naText()
                mediaFavTv.text = model.favourites?.prettyNumberFormat().naText()

                if (model.type == MediaType.ANIME.ordinal) {
                    seasonYearTv.text =
                        getString(R.string.source_seasonyear_s).format(model.season?.let { seasons[it] }
                            ?: "?", model.seasonYear ?: "?")
                } else {
                    seasonYearTv.visibility = View.GONE
                }

                model.mediaListStatus?.let {
                    binding.updateListStatusView(it)
                }

                model.airingTimeModel?.let {
                    mediaAiringAtTv.text = getString(R.string.episode_airing_date).format(
                        it.episode,
                        it.airingAt!!.airingDateTime
                    )
                }
            }

        }

    }


    private fun MediaInfoFragmentLayoutBinding.initTabLayout() {
        createTabLayout(R.string.overview, R.drawable.ic_fire)
        createTabLayout(R.string.watch, R.drawable.ic_watch)
        createTabLayout(R.string.character, R.drawable.ic_character)
        createTabLayout(R.string.staff, R.drawable.ic_staff)
        createTabLayout(R.string.review, R.drawable.ic_review)
        createTabLayout(R.string.stats, R.drawable.ic_chart)

        if(requireContext().loggedIn()){
            createTabLayout(R.string.social, R.drawable.ic_activity_union)
        }
    }

    private fun MediaInfoFragmentLayoutBinding.createTabLayout(
        @StringRes tabText: Int,
        @DrawableRes tabIcon: Int
    ) {
        val newTab =
            browseMediaTabLayout.newTab().setText(tabText).setIcon(tabIcon)
        val iconView = newTab.view.getChildAt(0)!!

        val lp = iconView.layoutParams as ViewGroup.MarginLayoutParams
        lp.bottomMargin = 0;
        iconView.layoutParams = lp
        iconView.requestLayout()
        browseMediaTabLayout.addTab(newTab)
    }

    private fun MediaInfoFragmentLayoutBinding.initListener() {

        appbarLayout.addOnOffsetChangedListener(offSetChangeListener)

        if (loadLegacyMediaBrowseTheme()) {
            legacyMediaAddContainerLayout.setOnClickListener {
                openListEditor()
            }

            legacyMediaAddMoreIv.setOnClickListener {

                val meta = mediaInfoMeta ?: return@setOnClickListener
                val mediaListStatus =
                    if (meta.type == MediaType.MANGA.ordinal) R.array.media_list_manga_status else R.array.media_list_status
                makeArrayPopupMenu(
                    it,
                    resources.getStringArray(mediaListStatus)
                ) { _, _, position, _ ->
                    changeMediaListStatus(MediaListStatusEditor.values()[position].status)
                }
            }

            legacyMediaReviewButton.setOnClickListener {
                openReviewWriter()
            }

            legacyMediaFavButton.setOnClickListener {
                toggleFav()
            }

            legacyMediaBrowserBannerImage.setOnClickListener {
                SimpleDraweeViewerActivity.openActivity(
                    requireContext(),
                    DraweeViewerMeta(mediaInfoMeta!!.bannerImage)
                )
            }

            legacyMediaBrowserCoverImage.setOnClickListener {
                SimpleDraweeViewerActivity.openActivity(
                    requireContext(),
                    DraweeViewerMeta(mediaInfoMeta!!.coverImageLarge)
                )
            }

            legacyMediaTitleTv.setOnLongClickListener {
                requireContext().copyToClipBoard(mediaInfoMeta!!.title)
                true
            }
        } else {
            mediaAddContainerLayout.setOnClickListener {
                openListEditor()
            }

            mediaAddMoreIv.setOnClickListener {
                val meta = mediaInfoMeta ?: return@setOnClickListener
                val mediaListStatus =
                    if (meta.type == MediaType.MANGA.ordinal) R.array.media_list_manga_status else R.array.media_list_status
                makeArrayPopupMenu(
                    it,
                    resources.getStringArray(mediaListStatus)
                ) { _, _, position, _ ->
                    changeMediaListStatus(MediaListStatusEditor.values()[position].status)
                }
            }

            mediaReviewButton.setOnClickListener {
                openReviewWriter()
            }

            mediaFavButton.setOnClickListener {
                toggleFav()
            }

            mediaBrowserBannerImage.setOnClickListener {
                SimpleDraweeViewerActivity.openActivity(
                    requireContext(),
                    DraweeViewerMeta(mediaInfoMeta!!.bannerImage)
                )
            }

            mediaBrowserCoverImage.setOnClickListener {
                SimpleDraweeViewerActivity.openActivity(
                    requireContext(),
                    DraweeViewerMeta(mediaInfoMeta!!.coverImageLarge)
                )
            }

            mediaTitleTv.setOnLongClickListener {
                requireContext().copyToClipBoard(mediaInfoMeta!!.title)
                true
            }

        }
    }


    @SuppressLint("RestrictedApi")
    override fun onToolbarInflated() {
        getBaseToolbar().menu.let { menu ->
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }
            if (isFavourite) {
                menu?.findItem(R.id.toggleFavMenu)?.let {
                    it.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_favourite)
                }
            }
        }
    }


    override fun onToolbarMenuSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addToListMenu -> {
                openListEditor()
                true
            }
            R.id.writeReviewMenu -> {
                openReviewWriter()
                true
            }
            R.id.toggleFavMenu -> {
                if (requireContext().loggedIn())
                    makeToast(R.string.please_wait, icon = R.drawable.ic_hour_glass)

                toggleFav()
                true
            }

            R.id.media_share_menu -> {
                requireContext().openLink(browseMediaBrowseModel?.siteUrl)
                true
            }

            R.id.media_copy_menu -> {
                requireContext().copyToClipBoard(browseMediaBrowseModel?.siteUrl)
                true
            }
            else -> false
        }
    }

    private fun changeMediaListStatus(position: Int) {
        if (mediaInfoMeta == null) return

        if (!requireContext().loggedIn()) {
            makeToast(R.string.please_log_in, null, R.drawable.ic_person)
            return
        }


        viewModel.saveMediaListEntry(EntryListEditorMediaModel().also {
            it.mediaId = mediaInfoMeta!!.mediaId
            it.status = position
        })

    }

    private fun openListEditor() {

        val mediaInfo = mediaInfoMeta ?: return

        if (requireContext().loggedIn()) {
            val meta = ListEditorMeta(
                mediaInfo.mediaId,
                mediaInfo.type,
                mediaInfo.title,
                mediaInfo.coverImage,
                mediaInfo.bannerImage
            )

            OpenMediaListEditorEvent(meta).postEvent
        } else {
            makeToast(R.string.please_log_in, null, R.drawable.ic_person)
        }
    }

    private fun toggleFav() {
        if (toggling || mediaInfoMeta == null) return

        if (!requireContext().loggedIn()) {
            makeToast(R.string.please_log_in, null, R.drawable.ic_person)
            return
        }

        viewModel.toggleMediaFavourite(ToggleFavouriteField().also {
            when (mediaInfoMeta!!.type) {
                MediaType.ANIME.ordinal -> {
                    it.animeId = mediaInfoMeta!!.mediaId
                }
                MediaType.MANGA.ordinal -> {
                    it.mangaId = mediaInfoMeta!!.mediaId
                }
            }
        })
    }

    private fun openReviewWriter() {
        if (mediaInfoMeta == null) return
        if (requireContext().loggedIn()) {
            OpenReviewComposerEvent(mediaInfoMeta!!.mediaId!!).postEvent
        } else {
            makeToast(R.string.please_log_in, null, R.drawable.ic_person)
        }
    }


    private fun setToolbarTheme() {
        if (loadLegacyMediaBrowseTheme()) {
            binding.mediaBrowserCollapsingToolbar.setCollapsedTitleTextColor(
                dynamicTextColorPrimary
            )
            binding.mediaBrowserCollapsingToolbar.setBackgroundColor(
                dynamicBackgroundColor
            )
            binding.mediaBrowserCollapsingToolbar.setStatusBarScrimColor(
                dynamicBackgroundColor
            )
            binding.mediaBrowserCollapsingToolbar.setContentScrimColor(
                dynamicBackgroundColor
            )

            binding.mediaBrowserToolbar.colorType = Theme.ColorType.BACKGROUND
            binding.mediaBrowserToolbar.textColorType = Theme.ColorType.TEXT_PRIMARY
        } else {
            binding.mediaBrowserCollapsingToolbar.setCollapsedTitleTextColor(
                dynamicTextColorPrimary
            )
            binding.mediaBrowserCollapsingToolbar.setBackgroundColor(
                dynamicBackgroundColor
            )

            binding.mediaBrowserToolbar.colorType = Theme.ColorType.BACKGROUND
            binding.mediaBrowserToolbar.textColorType = Theme.ColorType.TEXT_PRIMARY
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ListEditorResultEvent) {
        if (event.listEditorResultMeta.deleted) {
            binding.mediaAddButton.setText(R.string.add)
            binding.legacyMediaAddButton.setText(R.string.add)
        } else {
            event.listEditorResultMeta.status?.let {
                updateMediaStatusView(it)
            }
        }
    }

    override fun onStop() {
        unRegisterForEvent()
        super.onStop()
    }

    override fun onDestroy() {
        circularProgressDrawable?.stop()
        super.onDestroy()
    }
}