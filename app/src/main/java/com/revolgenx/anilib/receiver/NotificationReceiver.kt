package com.revolgenx.anilib.receiver


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.revolgenx.anilib.event.ShutdownEvent

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val SHUTDOWN_ACTION_KEY = "shutdown_action_key"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.hasExtra(SHUTDOWN_ACTION_KEY) == true) {
            ShutdownEvent().postEvent
        }
    }

}