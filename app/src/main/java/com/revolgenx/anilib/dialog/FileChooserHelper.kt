package com.revolgenx.anilib.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.obsez.android.lib.filechooser.ChooserDialog
import com.pranavpandey.android.dynamic.support.theme.DynamicTheme
import com.revolgenx.anilib.R
import java.io.File


fun openFileChooser(context: Context, startPath: String, callback: (String, File) -> Unit) {
    ChooserDialog(context)
        .withFilter(false, false, "torrent")
        .withStartFile(startPath)
        .withChosenListener { dir, dirFile ->
            callback.invoke(dir, dirFile)
        }
        .withResources(R.string.choose_a_file, R.string.done, R.string.cancel)
        .titleFollowsDir(true)
        .withFileIcons(
            false,
            ContextCompat.getDrawable(
                context,
                R.drawable.ads_ic_file
            )?.also { DrawableCompat.setTint(it, DynamicTheme.getInstance().get().tintAccentColor) },
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_folder
            )?.also { DrawableCompat.setTint(it, DynamicTheme.getInstance().get().tintAccentColor) }
        )
        .enableOptions(true)
        .build()
        .show()
}

fun openFolderChooser(context: Context, startPath: String, callback: (String) -> Unit) {
    ChooserDialog(
        context
    )
        .withFilter(true, false)
        .withStartFile(startPath)
        .withChosenListener { dir, _ ->
            callback.invoke(dir)
        }
        .withResources(R.string.choose_a_directory, R.string.done, R.string.cancel)
        .titleFollowsDir(true)
        .withFileIcons(
            false,
            ContextCompat.getDrawable(
                context,
                R.drawable.ads_ic_file
            )?.also {
                DrawableCompat.setTint(
                    it,
                    DynamicTheme.getInstance().get().tintAccentColor
                )
            },
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_folder
            )?.also { DrawableCompat.setTint(it, DynamicTheme.getInstance().get().tintAccentColor) }
        )
        .enableOptions(true)
        .build()
        .show()
}