package com.revolgenx.anilib.torrent.core

import com.revolgenx.anilib.event.TorrentEngineEvent
import com.revolgenx.anilib.event.TorrentEngineEventTypes
import com.revolgenx.anilib.exception.TorrentException
import com.revolgenx.anilib.preference.TorrentPreference
import org.libtorrent4j.*
import org.libtorrent4j.swig.*
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock


//TODO:CHECK ENGINE BEFORE ADDING MAGNET OR TORRENT

class TorrentEngine(val torrentPreferenceModel: TorrentPreference) : SessionManager() {

    var isEngineRunning: AtomicBoolean = AtomicBoolean(false)
    private val syncMagnet = ReentrantLock()

    override fun onBeforeStart() {
        super.onBeforeStart()
        Timber.d("torrent engine starting")
    }

    override fun onAfterStart() {
        super.onAfterStart()
        Timber.d("torrent engine started")
    }

    override fun start() {
        val params = loadSetting()

        if (isEngineRunning()) {
            TorrentEngineEvent(
                TorrentEngineEventTypes.ENGINE_STARTED
            ).postEvent
            return
        }

        TorrentEngineEvent(
            TorrentEngineEventTypes.ENGINE_STARTING
        ).postEvent

        Thread {
            super.start(params)
            isEngineRunning.set(true)
            TorrentEngineEvent(
                TorrentEngineEventTypes.ENGINE_STARTED
            ).postEvent
        }.start()

    }

    override fun onBeforeStop() {
        super.onBeforeStop()
        Timber.d("torrent engine stopping")
        isEngineRunning.set(false)
        TorrentEngineEvent(TorrentEngineEventTypes.ENGINE_STOPPING).postEvent
    }


    override fun stop() {
        if (isEngineRunning()) {
            Thread {
                super.stop()
            }.start()
        }
    }


    override fun onAfterStop() {
        super.onAfterStop()
        Timber.d("torrent engine stopped")
        TorrentEngineEvent(TorrentEngineEventTypes.ENGINE_STOPPED).postEvent
    }


