package com.revolgenx.anilib.torrent.sort

import com.revolgenx.anilib.torrent.core.Torrent

class TorrentSortingComparator(private val sorting: TorrentSorting) : Comparator<Torrent> {
    override fun compare(state1: Torrent?, state2: Torrent?): Int {
        return TorrentSorting.SortingColumns.fromValue(sorting.columnName)
            .compare(state1!!, state2!!, sorting.direction)
    }
}

fun makeTorrentSortingComparator(sort: Int): TorrentSortingComparator {
    return when (sort) {
        0 -> {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.NAME,
                    BaseSorting.Direction.ASC
                )
            )
        }
        1 -> {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.NAME,
                    BaseSorting.Direction.DESC
                )
            )
        }
        2 -> {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.SIZE,
                    BaseSorting.Direction.ASC
                )
            )
        }
        3 -> {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.SIZE,
                    BaseSorting.Direction.DESC
                )
            )
        }
        4 -> {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.PROGRESS,
                    BaseSorting.Direction.ASC
                )
            )
        }
        5 -> {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.PROGRESS,
                    BaseSorting.Direction.DESC
                )
            )
        }
        6 -> {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.DATE,
                    BaseSorting.Direction.ASC
                )
            )
        }
        7 -> {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.DATE,
                    BaseSorting.Direction.DESC
                )
            )
        }
        else->{
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.NAME,
                    BaseSorting.Direction.ASC
                )
            )
        }
    }
}