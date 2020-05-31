package com.revolgenx.anilib.preference

import android.content.Context

const val EXO_ORIENTATION_KEY = "exo_orientation_key"
const val TORRENT_SORT_KEY = "torrent_sort_key"
fun orientation(context: Context, value: Int) = context.putInt(EXO_ORIENTATION_KEY, value)
fun orientation(context: Context) = context.getInt(EXO_ORIENTATION_KEY, 10)

fun torrentSort(context: Context) = context.getInt(TORRENT_SORT_KEY, 0)
fun torrentSort(context: Context, sort: Int) = context.putInt(TORRENT_SORT_KEY, sort)