    @Throws(TorrentException::class)
    fun fetchMagnet(uri: String, saveDir: File?): TorrentHandle {
        val ec = error_code()
        val p = add_torrent_params.parse_magnet_uri(uri, ec)
        require(ec.value() == 0) { ec.message() }
        p.set_disabled_storage()
        val hash = p.info_hash
        val strHash = hash.to_hex()
        var th: torrent_handle? = null
        return try {
            syncMagnet.lock()
            try {
                th = swig().find_torrent(hash)
                if (th != null && th.is_valid) {
                    throw TorrentException("Torrent exists!!!", TorrentHandle(th))
                }

                saveDir?.let {
                    p.save_path = it.absolutePath
                }

                if (p.name.isEmpty()) p.name = strHash
                var flags = p.flags
                flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())
                flags = flags.or_(TorrentFlags.UPLOAD_MODE)
                flags = flags.or_(TorrentFlags.STOP_WHEN_READY)

                p.flags = flags
                ec.clear()
                th = swig().add_torrent(p, ec)
                th.resume()
                TorrentHandle(th)
            } finally {
                syncMagnet.unlock()
            }
        } catch (e: TorrentException) {
            throw e
        } catch (e: Exception) {
            if (th != null && th.is_valid) swig().remove_torrent(th)
            throw Exception(e)
        }
    }

    fun cancelFetchMagnet(infoHash: String) {
        val th = find(Sha1Hash(infoHash))
        if (th != null && th.isValid) remove(th, SessionHandle.DELETE_FILES)
    }


    fun loadTorrent(magnetUri: String?, saveDir: File?): TorrentHandle? {
        if (swig() == null) {
            return null
        }
        val ec = error_code()
        val p = add_torrent_params.parse_magnet_uri(magnetUri, ec)
        require(ec.value() == 0) { ec.message() }
        val info_hash = p.info_hash
        var th = swig().find_torrent(info_hash)


        var flags = p.flags
        flags = flags.or_(TorrentFlags.UPLOAD_MODE)
        flags = flags.or_(TorrentFlags.STOP_WHEN_READY)

        if (th != null && th.is_valid) { // found a download with the same hash
            return TorrentHandle(th).also {
                it.unsetFlags(flags)
            }
        }
        if (saveDir != null) {
            p.save_path = saveDir.absolutePath
        }
        flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())
        p.flags = flags
        th = swig().add_torrent(p, ec)
        return TorrentHandle(th)
    }


    @Throws(TorrentException::class)
    fun loadTorrent(
        ti: TorrentInfo,
        saveDir: File?,
        resumeFile: ByteArray?,
        pri: Array<Priority>?,
        peers: MutableList<TcpEndpoint>?
    ): TorrentHandle? {
        if (swig() == null) {
            return null
        }
        var th = swig().find_torrent(ti.swig().info_hash())
        var priorities = pri
        if (th != null && th.is_valid) {
            throw TorrentException("Torrent Exists!!!", TorrentHandle(th))
        }

        var p: add_torrent_params? = null

        if (resumeFile != null) {
            try {
                val ec = error_code()
                p = add_torrent_params.read_resume_data(Vectors.bytes2byte_vector(resumeFile), ec)
                require(ec.value() == 0) { "Unable to read the resume data: " + ec.message() }
            } catch (e: Throwable) {
                Timber.e(e, "Unable to set resume data")
            }
        }

        if (p == null) {
            p = add_torrent_params.create_instance()
        }

        p!!.set_ti(ti.swig())
        if (saveDir != null) {
            p.save_path = saveDir.absolutePath
        }

        if (priorities != null) {
            require(ti.files().numFiles() == priorities.size) { "priorities count should be equals to the number of files" }
            val v = byte_vector()
            for (i in priorities.indices) {
                v.push_back(priorities[i].swig().toByte())
            }
            p.set_file_priorities2(v)
        }

        if (peers != null && peers.isNotEmpty()) {
            val v = tcp_endpoint_vector()
            for (endp in peers) {
                v.push_back(endp.swig())
            }
            p._peers = v
        }

        var flags = p.flags

        flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())

        p.flags = flags
        val ec = error_code()
        th = swig().add_torrent(p, ec)
        return TorrentHandle(th)
    }


    private fun loadSetting(): SessionParams {
        return SessionParams(defaultSettingsPack())
    }

    private fun defaultSettingsPack(): SettingsPack {
        val sp = SettingsPack()
        val maxQueuedDiskBytes = sp.maxQueuedDiskBytes()
        sp.maxQueuedDiskBytes(maxQueuedDiskBytes / 2)
        val sendBufferWatermark = sp.sendBufferWatermark()
        sp.sendBufferWatermark(sendBufferWatermark / 2)
        sp.seedingOutgoingConnections(false)
        settingsToSettingsPack(torrentPreferenceModel, sp)
        return sp
    }


    private fun settingsToSettingsPack(torrentPref: TorrentPreference, sp: SettingsPack) {
        sp.cacheSize(torrentPref.cacheSize)
        sp.activeDownloads(torrentPref.activeDownloads)
        sp.activeSeeds(torrentPref.activeSeeds)
        sp.activeLimit(torrentPref.activeLimit)
        sp.maxPeerlistSize(torrentPref.maxPeerListSize)
        sp.tickInterval(torrentPref.tickInterval)
        sp.inactivityTimeout(torrentPref.inactivityTimeout)
        sp.connectionsLimit(torrentPref.connectionsLimit)

        sp.listenInterfaces("0.0.0.0:49160,[::]:49160");
        sp.setInteger(
            settings_pack.int_types.max_retry_port_bind.swigValue(),
            65535 - 49160
        );
//        sp.setString(
//            settings_pack.string_types.listen_interfaces.swigValue(),
//            "0.0.0.0:65534"// + torrentPref.port
//        )

        sp.enableDht(torrentPref.dhtEnabled)
        sp.broadcastLSD(torrentPref.lsdEnabled)
        sp.setBoolean(
            settings_pack.bool_types.enable_incoming_utp.swigValue(),
            torrentPref.utpEnabled
        )
        sp.setBoolean(
            settings_pack.bool_types.enable_outgoing_utp.swigValue(),
            torrentPref.utpEnabled
        )
        sp.setBoolean(settings_pack.bool_types.enable_upnp.swigValue(), torrentPref.upnpEnabled)
        sp.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), torrentPref.natPmpEnabled)
        sp.setInteger(settings_pack.int_types.in_enc_policy.swigValue(), torrentPref.encryptMode)
        sp.setInteger(settings_pack.int_types.out_enc_policy.swigValue(), torrentPref.encryptMode)
        sp.uploadRateLimit(torrentPref.uploadRateLimit)
        sp.downloadRateLimit(torrentPref.downloadRateLimit)
    }


    /**
     * max speed in kb*/
    fun setMaxDownloadSpeed() {
        applySettings()
    }

    fun setMaxUploadSpeed() {
        applySettings()
    }

    private fun applySettings() {
        if (!isEngineRunning() || !isRunning) return

        val sp: SettingsPack = settings()
        settingsToSettingsPack(torrentPreferenceModel, sp)
        applySettings(sp)
    }


    fun isEngineRunning() = isEngineRunning.get()


}