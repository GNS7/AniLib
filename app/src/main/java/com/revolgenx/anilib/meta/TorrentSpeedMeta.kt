package com.revolgenx.anilib.meta

import kotlinx.android.parcel.Parcelize

@Parcelize
data class TorrentSpeedMeta(var uploadSpeed:Int, var downloadSpeed:Int):BaseMeta