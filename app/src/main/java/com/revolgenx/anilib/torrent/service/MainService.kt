package com.revolgenx.anilib.torrent.service


import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.revolgenx.anilib.R
import com.revolgenx.anilib.activity.MainActivity
import com.revolgenx.anilib.event.*
import com.revolgenx.anilib.receiver.NotificationReceiver
import com.revolgenx.anilib.repository.db.TorrentRepository
import com.revolgenx.anilib.torrent.core.Torrent
import com.revolgenx.anilib.torrent.core.TorrentEngine
import com.revolgenx.anilib.torrent.state.TorrentActiveState
import com.revolgenx.anilib.util.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject

class MainService : Service() {

    private val binder = LocalBinder()
    private var notifyManager: NotificationManager? = null

    private val serviceStartedNotifId: Int = 1000
    private val foregroundChanId = "com.revolgenx.anilib.FOREGROUND_DEFAULT_CHAN_ID"
    private val defChanId = "com.revolgenx.anilib.DEFAULT_CHAN_ID"
    private val channelName = "anilib_channel_0"
    private val channelName1 = "anilib_channel_1"
    private var foregroundNotification: NotificationCompat.Builder? = null
    private var startupPendingIntent: PendingIntent? = null
    private val handler = Handler()


    val torrentHashMap = mutableMapOf<String, Torrent>()
    private val torrentEngine by inject<TorrentEngine>()
    private val torrentRepository by inject<TorrentRepository>()
    private val torrentActiveState by inject<TorrentActiveState>()

    //for any error
    private val runnable = object : Runnable {
        override fun run() {
            checkIfServiceIsEmpty()
            handler.postDelayed(this, 2000)
        }
    }

