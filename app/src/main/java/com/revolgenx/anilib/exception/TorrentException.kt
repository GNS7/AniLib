package com.revolgenx.anilib.exception

import timber.log.Timber

class TorrentException(msg: String, var data:Any? = null) : Exception(msg) {
    init {
        Timber.e(msg)
    }
}

class TorrentResumeException(msg: String, e: Throwable?) : Exception(msg, e) {
    init {
        Timber.e(e, msg)
    }
}

class TorrentPauseException(msg: String, e: Throwable?) : Exception(msg, e) {
    init {
        Timber.e(e, msg)
    }
}

