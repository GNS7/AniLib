package com.revolgenx.anilib.preference

import android.content.Context
import com.revolgenx.anilib.util.getDefaultStoragePath
import org.libtorrent4j.swig.settings_pack


class TorrentPreference(
    private val context: Context
) {
    var cacheSize = DEFAULT_CACHE_SIZE
        get() = context.getInt(Key.CACHE_SIZE.name, field)
        set(value) = context.putInt(Key.CACHE_SIZE.name, value)

    var activeDownloads = DEFAULT_ACTIVE_DOWNLOADS
        get() = context.getInt(Key.ACTIVE_DOWNLOADS.name, field)
        set(value) = context.putInt(Key.ACTIVE_DOWNLOADS.name, value)

    var activeSeeds = DEFAULT_ACTIVE_SEEDS
        get() = context.getInt(Key.ACTIVE_SEEDS.name, field)
        set(value) = context.putInt(Key.ACTIVE_SEEDS.name, value)

    var maxPeerListSize = DEFAULT_MAX_PEER_LIST_SIZE
        get() = context.getInt(Key.MAX_PEER_LIST_SIZE.name, field)
        set(value) = context.putInt(Key.MAX_PEER_LIST_SIZE.name, value)

    var tickInterval = DEFAULT_TICK_INTERVAL
        get() = context.getInt(Key.TICK_INTERVAL.name, field)
        set(value) = context.putInt(Key.TICK_INTERVAL.name, value)

    var inactivityTimeout = DEFAULT_INACTIVITY_TIMEOUT
        get() = context.getInt(Key.INACTIVITY_TIMEOUT.name, field)
        set(value) = context.putInt(Key.INACTIVITY_TIMEOUT.name, value)

    var connectionsLimit = DEFAULT_CONNECTIONS_LIMIT
        get() = context.getInt(Key.CONNECTION_LIMIT.name, field)
        set(value) = context.putInt(Key.CONNECTION_LIMIT.name, value)

    var connectionsLimitPerTorrent = DEFAULT_CONNECTIONS_LIMIT_PER_TORRENT
        get() = context.getInt(Key.CONNECTION_LIMIT_PER_TORRENT.name, field)
        set(value) = context.putInt(Key.CONNECTION_LIMIT_PER_TORRENT.name, value)

    var uploadsLimitPerTorrent = DEFAULT_UPLOADS_LIMIT_PER_TORRENT
        get() = context.getInt(Key.UPLOADS_LIMIT_PER_TORRENT.name, field)
        set(value) = context.putInt(Key.UPLOADS_LIMIT_PER_TORRENT.name, value)

    var activeLimit = DEFAULT_ACTIVE_LIMIT
        get() = context.getInt(Key.ACTIVE_LIMIT.name, field)
        set(value) = context.putInt(Key.ACTIVE_LIMIT.name, value)

    var port = DEFAULT_PORT
        get() = context.getInt(Key.PORT.name, field)
        set(value) = context.putInt(Key.PORT.name, value)

    var downloadRateLimit = DEFAULT_DOWNLOAD_RATE_LIMIT
        get() = context.getInt(Key.DOWNLOAD_RATE_LIMIT.name, field) * 1024
        set(value) = context.putInt(Key.DOWNLOAD_RATE_LIMIT.name, value)

    var uploadRateLimit = DEFAULT_UPLOAD_RATE_LIMIT
        get() = context.getInt(Key.UPLOAD_RATE_LIMIT.name, field) * 1024
        set(value) = context.putInt(Key.UPLOAD_RATE_LIMIT.name, value)

    var dhtEnabled = DEFAULT_DHT_ENABLED
        get() = context.getBoolean(Key.DHT_ENABLED.name, field)
        set(value) = context.putBoolean(Key.DHT_ENABLED.name, value)

    var lsdEnabled = DEFAULT_LSD_ENABLED
        get() = context.getBoolean(Key.LSD_ENABLED.name, field)
        set(value) = context.putBoolean(Key.LSD_ENABLED.name, value)

    var utpEnabled = DEFAULT_UTP_ENABLED
        get() = context.getBoolean(Key.UTP_ENABLED.name, field)
        set(value) = context.putBoolean(Key.UTP_ENABLED.name, value)

    var upnpEnabled = DEFAULT_UPNP_ENABLED
        get() = context.getBoolean(Key.UPNP_ENABLED.name, field)
        set(value) = context.putBoolean(Key.UPNP_ENABLED.name, value)

    var natPmpEnabled = DEFAULT_NATPMP_ENABLED
        get() = context.getBoolean(Key.NATPMP_ENABLED.name, field)
        set(value) = context.putBoolean(Key.NATPMP_ENABLED.name, value)

    var encryptInConnections = DEFAULT_ENCRYPT_IN_CONNECTIONS
        get() = context.getBoolean(Key.ENCRYPT_IN_CONNECTIONS.name, field)
        set(value) = context.putBoolean(Key.ENCRYPT_IN_CONNECTIONS.name, value)

    var encryptOutConnections = DEFAULT_ENCRYPT_OUT_CONNECTIONS
        get() = context.getBoolean(Key.ENCRYPT_OUT_CONNECTIONS.name, field)
        set(value) = context.putBoolean(Key.ENCRYPT_OUT_CONNECTIONS.name, value)

    var encryptMode = DEFAULT_ENCRYPT_MODE
        get() = context.getInt(Key.ENCRYPT_MODE.name, field)
        set(value) = context.putInt(Key.ENCRYPT_MODE.name, value)

    var autoManaged = DEFAULT_AUTO_MANAGED
        get() = context.getBoolean(Key.AUTO_MANAGED.name, field)
        set(value) = context.putBoolean(Key.AUTO_MANAGED.name, value)

    var storagePath = getDefaultStoragePath()
        get() = context.getString(Key.STORAGE_PATH.name, field)!!
        set(value) = context.putString(Key.STORAGE_PATH.name, value)


    companion object {
        fun getTorrentPreferenceInstance(context: Context) = TorrentPreference(context)

        const val DEFAULT_CACHE_SIZE = 256
        const val DEFAULT_ACTIVE_DOWNLOADS = 3
        const val DEFAULT_ACTIVE_SEEDS = 3
        const val DEFAULT_MAX_PEER_LIST_SIZE = 200
        const val DEFAULT_TICK_INTERVAL = 1000
        const val DEFAULT_INACTIVITY_TIMEOUT = 60
        const val MIN_CONNECTIONS_LIMIT = 2
        const val DEFAULT_CONNECTIONS_LIMIT = 200
        const val DEFAULT_CONNECTIONS_LIMIT_PER_TORRENT = 40
        const val DEFAULT_UPLOADS_LIMIT_PER_TORRENT = 4
        const val DEFAULT_ACTIVE_LIMIT = 6
        const val DEFAULT_PORT = 6881
        const val MAX_PORT_NUMBER = 65535
        const val MIN_PORT_NUMBER = 49160
        const val DEFAULT_DOWNLOAD_RATE_LIMIT = 0
        const val DEFAULT_UPLOAD_RATE_LIMIT = 0
        const val DEFAULT_DHT_ENABLED = true
        const val DEFAULT_LSD_ENABLED = true
        const val DEFAULT_UTP_ENABLED = true
        const val DEFAULT_UPNP_ENABLED = true
        const val DEFAULT_NATPMP_ENABLED = true
        const val DEFAULT_ENCRYPT_IN_CONNECTIONS = true
        const val DEFAULT_ENCRYPT_OUT_CONNECTIONS = true
        val DEFAULT_ENCRYPT_MODE = settings_pack.enc_policy.pe_enabled.swigValue()
        const val DEFAULT_AUTO_MANAGED = false
    }

    enum class Key {
        CACHE_SIZE,
        ACTIVE_DOWNLOADS,
        ACTIVE_SEEDS,
        MAX_PEER_LIST_SIZE,
        TICK_INTERVAL,
        INACTIVITY_TIMEOUT,
        MIN_CONNECTIONS_LIMIT,
        CONNECTION_LIMIT,
        CONNECTION_LIMIT_PER_TORRENT,
        UPLOADS_LIMIT_PER_TORRENT,
        ACTIVE_LIMIT,
        PORT,
        PORT_NUMBER,
        MIN_PORT_NUMBER,
        DOWNLOAD_RATE_LIMIT,
        UPLOAD_RATE_LIMIT,
        DHT_ENABLED,
        LSD_ENABLED,
        UTP_ENABLED,
        UPNP_ENABLED,
        NATPMP_ENABLED,
        ENCRYPT_IN_CONNECTIONS,
        ENCRYPT_OUT_CONNECTIONS,
        ENCRYPT_MODE,
        AUTO_MANAGED,
        STORAGE_PATH
    }
}