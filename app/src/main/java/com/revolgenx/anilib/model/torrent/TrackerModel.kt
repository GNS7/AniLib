package com.revolgenx.anilib.model.torrent

import com.revolgenx.anilib.model.BaseModel

data class TrackerModel(var name: String, var working: TrackerStatus, var message: String) :
    BaseModel() {
    override fun equals(other: Any?): Boolean {
        return if (other is TrackerModel) {
            this.name == other.name && this.working == other.working && this.message == other.message
        } else false
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + working.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }
}

enum class TrackerStatus {
    WORKING, NOT_WORKING, UPDATING, NOT_CONTACTED, UNKNOWN
}