    //for updating notification
    private val notifRunnable = object : Runnable {
        override fun run() {
            if (isTorrentEmpty()) return

            updateNotification()
            handler.postDelayed(this, 1000L)
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    inner class LocalBinder : Binder() {
        val service: MainService
            get() = this@MainService
    }


    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onCreate() {
        super.onCreate()
        notifyManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        makeNotifyChans()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerForEvent()
        registerNetwork()
        makeForegroundNotify()
        return START_NOT_STICKY
    }

    private fun registerNetwork() {
        if (applicationContext == null) return
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    super.onLost(network)
                    stopService()
                }
            })

    }


    //TODO://CHECK FOR DIFFERENT EVENTS
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun torrentEvent(event: TorrentEvent) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(runnable, 2000)
        handler.postDelayed(notifRunnable, 1000L)

        torrentActiveState.serviceActive = true
        when (event.type) {
            TorrentEventType.TORRENT_RESUMED -> {
                synchronized(torrentHashMap) {
                    event.torrents.forEach { torrent ->
                        torrentHashMap[torrent.hash] = torrent
                    }

                    event.torrents.filter { !it.checkValidity() }.forEach { torrent ->
                        torrentHashMap.remove(torrent.hash)
                    }
                }
            }

            TorrentEventType.TORRENT_PAUSED -> {
                synchronized(torrentHashMap) {
                    CoroutineScope(Dispatchers.IO).launch {
                        torrentRepository.updateAll(event.torrents)
                    }

                    event.torrents.forEach { torrent ->
                        torrentHashMap.remove(torrent.hash)
                    }

                    checkIfServiceIsEmpty()
                }
            }
            TorrentEventType.TORRENT_FINISHED -> {
                synchronized(torrentHashMap) {
                    makeCompletedNotification(event.torrents)
                    CoroutineScope(Dispatchers.IO).launch {
                        torrentRepository.updateAll(event.torrents)
                    }
                }
            }
            TorrentEventType.TORRENT_ERROR -> {
                synchronized(torrentHashMap) {
                    makeErrorNotification(event.torrents)
                    CoroutineScope(Dispatchers.IO).launch {
                        torrentRepository.updateAll(event.torrents)
                    }
                    event.torrents.forEach { torrent ->
                        torrentHashMap.remove(torrent.hash)
                    }
                    checkIfServiceIsEmpty()
                }
            }
            else -> {
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentRemovedEvent(event: TorrentRemovedEvent) {
        event.hashes.forEach { torrentHashMap.remove(it) }
        checkIfServiceIsEmpty()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateDatabase(event: TorrentDbEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            torrentRepository.update(event.torrent)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShutdownEvent(event: ShutdownEvent) {
        stopService()
    }


    private fun checkIfServiceIsEmpty() {
        val torrentIsEmpty = isTorrentEmpty()

        if (torrentIsEmpty) {
            torrentActiveState.serviceActive = false
        }

        if (torrentIsEmpty) {
            stopService()
        }
    }

    private fun isTorrentEmpty() = torrentHashMap.isEmpty()


    private fun stopService() {
        CoroutineScope(Dispatchers.IO).launch {
            handler.removeCallbacksAndMessages(null)
            this@MainService.unRegisterForEvent()
            torrentHashMap.values.map {
                val torrent = it
                torrent.pause()
            }
            delay(100)
            torrentRepository.updateAll(torrentHashMap.values.toList())
            torrentHashMap.clear()
            if (!torrentActiveState.fragmentActive) {
                torrentEngine.stop()
            }
            stopSelf()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        stopService()
    }

    //notification
    private fun makeNotifyChans() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        val chans = mutableListOf<NotificationChannel>()
        val defaultChan =
            NotificationChannel(defChanId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        defaultChan.enableVibration(true)
        defaultChan.vibrationPattern = longArrayOf(1000) /* ms */
        defaultChan.enableLights(true)
        defaultChan.lightColor = Color.WHITE

        chans.add(defaultChan)
        chans.add(
            NotificationChannel(
                foregroundChanId,
                channelName1,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        notifyManager!!.createNotificationChannels(chans)
    }

    private fun makeForegroundNotify() {
        /* For starting main activity */
        val startupIntent = Intent(applicationContext, MainActivity::class.java)
        startupIntent.action = Intent.ACTION_MAIN
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER)
//        startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        startupPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            startupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        foregroundNotification = NotificationCompat.Builder(
            applicationContext,
            foregroundChanId
        )
            .setContentIntent(startupPendingIntent)
            .setContentTitle(getTitleNotifString())
            .setStyle(notificationStyle())
            .setContentText(getString(R.string.foreground_notification))
            .setTicker(getString(R.string.foreground_notification))
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_anilib_inline)
            .addAction(makeShutdownAction())
            .setCategory(Notification.CATEGORY_SERVICE)

        startForeground(serviceStartedNotifId, foregroundNotification!!.build())
    }

    private fun makeShutdownAction(): NotificationCompat.Action {
        val shutdownIntent = Intent(applicationContext, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.SHUTDOWN_ACTION_KEY, "shutdown")
        }
        return NotificationCompat.Action(
            0,
            getString(R.string.exit),
            PendingIntent.getBroadcast(
                applicationContext,
                0,
                shutdownIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }


    private fun makeCompletedNotification(list: List<Torrent>) {
        val builder = NotificationCompat.Builder(applicationContext, defChanId)
            .setSmallIcon(R.drawable.ic_completed)
            .setColor(color(R.color.colorPrimary))
            .setContentTitle(string(R.string.completed))
            .setWhen(System.currentTimeMillis())
            .setDefaults(Notification.DEFAULT_SOUND)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setLights(color(R.color.colorPrimary), 1000, 1000)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(startupPendingIntent)


        list.forEach { obj ->
            builder.setContentText(obj.name)
            notifyManager!!.notify(
                obj.hashCode(),
                builder.build()
            )
        }
    }

    private fun makeErrorNotification(list: List<Torrent>) {
        val builder = NotificationCompat.Builder(applicationContext, defChanId)
            .setSmallIcon(R.drawable.ic_error)
            .setColor(color(R.color.colorPrimary))
            .setWhen(System.currentTimeMillis())
            .setDefaults(Notification.DEFAULT_SOUND)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setLights(color(R.color.colorPrimary), 1000, 1000)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(startupPendingIntent)

        list.forEach { obj ->
            builder.setContentTitle(obj.name)
            builder.setContentText(obj.errorMsg)
            notifyManager!!.notify(
                obj.hashCode(),
                builder.build()
            )
        }
    }


    private fun updateNotification() {
        foregroundNotification!!.setContentTitle(getTitleNotifString())
        foregroundNotification!!.setStyle(notificationStyle())
        notifyManager!!.notify(serviceStartedNotifId, foregroundNotification!!.build())
    }


    private fun notificationStyle(): NotificationCompat.Style {
        val inboxStyle = NotificationCompat.InboxStyle()
        torrentHashMap.values.take(3).forEach {
            inboxStyle.addLine(it.name + " · " + it.downloadSpeed.formatSpeed() + " · " + it.progress.formatProgress())
        }
        return inboxStyle
    }

    private fun getTitleNotifString() =
        getString(R.string.torrent_d).format(torrentHashMap.size)

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unRegisterForEvent()
        super.onDestroy()
    }

}