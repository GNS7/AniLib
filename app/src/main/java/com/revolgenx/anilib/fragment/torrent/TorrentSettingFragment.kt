package com.revolgenx.anilib.fragment.torrent

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.obsez.android.lib.filechooser.ChooserDialog
import com.pranavpandey.android.dynamic.preferences.DynamicPreferences
import com.pranavpandey.android.dynamic.support.theme.DynamicTheme
import com.revolgenx.anilib.R
import com.revolgenx.anilib.dialog.openFolderChooser
import com.revolgenx.anilib.fragment.base.BaseLayoutFragment
import com.revolgenx.anilib.preference.TorrentPreference
import com.revolgenx.anilib.preference.sharedPreference
import kotlinx.android.synthetic.main.torrent_setting_fragment.*
import org.koin.android.ext.android.inject

class TorrentSettingFragment : BaseLayoutFragment(), OnSharedPreferenceChangeListener {
    override val layoutRes: Int = R.layout.torrent_setting_fragment
    override var setHomeAsUp: Boolean = true
    override var titleRes: Int? = R.string.torrent_setting

    private val torrentPreference by inject<TorrentPreference>()

    private val tintAccentColor: Int
        get() = DynamicTheme.getInstance().get().tintAccentColor

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        requireContext().sharedPreference().registerOnSharedPreferenceChangeListener(this)

        torrentStorageView.subtitle = torrentPreference.storagePath
        torrentStorageView.setOnClickListener {
            openFolderChooser(requireContext(), torrentPreference.storagePath){dir->
                torrentPreference.storagePath = dir
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == TorrentPreference.Key.STORAGE_PATH.name) {
            torrentStorageView.subtitle = torrentPreference.storagePath
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().sharedPreference().unregisterOnSharedPreferenceChangeListener(this)
    }
}