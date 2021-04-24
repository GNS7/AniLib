package com.revolgenx.anilib.ui.fragment.home.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.revolgenx.anilib.R
import com.revolgenx.anilib.common.ui.fragment.BaseLayoutFragment
import com.revolgenx.anilib.databinding.UserLoginFragmentLayoutBinding
import com.revolgenx.anilib.infrastructure.event.AuthenticateEvent
import com.revolgenx.anilib.infrastructure.event.SettingEvent
import com.revolgenx.anilib.infrastructure.event.SettingEventTypes
import com.revolgenx.anilib.util.openLink

class UserLoginFragment:BaseLayoutFragment<UserLoginFragmentLayoutBinding>(){

    override fun bindView(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): UserLoginFragmentLayoutBinding {
        return UserLoginFragmentLayoutBinding.inflate(inflater, parent, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.apply {
            settingLayout.setOnClickListener {
                SettingEvent(SettingEventTypes.SETTING).postEvent
            }

            signInLayout.setOnClickListener {
                AuthenticateEvent().postEvent
            }

            discordLayout.setOnClickListener {
                requireContext().openLink(getString(R.string.discord_invite_link))
            }

            aboutItemView.setOnClickListener {
                SettingEvent(SettingEventTypes.ABOUT).postEvent
            }

        }
    }
}