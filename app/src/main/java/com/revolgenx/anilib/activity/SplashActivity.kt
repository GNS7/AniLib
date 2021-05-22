package com.revolgenx.anilib.activity

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageView
import com.pranavpandey.android.dynamic.support.splash.activity.DynamicSplashActivity
import com.revolgenx.anilib.R
import com.revolgenx.anilib.app.theme.ThemeController
import com.revolgenx.anilib.app.theme.dynamicBackgroundColor
import com.revolgenx.anilib.common.preference.getApplicationLocale
import java.util.*

/**
 * Implementing a splash screen by using [DynamicSplashActivity].
 */
class SplashActivity : DynamicSplashActivity() {

    /**
     * Splash image view to start animations.
     */
    private var mSplash: AppCompatImageView? = null

    override fun getLocale(): Locale {
        return Locale(getApplicationLocale())
    }

    override fun setNavigationBarThemeInLandscape(): Boolean {
        return true
    }

    override fun getLayoutRes(): Int {
        return R.layout.activity_splash
    }

    override fun getMinSplashTime(): Long {
        return 550
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        statusBarColor = backgroundColor
    }

    override fun onViewCreated(view: View) {
        mSplash = view.findViewById(R.id.splash_image)
    }

    override fun onPreSplash() {
    }

    override fun doBehindSplash() {
        // TODO: Do any operation behind the splash.
    }

    override fun onPostSplash() {
        // TODO: Do any operation on post splash.
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
