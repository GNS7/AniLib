package com.revolgenx.anilib.torrent.sort

import com.revolgenx.anilib.torrent.core.Torrent

class TorrentSorting(columnName: SortingColumns, direction: Direction) :
    BaseSorting(columnName.name, direction) {

    enum class SortingColumns : SortingColumnsInterface<Torrent> {
        NAME {
            override fun compare(
                item1: Torrent,
                item2: Torrent, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item2.name.compareTo(item1.name)
                else
                    item1.name.compareTo(item2.name)
            }
        },
        SIZE {
            override fun compare(
                item1: Torrent,
                item2: Torrent, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item1.totalSize.compareTo(item2.totalSize)
                else
                    item2.totalSize.compareTo(item1.totalSize)
            }
        },
        PROGRESS {
            override fun compare(
                item1: Torrent,
                item2: Torrent, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item1.progress.compareTo(item2.progress)
                else
                    item2.progress.compareTo(item1.progress)
            }
        },
        DATE {
            override fun compare(
                item1: Torrent,
                item2: Torrent, direction: Direction
            ): Int {
                return if (direction === Direction.ASC) {
                    item1.createDate.time.compareTo(item2.createDate.time)
                } else {
                    item2.createDate.time.compareTo(item1.createDate.time)
                }
            }
        };

        companion object {
            fun fromValue(value: String): SortingColumns {
                for (column in SortingColumns::class.java.enumConstants!!) {
                    if (column.toString().equals(value, ignoreCase = true)) {
                        return column
                    }
                }
                return NAME
            }
        }
    }
}