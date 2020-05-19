package com.revolgenx.anilib.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.revolgenx.anilib.R
import com.revolgenx.anilib.event.*
import com.revolgenx.anilib.field.torrent.TorrentField
import com.revolgenx.anilib.repository.db.TorrentRepository
import com.revolgenx.anilib.repository.util.Resource
import com.revolgenx.anilib.repository.util.Status
import com.revolgenx.anilib.torrent.core.Torrent
import com.revolgenx.anilib.torrent.service.ServiceConnector
import com.revolgenx.anilib.torrent.service.isServiceRunning
import com.revolgenx.anilib.torrent.state.TorrentActiveState
import com.revolgenx.anilib.util.makeToast
import com.revolgenx.anilib.util.registerForEvent
import com.revolgenx.anilib.util.unRegisterForEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class TorrentViewModel(
    private val context: Context,
    private val torrentRepository: TorrentRepository,
    private val connector: ServiceConnector,
    private val torrentActiveState: TorrentActiveState
) : BaseViewModel() {
    private val torrentHashMap = mutableMapOf<String, Torrent>()
    private val hd: Handler = Handler(Looper.getMainLooper())

    val torrentLiveData = MutableLiveData<Resource<List<Torrent>>>()

    init {
        registerForEvent()
        torrentActiveState.fragmentActive = true
    }

    val torrentField = TorrentField()

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentAddedEvent(event: TorrentAddedEvent) {
        when (event.type) {
            TorrentAddedEventTypes.TORRENT_ADDED -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val torrent = event.torrent
                    val resource = torrentRepository.add(torrent)

                    if (resource.status == Status.SUCCESS) {
                        if (torrent.isPaused()) {
                            try {
                                torrent.resume()
                            } catch (e: Exception) {
                            }
                        }
                        torrentHashMap[torrent.hash] = torrent
                        updateResource()
                    }
                    TorrentEvent(listOf(torrent), TorrentEventType.TORRENT_RESUMED).postEvent
                }
            }
            TorrentAddedEventTypes.TORRENT_ADD_ERROR -> {
                context.makeToast(R.string.unable_to_add_torrent_file)
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentRemovedEvent(event: TorrentRemovedEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            val torrents =
                event.hashes.filter { torrentHashMap[it] != null }.map { torrentHashMap[it]!! }
            val resource = torrentRepository.removeAllWithIds(torrents)

            if (resource.status == Status.SUCCESS) {
                torrents.forEach { torrent ->
                    torrent.remove(event.withFiles)
                    torrent.removeAllListener()
                    torrentHashMap.remove(torrent.hash)
                }
                updateResource()
            } else {
                launch(Dispatchers.Main) {
                    context.makeToast(R.string.failed_to_remove_torrent)
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun torrentEvent(event: TorrentEvent) {
        if (context.isServiceRunning()) return
        if (event.torrents.isEmpty()) return

        when (event.type) {
            TorrentEventType.TORRENT_RESUMED -> {
                connector.connect { service, connected ->
                    connector.serviceConnectionListener = null
                    if (connected) {
                        event.postEvent
                    }
                    connector.disconnect()
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateDatabase(event: TorrentDbEvent) {
        if (context.isServiceRunning()) return

        viewModelScope.launch(Dispatchers.IO) {
            val data = event.torrent
            torrentRepository.update(data)
        }
    }


    fun resumeAll() {
        if (torrentHashMap.isEmpty()) return

        torrentHashMap.values.forEach {
            try {
                it.resume(true)
                it.update()
            } catch (e: Exception) {

            }
        }

        connector.connect { service, connected ->
            connector.serviceConnectionListener = null
            if (connected) {
                TorrentEvent(
                    torrentHashMap.values.filter { !it.isPaused() }.toList(),
                    TorrentEventType.TORRENT_RESUMED
                ).postEvent
            }
            connector.disconnect()
        }
    }

    fun pauseAll() {
        if (torrentHashMap.isEmpty()) return

        torrentHashMap.values.forEach {
            try {
                it.pause(true)
                it.update()
            } catch (e: Exception) {

            }
        }

        connector.connect { service, connected ->
            connector.serviceConnectionListener = null
            if (connected) {
                TorrentEvent(
                    torrentHashMap.values.filter { it.isPaused() }.toList(),
                    TorrentEventType.TORRENT_PAUSED
                ).postEvent
            }
            connector.disconnect()
        }
    }


    fun getAllTorrents() {
        viewModelScope.launch(Dispatchers.IO) {
            if (torrentHashMap.isNotEmpty()) {
                return@launch
            } else {
                torrentLiveData.postValue(Resource.loading(null))
            }

            if (context.isServiceRunning()) {
                connector.connect { service, connected ->
                    if (connected) {
                        connector.serviceConnectionListener = null
                        service!!.torrentHashMap.forEach {
                            torrentHashMap[it.value.hash] = it.value
                        }
                        viewModelScope.launch(Dispatchers.IO) {
                            val resource =
                                torrentRepository.getAllNotIn(torrentHashMap.values.map { it.hash })

                            resource.data!!.forEach { torrent ->
                                torrentHashMap[torrent.hash] = torrent
                            }

//                            resource.data.filter { !it.isPaused() }.let {
//                                    postEvent(TorrentEvent(it, TorrentEventType.TORRENT_RESUMED))
//                                }

                            updateResource()
                        }
                        connector.disconnect()
                    }
                }
                Timber.d("service running")
            } else {
                val resource = torrentRepository.getAll()
                if (resource.status == Status.SUCCESS) {
                    resource.data!!.forEach { torrent -> torrentHashMap[torrent.hash] = torrent }
                    updateResource()
                    Timber.d("service not running")
                } else {
                    torrentLiveData.postValue(resource)
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun recheckTorrents(event: TorrentRecheckEvent) {
        val selectedHashes = event.selectedHashes
        val torrents = selectedHashes.mapNotNull { torrentHashMap[it] }.toList()
        torrents.forEach {
            it.forceRecheck()
            it.update()
        }

        connector.connect { service, connected ->
            connector.serviceConnectionListener = null
            if (connected) {
                TorrentEvent(
                    torrents,
                    TorrentEventType.TORRENT_RESUMED
                ).postEvent
            }

            connector.disconnect()
        }
    }


    private fun updateResource() {
        torrentLiveData.postValue(
            Resource.success(torrentHashMap.values.toMutableList())
        )
    }


    override fun onCleared() {
        unRegisterForEvent()
        torrentHashMap.clear()
        connector.disconnect()
        torrentActiveState.fragmentActive = false
        super.onCleared()
    }

    fun getTorrent(toHex: String?): Torrent? {
        return torrentHashMap[toHex]
    }

    fun removeAllTorrentEngineListener() {
        torrentHashMap.forEach { u ->
            u.value.removeEngineListener()
        }
    }
}
