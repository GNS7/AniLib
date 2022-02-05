package com.revolgenx.anilib.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.revolgenx.anilib.R
import com.revolgenx.anilib.common.ui.adapter.makePagerAdapter
import com.revolgenx.anilib.common.ui.adapter.makeViewPagerAdapter2
import com.revolgenx.anilib.common.ui.adapter.setupWithViewPager2
import com.revolgenx.anilib.common.ui.fragment.BaseLayoutFragment
import com.revolgenx.anilib.constant.UserConstant
import com.revolgenx.anilib.user.data.meta.UserStatsMeta
import com.revolgenx.anilib.databinding.UserStatsContainerFragmentLayoutBinding
import com.revolgenx.anilib.type.MediaType

class UserStatsContainerFragment : BaseLayoutFragment<UserStatsContainerFragmentLayoutBinding>() {
    companion object {
        fun newInstance(userStatsMeta: UserStatsMeta) = UserStatsContainerFragment().also {
            it.arguments = bundleOf(UserConstant.USER_STATS_META_KEY to userStatsMeta)
        }
    }


    private val userStatsMeta get()= arguments?.getParcelable<UserStatsMeta?>(UserConstant.USER_STATS_META_KEY)

    private val userAnimeStatsFragments by lazy {
        listOf(
            UserStatisticOverviewFragment(),
            StatsGenreFragment(),
            StatsTagFragment(),
            StatsVoiceActorFragment(),
            StatsStudioFragment(),
            StatsStaffFragment()
        )
    }

    private val userMangaStatsFragments by lazy {
        listOf(
            UserStatisticOverviewFragment(),
            StatsGenreFragment(),
            StatsTagFragment(),
            StatsStaffFragment()
        )
    }

    override fun bindView(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): UserStatsContainerFragmentLayoutBinding {
        return UserStatsContainerFragmentLayoutBinding.inflate(inflater, parent, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val userStats = userStatsMeta ?: return

        val adapter: FragmentStateAdapter
        val tabItems: Array<String>

        when (userStats.type) {
            MediaType.ANIME.ordinal -> {
                userAnimeStatsFragments.forEach {
                    it.arguments = bundleOf(
                        UserConstant.USER_STATS_META_KEY to userStats
                    )
                }

                adapter = makeViewPagerAdapter2(
                    userAnimeStatsFragments
                )
                tabItems = resources.getStringArray(R.array.user_anime_stats_tab_menu)
            }
            else -> {
                userMangaStatsFragments.forEach {
                    it.arguments = bundleOf(
                        UserConstant.USER_STATS_META_KEY to userStats
                    )
                }
                adapter = makeViewPagerAdapter2(
                    userMangaStatsFragments
                )
                tabItems = resources.getStringArray(R.array.user_manga_stats_tab_menu)
            }
        }

        binding.userStatsContainerViewPager.adapter = adapter
        setupWithViewPager2(binding.userStatsTabLayout, binding.userStatsContainerViewPager, tabItems)
        binding.userStatsContainerViewPager.offscreenPageLimit = tabItems.size - 1
    }
}